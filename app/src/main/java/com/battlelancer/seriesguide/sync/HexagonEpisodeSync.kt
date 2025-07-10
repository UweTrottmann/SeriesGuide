// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2025 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.Context
import android.text.TextUtils
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.shows.database.SgEpisode2CollectedUpdateByNumber
import com.battlelancer.seriesguide.shows.database.SgEpisode2Helper
import com.battlelancer.seriesguide.shows.database.SgEpisode2WatchedUpdateByNumber
import com.battlelancer.seriesguide.shows.database.SgShow2Helper
import com.battlelancer.seriesguide.shows.database.ShowLastWatchedInfo
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
    private val hexagonTools: HexagonTools,
    private val dbEpisodeHelper: SgEpisode2Helper,
    private val dbShowHelper: SgShow2Helper
) {
    /**
     * Downloads all episodes changed since the last time this was called and applies changes to
     * the database.
     */
    fun downloadChangedFlags(tmdbIdsToShowIds: Map<Int, Long>): Boolean {
        val currentTime = System.currentTimeMillis()
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

            // Record the latest last watched time and episode ID for a show by taking the one with
            // the latest updatedAt time.
            for (episode in episodes) {
                val showTmdbId: Int = episode.showTmdbId
                val showId = tmdbIdsToShowIds[showTmdbId]
                    ?: continue // ignore, show not added in this library

                val watchedFlag: Int? = episode.watchedFlag
                if (watchedFlag != null) {
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
            }

            buildAndApplyEpisodeUpdatesFromCloud(episodes, tmdbIdsToShowIds)
        }

        if (showIdsToLastWatched.isNotEmpty()) {
            // Note: it is possible that this overwrites a more recently watched episode,
            // however, the next sync should contain this episode and restore it.
            dbShowHelper.updateLastWatchedMsIfLaterAndLastWatchedEpisodeId(
                showIdsToLastWatched,
                dbEpisodeHelper
            )
        }

        HexagonSettings.setLastEpisodesSyncTime(context, currentTime)

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
        Timber.d(
            "downloadFlags: for show %s (TMDB ID %s, TVDB ID %s)",
            showId,
            showTmdbId,
            showTvdbId
        )

        var result = downloadFlagsByTmdbId(showId, showTmdbId)
        if (result.noData && showTvdbId != null) {
            // If no data by TMDB ID, try to get legacy data by TVDB ID.
            Timber.d("downloadFlags: no data by TMDB ID, trying by TVDB ID")
            result = downloadFlagsByTvdbId(showId, showTvdbId)
            if (result.success) {
                // If had to use legacy show data, schedule episode upload (using TMDB IDs).
                dbShowHelper.setHexagonMergeNotCompleted(showId)
            }
        }

        result.lastWatchedMs?.let {
            dbShowHelper.updateLastWatchedMsIfLater(showId, it)
        }

        return result.success
    }

    data class DownloadFlagsResult(
        val success: Boolean,
        val noData: Boolean,
        val lastWatchedMs: Long?
    ) {
        companion object {
            @JvmField
            val FAILED = DownloadFlagsResult(success = false, noData = false, lastWatchedMs = null)

            @JvmField
            val NO_DATA = DownloadFlagsResult(success = true, noData = true, lastWatchedMs = null)
        }
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

            // Record last watched time by taking latest updatedAt of all watched/skipped episodes
            lastWatchedMs = getLatestUpdatedAt(lastWatchedMs, episodes)

            buildAndApplyEpisodeValuesFromCloud(showId, episodes)
        }

        return DownloadFlagsResult(success = true, noData = false, lastWatchedMs = lastWatchedMs)
    }

    private fun downloadFlagsByTvdbId(showId: Long, showTvdbId: Int): DownloadFlagsResult {
        var legacyEpisodes: List<Episode>?
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

                legacyEpisodes = response.episodes

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

            if (legacyEpisodes.isNullOrEmpty()) {
                break // no (more) updates to apply
            }

            val episodes = legacyEpisodes.map { it.toCloudEpisode() }

            // Record last watched time by taking latest updatedAt of all watched/skipped episodes
            lastWatchedMs = getLatestUpdatedAt(lastWatchedMs, episodes)

            buildAndApplyEpisodeValuesFromCloud(showId, episodes)
        }

        return DownloadFlagsResult(success = true, noData = false, lastWatchedMs = lastWatchedMs)
    }

    private fun Episode.toCloudEpisode(): SgCloudEpisode {
        return SgCloudEpisode()
            .setEpisodeNumber(episodeNumber)
            .setSeasonNumber(seasonNumber)
            .setWatchedFlag(watchedFlag)
            .setPlays(plays)
            .setIsInCollection(isInCollection)
            .setCreatedAt(createdAt)
            .setUpdatedAt(updatedAt)
    }

    fun buildAndApplyEpisodeUpdatesFromCloud(
        episodes: List<SgCloudEpisode>,
        tmdbIdsToShowIds: Map<Int, Long>
    ) {
        val watchedUpdate = ArrayList<SgEpisode2WatchedUpdateByNumber>()
        val collectedUpdate = ArrayList<SgEpisode2CollectedUpdateByNumber>()
        for (episode in episodes) {
            val showTmdbId: Int = episode.showTmdbId
            val showId = tmdbIdsToShowIds[showTmdbId]
                ?: continue // ignore, show not added to this library

            episode.watchedFlag
                ?.let { buildWatchedUpdate(showId, episode, it) }
                ?.let { watchedUpdate.add(it) }

            episode.isInCollection
                ?.let { buildCollectedUpdate(showId, episode, it) }
                ?.let { collectedUpdate.add(it) }
        }

        // execute database update
        dbEpisodeHelper.updateWatchedAndCollectedByNumber(watchedUpdate, collectedUpdate)
    }

    /**
     * Note: skips updating watched flag for unwatched episodes and in collection state for
     * episodes not in collection.
     */
    fun buildAndApplyEpisodeValuesFromCloud(showId: Long, episodes: List<SgCloudEpisode>) {
        val watchedUpdate = ArrayList<SgEpisode2WatchedUpdateByNumber>()
        val collectedUpdate = ArrayList<SgEpisode2CollectedUpdateByNumber>()
        for (episode in episodes) {
            // Optimization: episodes of a newly added show are all unwatched by default, no need to
            // update them as unwatched.
            episode.watchedFlag
                ?.let {
                    if (!EpisodeTools.isUnwatched(it)) {
                        buildWatchedUpdate(showId, episode, it)
                    } else null
                }
                ?.let { watchedUpdate.add(it) }

            // Optimization: episodes of a newly added show are all not in the collection by
            // default, no need to update them as not in collection.
            episode.isInCollection
                ?.let {
                    if (it) {
                        buildCollectedUpdate(showId, episode, it)
                    } else null
                }
                ?.let { collectedUpdate.add(it) }
        }

        // execute database update
        dbEpisodeHelper.updateWatchedAndCollectedByNumber(watchedUpdate, collectedUpdate)
    }

    private fun buildWatchedUpdate(
        showId: Long,
        cloudEpisode: SgCloudEpisode,
        watchedFlag: Int
    ): SgEpisode2WatchedUpdateByNumber {
        val plays = if (EpisodeTools.isWatched(watchedFlag)) {
            // Note: plays may be null for legacy data. Protect against invalid data.
            val playsOrNull: Int? = cloudEpisode.plays
            if (playsOrNull != null && playsOrNull >= 1) {
                playsOrNull
            } else {
                1
            }
        } else {
            0 // Skipped or not watched
        }

        return SgEpisode2WatchedUpdateByNumber(
            showId = showId,
            episodeNumber = cloudEpisode.episodeNumber,
            seasonNumber = cloudEpisode.seasonNumber,
            watched = watchedFlag,
            plays = plays
        )
    }

    private fun buildCollectedUpdate(
        showId: Long,
        cloudEpisode: SgCloudEpisode,
        inCollection: Boolean
    ) = SgEpisode2CollectedUpdateByNumber(
        showId = showId,
        episodeNumber = cloudEpisode.episodeNumber,
        seasonNumber = cloudEpisode.seasonNumber,
        collected = inCollection
    )

    /**
     * Get latest updatedAt value of all watched or skipped [episodes] or the given
     * [latestUpdatedAtCurrent].
     */
    private fun getLatestUpdatedAt(
        latestUpdatedAtCurrent: Long?,
        episodes: List<SgCloudEpisode>
    ): Long? {
        var latestUpdatedAt = latestUpdatedAtCurrent
        for (episode in episodes) {
            val updatedAt = episode.updatedAt?.value ?: continue
            val watchedFlag = episode.watchedFlag ?: continue

            // Only check watched or skipped
            if (EpisodeTools.isUnwatched(watchedFlag)) continue

            if (latestUpdatedAt == null || latestUpdatedAt < updatedAt) {
                latestUpdatedAt = updatedAt
            }
        }
        return latestUpdatedAt
    }

    /**
     * Uploads all watched, skipped including plays or collected episodes of this show to Hexagon.
     *
     * @return Whether the upload was successful.
     */
    fun uploadFlags(showId: Long, showTmdbId: Int): Boolean {
        // query for watched, skipped or collected episodes
        val episodesForSync = dbEpisodeHelper.getEpisodesForHexagonSync(showId)
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
