// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2024 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.Context
import android.text.TextUtils
import androidx.core.util.Pair
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2UpdateByNumber
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.util.Errors.Companion.logAndReportHexagon
import com.google.api.client.util.DateTime
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.backend.episodes.model.Episode
import com.uwetrottmann.seriesguide.backend.episodes.model.SgCloudEpisode
import com.uwetrottmann.seriesguide.backend.episodes.model.SgCloudEpisodeList
import timber.log.Timber
import java.io.IOException

class HexagonEpisodeSync(
    private val context: Context,
    private val hexagonTools: HexagonTools
) {
    /**
     * Downloads all episodes changed since the last time this was called and applies changes to
     * the database.
     */
    fun downloadChangedFlags(tmdbIdsToShowIds: Map<Int, Long>): Boolean {
        val currentTime = System.currentTimeMillis()
        val database = SgRoomDatabase.getInstance(context)
        val lastSyncTime = DateTime(HexagonSettings.getLastEpisodesSyncTime(context))
        Timber.d("downloadChangedFlags: since %s", lastSyncTime)

        var episodes: List<SgCloudEpisode>?
        var cursor: String? = null
        var hasMoreEpisodes = true
        val showIdsToLastWatched: MutableMap<Long, ShowLastWatchedInfo> = HashMap()
        while (hasMoreEpisodes) {
            try {
                // get service each time to check if auth was removed
                val episodesService = hexagonTools.episodesService
                    ?: return false

                val request = episodesService.sgEpisodes
                    .setUpdatedSince(lastSyncTime) // use default server limit
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor)
                }

                val response = request.execute()
                if (response == null) {
                    Timber.d("downloadChangedFlags: response was null, nothing more to do")
                    break
                }

                episodes = response.episodes

                // check for more items
                if (response.cursor != null) {
                    cursor = response.cursor
                } else {
                    hasMoreEpisodes = false
                }
            } catch (e: IOException) {
                logAndReportHexagon("get updated episodes", e)
                return false
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                logAndReportHexagon("get updated episodes", e)
                return false
            }

            if (episodes.isNullOrEmpty()) {
                break
            }

            // build batch of episode flag updates
            val batch = ArrayList<SgEpisode2UpdateByNumber>()
            for (episode in episodes) {
                val showTmdbId = episode.showTmdbId
                val showId = tmdbIdsToShowIds[showTmdbId]
                    ?: continue  // ignore, show not added on this device

                val watchedFlag = episode.watchedFlag
                var playsOrNull: Int? = null
                if (watchedFlag != null) {
                    playsOrNull = if (watchedFlag == EpisodeFlags.WATCHED) {
                        // Watched.
                        // Note: plays may be null for legacy data. Protect against invalid data.
                        if (episode.plays != null && episode.plays >= 1) {
                            episode.plays
                        } else {
                            1
                        }
                    } else {
                        0 // Skipped or not watched.
                    }

                    // record the latest last watched time and episode ID for a show
                    if (!EpisodeTools.isUnwatched(watchedFlag)) {
                        val lastWatchedInfo = showIdsToLastWatched[showId]
                        // episodes returned in reverse chronological order, so just get the first time
                        if (lastWatchedInfo == null && episode.updatedAt != null) {
                            val updatedAtMs = episode.updatedAt.value
                            showIdsToLastWatched[showId] = ShowLastWatchedInfo(
                                updatedAtMs, episode.seasonNumber, episode.episodeNumber
                            )
                        }
                    }
                }

                batch.add(
                    SgEpisode2UpdateByNumber(
                        showId,
                        episode.episodeNumber,
                        episode.seasonNumber,
                        watchedFlag,
                        playsOrNull,
                        episode.isInCollection
                    )
                )
            }

            // execute database update
            database.sgEpisode2Helper().updateWatchedAndCollectedByNumber(batch)
        }

        if (showIdsToLastWatched.isNotEmpty()) {
            // Note: it is possible that this overwrites a more recently watched episode,
            // however, the next sync should contain this episode and restore it.
            database.sgShow2Helper()
                .updateLastWatchedMsIfLaterAndLastWatchedEpisodeId(
                    showIdsToLastWatched,
                    database.sgEpisode2Helper()
                )
        }

        // store new last sync time
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putLong(HexagonSettings.KEY_LAST_SYNC_EPISODES, currentTime)
            .apply()

        return true
    }

    /**
     * Downloads watched, skipped or collected episodes of this show from Hexagon and applies
     * those flags and plays to episodes in the database.
     *
     * If a [showTvdbId] is given tries to use legacy data if no data using [showTmdbId] is found.
     *
     * @return Whether the download was successful and all changes were applied to the database.
     */
    fun downloadFlags(showId: Long, showTmdbId: Int, showTvdbId: Int?): Boolean {
        Timber.d("downloadFlags: for show %s", showId)

        var result = downloadFlagsByTmdbId(showId, showTmdbId)
        if (result.noData && showTvdbId != null) {
            // If no data by TMDB ID, try to get legacy data by TVDB ID.
            Timber.d("downloadFlags: no data by TMDB ID, trying by TVDB ID")
            result = downloadFlagsByTvdbId(showId, showTvdbId)
            if (result.success) {
                // If had to use legacy show data, schedule episode upload (using TMDB IDs).
                SgRoomDatabase.getInstance(context).sgShow2Helper()
                    .setHexagonMergeNotCompleted(showId)
            }
        }

        result.lastWatchedMs?.let {
            SgRoomDatabase.getInstance(context).sgShow2Helper()
                .updateLastWatchedMsIfLater(showId, it)
        }

        return result.success
    }

    private fun downloadFlagsByTmdbId(showId: Long, showTmdbId: Int): DownloadFlagsResult {
        var episodes: List<SgCloudEpisode>?
        var onFirstPage = true
        var hasMoreEpisodes = true
        var cursor: String? = null

        var lastWatchedMs: Long? = null
        while (hasMoreEpisodes) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("downloadFlags: no network connection")
                return DownloadFlagsResult.FAILED
            }

            try {
                // get service each time to check if auth was removed
                val episodesService = hexagonTools.episodesService
                    ?: return DownloadFlagsResult.FAILED

                // build request
                val request = episodesService.sgEpisodes
                    .setShowTmdbId(showTmdbId) // use default server limit
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor)
                }

                // execute request
                // If empty server should send status 200 and empty list, so no body is a failure
                val response = request.execute()
                    ?: return DownloadFlagsResult.FAILED

                episodes = response.episodes

                // check for more items
                if (response.cursor != null) {
                    cursor = response.cursor
                } else {
                    hasMoreEpisodes = false
                }
            } catch (e: IOException) {
                logAndReportHexagon("get episodes of show", e)
                return DownloadFlagsResult.FAILED
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                logAndReportHexagon("get episodes of show", e)
                return DownloadFlagsResult.FAILED
            }

            if (episodes.isNullOrEmpty()) {
                if (onFirstPage) {
                    // If there is no data by TMDB ID at all, try again using TVDB ID.
                    return DownloadFlagsResult.NO_DATA
                } else {
                    // no more updates to apply
                    break
                }
            }
            onFirstPage = false

            // build batch of episode flag updates
            val batch = ArrayList<SgEpisode2UpdateByNumber>()
            for (episode in episodes) {
                val update = buildSgEpisodeUpdate(
                    episode.watchedFlag,
                    episode.plays,
                    episode.isInCollection,
                    episode.updatedAt,
                    episode.episodeNumber,
                    episode.seasonNumber,
                    showId,
                    lastWatchedMs
                )
                if (update != null) {
                    batch.add(update.first)
                    lastWatchedMs = update.second
                }
            }

            // execute database update
            SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .updateWatchedAndCollectedByNumber(batch)
        }

        return DownloadFlagsResult(success = true, noData = false, lastWatchedMs = lastWatchedMs)
    }

    private fun downloadFlagsByTvdbId(showId: Long, showTvdbId: Int): DownloadFlagsResult {
        var episodes: List<Episode>?
        var hasMoreEpisodes = true
        var cursor: String? = null

        var lastWatchedMs: Long? = null
        while (hasMoreEpisodes) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("downloadFlags: no network connection")
                return DownloadFlagsResult.FAILED
            }

            try {
                // get service each time to check if auth was removed
                val episodesService = hexagonTools.episodesService
                    ?: return DownloadFlagsResult.FAILED

                // build request
                val request = episodesService.get()
                    .setShowTvdbId(showTvdbId) // use default server limit
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor)
                }

                // execute request
                // If empty server should send status 200 and empty list, so no body is a failure
                val response = request.execute()
                    ?: return DownloadFlagsResult.FAILED

                episodes = response.episodes

                // check for more items
                if (response.cursor != null) {
                    cursor = response.cursor
                } else {
                    hasMoreEpisodes = false
                }
            } catch (e: IOException) {
                logAndReportHexagon("get episodes of show", e)
                return DownloadFlagsResult.FAILED
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                logAndReportHexagon("get episodes of show", e)
                return DownloadFlagsResult.FAILED
            }

            if (episodes.isNullOrEmpty()) {
                break // no (more) updates to apply
            }

            // build batch of episode flag updates
            val batch = ArrayList<SgEpisode2UpdateByNumber>()
            for (episode in episodes) {
                val update = buildSgEpisodeUpdate(
                    episode.watchedFlag,
                    episode.plays,
                    episode.isInCollection,
                    episode.updatedAt,
                    episode.episodeNumber,
                    episode.seasonNumber,
                    showId,
                    lastWatchedMs
                )
                if (update != null) {
                    batch.add(update.first)
                    lastWatchedMs = update.second
                }
            }

            // execute database update
            SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .updateWatchedAndCollectedByNumber(batch)
        }

        return DownloadFlagsResult(success = true, noData = false, lastWatchedMs = lastWatchedMs)
    }

    private fun buildSgEpisodeUpdate(
        watchedFlag: Int?,
        plays: Int?,
        isInCollection: Boolean?,
        updatedAt: DateTime?,
        episodeNumber: Int,
        seasonNumber: Int,
        showId: Long,
        lastWatchedMs: Long?
    ): Pair<SgEpisode2UpdateByNumber, Long?>? {
        var lastWatchedMsUpdated = lastWatchedMs
        var watchedFlagOrNull: Int? = null
        var playsOrNull: Int? = null
        if (watchedFlag != null && watchedFlag != EpisodeFlags.UNWATCHED) {
            // Watched or skipped.
            watchedFlagOrNull = watchedFlag
            if (watchedFlag == EpisodeFlags.WATCHED) {
                // Note: plays may be null for legacy data. Protect against invalid data.
                playsOrNull = if (plays != null && plays >= 1) {
                    plays
                } else {
                    1
                }
            }
            // record last watched time by taking latest updatedAt of watched/skipped
            if (updatedAt != null) {
                val lastWatchedMsNew = updatedAt.value
                if (lastWatchedMsUpdated == null || lastWatchedMsUpdated < lastWatchedMsNew) {
                    lastWatchedMsUpdated = lastWatchedMsNew
                }
            }
        }

        val inCollection = isInCollection != null && isInCollection

        if (watchedFlag == null && !inCollection) {
            // skip if episode has no watched flag and is not in collection
            return null
        }

        return Pair(
            SgEpisode2UpdateByNumber(
                showId,
                episodeNumber,
                seasonNumber,
                watchedFlagOrNull,
                playsOrNull,
                isInCollection
            ),
            lastWatchedMsUpdated
        )
    }

    /**
     * Uploads all watched, skipped including plays or collected episodes of this show to Hexagon.
     *
     * @return Whether the upload was successful.
     */
    fun uploadFlags(showId: Long, showTmdbId: Int): Boolean {
        // query for watched, skipped or collected episodes
        val episodesForSync = SgRoomDatabase.getInstance(context)
            .sgEpisode2Helper()
            .getEpisodesForHexagonSync(showId)
        if (episodesForSync.isEmpty()) {
            Timber.d("uploadFlags: uploading none for show %d", showId)
            return true
        } else {
            // Issues with some requests failing at Cloud due to
            // EOFException: Unexpected end of ZLIB input stream
            // Using info log to report sizes that are uploaded to determine
            // if MAX_BATCH_SIZE is actually too large.
            // https://github.com/UweTrottmann/SeriesGuide/issues/781
            Timber.i("uploadFlags: uploading %d for show %d", episodesForSync.size, showId)
        }

        // build list of episodes to upload
        var episodes: MutableList<SgCloudEpisode> = ArrayList()
        val count = episodesForSync.size
        for (i in 0 until count) {
            val episodeForSync = episodesForSync[i]

            val episode = SgCloudEpisode()
            episode.setSeasonNumber(episodeForSync.season)
            episode.setEpisodeNumber(episodeForSync.number)

            val watchedFlag = episodeForSync.watched
            if (!EpisodeTools.isUnwatched(watchedFlag)) {
                // Skipped or watched.
                episode.setWatchedFlag(watchedFlag)
                episode.setPlays(episodeForSync.plays)
            }

            if (episodeForSync.collected) {
                episode.setIsInCollection(true)
            }

            episodes.add(episode)

            // upload a batch
            val isLast = i + 1 == count
            if (episodes.size == MAX_BATCH_SIZE || isLast) {
                val episodeList = SgCloudEpisodeList()
                episodeList.setEpisodes(episodes)
                episodeList.setShowTmdbId(showTmdbId)

                try {
                    // get service each time to check if auth was removed
                    val episodesService = hexagonTools.episodesService
                        ?: return false
                    episodesService.saveSgEpisodes(episodeList).execute()
                } catch (e: IOException) {
                    // abort
                    logAndReportHexagon("save episodes of show", e)
                    return false
                }

                // clear array
                episodes = ArrayList()
            }
        }

        return true
    }

    companion object {
        // See API documentation on list size limit.
        const val MAX_BATCH_SIZE: Int = 500
    }
}
