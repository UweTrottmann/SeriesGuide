package com.battlelancer.seriesguide.sync

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.provider.SgEpisode2CollectedUpdate
import com.battlelancer.seriesguide.provider.SgEpisode2ForSync
import com.battlelancer.seriesguide.provider.SgEpisode2WatchedUpdate
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.Errors.Companion.logAndReport
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.trakt5.entities.BaseSeason
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncEpisode
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncSeason
import com.uwetrottmann.trakt5.entities.SyncShow
import com.uwetrottmann.trakt5.services.Sync
import org.threeten.bp.OffsetDateTime
import timber.log.Timber

/**
 * Downloads and uploads watched and collected episodes.
 *
 * [traktSync] may be null if not an initial sync, e.g. if calling [storeEpisodeFlags].
 */
class TraktEpisodeSync(
    private val context: Context,
    private val traktSync: Sync?
) {

    /**
     * Similar to the sync methods, but does not download anything and only processes a single show.
     */
    fun storeEpisodeFlags(
        tmdbIdsToTraktShow: Map<Int, BaseShow>?,
        showTmdbId: Int,
        showRowId: Long,
        flag: Flag
    ): Boolean {
        if (tmdbIdsToTraktShow == null || tmdbIdsToTraktShow.isEmpty()) {
            return true // no watched/collected shows on Trakt, done.
        }
        val traktShow = tmdbIdsToTraktShow[showTmdbId]
            ?: return true // show is not watched/collected on Trakt, done.
        return processTraktSeasons(false, showRowId, traktShow, flag)
    }

    /**
     * Sets all episodes that are watched on Trakt and not watched locally as watched.
     * If [isInitialSync] will upload episodes watched locally that are not watched on
     * Trakt. If an episode has multiple plays, uploads it multiple times.
     * If false, sets episodes that are not watched on Trakt but watched locally
     * (and only those, e.g. no skipped episodes) as not watched.
     */
    fun syncWatched(
        tmdbIdsToShowIds: Map<Int, Long>,
        watchedAt: OffsetDateTime?,
        isInitialSync: Boolean
    ): Boolean {
        if (watchedAt == null) {
            Timber.e("syncWatched: null watched_at")
            return false
        }
        val lastWatchedAt = TraktSettings.getLastEpisodesWatchedAt(context)
        if (isInitialSync || TimeTools.isAfterMillis(watchedAt, lastWatchedAt)) {
            val watchedShowsTrakt = try {
                val response = traktSync!!
                    .watchedShows(null)
                    .execute()
                if (!response.isSuccessful) {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return false
                    }
                    logAndReport("get watched shows", response)
                    return false
                }
                response.body()
            } catch (e: Exception) {
                logAndReport("get watched shows", e)
                return false
            } ?: return false

            // apply database updates, if initial sync upload diff
            val startTime = System.currentTimeMillis()
            val success = processTraktShows(
                watchedShowsTrakt, tmdbIdsToShowIds, Flag.WATCHED,
                isInitialSync
            )
            Timber.d("syncWatched: processing took %s ms", System.currentTimeMillis() - startTime)
            if (!success) {
                return false
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(
                    TraktSettings.KEY_LAST_EPISODES_WATCHED_AT,
                    watchedAt.toInstant().toEpochMilli()
                )
                .apply()
            Timber.d("syncWatched: success")
        } else {
            Timber.d("syncWatched: no changes since %tF %tT", lastWatchedAt, lastWatchedAt)
        }
        return true
    }

    /**
     * Sets all episodes that are collected on Trakt and not collected locally as collected.
     * If [isInitialSync] will upload episodes collected locally that are not collected on
     * Trakt.
     * If false, sets episodes that are not collected on Trakt but collected locally
     * as not collected.
     */
    fun syncCollected(
        tmdbIdsToShowIds: Map<Int, Long>,
        collectedAt: OffsetDateTime?,
        isInitialSync: Boolean
    ): Boolean {
        if (collectedAt == null) {
            Timber.e("syncCollected: null collected_at")
            return false
        }
        val lastCollectedAt = TraktSettings.getLastEpisodesCollectedAt(context)
        if (isInitialSync || TimeTools.isAfterMillis(collectedAt, lastCollectedAt)) {
            val collectedShowsTrakt = try {
                val response = traktSync!!
                    .collectionShows(null)
                    .execute()
                if (!response.isSuccessful) {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return false
                    }
                    logAndReport("get collected shows", response)
                    return false
                }
                response.body()
            } catch (e: Exception) {
                logAndReport("get collected shows", e)
                return false
            } ?: return false

            // apply database updates, if initial sync upload diff
            val startTime = System.currentTimeMillis()
            val success = processTraktShows(
                collectedShowsTrakt, tmdbIdsToShowIds,
                Flag.COLLECTED, isInitialSync
            )
            Timber.d(
                "syncCollected: processing took %s ms",
                System.currentTimeMillis() - startTime
            )
            if (!success) {
                return false
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(
                    TraktSettings.KEY_LAST_EPISODES_COLLECTED_AT,
                    collectedAt.toInstant().toEpochMilli()
                )
                .apply()
            Timber.d("syncCollected: success")
        } else {
            Timber.d("syncCollected: no changes since %tF %tT", lastCollectedAt, lastCollectedAt)
        }
        return true
    }

    private fun processTraktShows(
        remoteShows: List<BaseShow>,
        tmdbIdsToShowIds: Map<Int, Long>,
        flag: Flag,
        isInitialSync: Boolean
    ): Boolean {
        val tmdbIdsToTraktShow = TraktTools2.mapByTmdbId(remoteShows)

        var uploadedShowsCount = 0
        val showIdsToLastWatched: MutableMap<Long, Long> = HashMap()
        val showsToClear = ArrayList<Long>()

        for ((tmdbId, showId) in tmdbIdsToShowIds) {
            val traktShow = tmdbIdsToTraktShow[tmdbId]
            if (traktShow != null) {
                // show watched/collected on Trakt
                if (!processTraktSeasons(isInitialSync, showId, traktShow, flag)) {
                    return false // processing seasons failed, give up.
                }
                // For watched shows update local last watched timestamp.
                if (flag == Flag.WATCHED) {
                    val lastWatchedAt = traktShow.last_watched_at
                    if (lastWatchedAt != null) {
                        showIdsToLastWatched[showId] = lastWatchedAt.toInstant().toEpochMilli()
                    }
                }
            } else {
                // show not watched/collected on Trakt
                // check if this is because the show can not be tracked with Trakt (yet)
                // keep state local and maybe upload in the future
                val showTraktId = ShowTools.getShowTraktId(context, showId)
                if (showTraktId != null) {
                    // Show can be tracked with Trakt.
                    if (isInitialSync) {
                        // upload all watched/collected episodes of the show
                        // do in between processing to stretch uploads over longer time periods
                        uploadShow(traktSync!!, showId, showTraktId, flag)
                        uploadedShowsCount++
                    } else {
                        // Set all watched/collected episodes of show not watched/collected,
                        // clear plays if watched.
                        showsToClear.add(showId)
                    }
                }
            }
        }
        // Clear all watched/collected episodes of marked shows.
        val database = SgRoomDatabase.getInstance(context)
        if (showsToClear.isNotEmpty()) {
            if (flag == Flag.WATCHED) {
                database.sgEpisode2Helper().setShowsNotWatchedExcludeSkipped(showsToClear)
            } else {
                database.sgEpisode2Helper().updateCollectedOfShows(showsToClear, false)
            }
        }
        // Update last watched timestamps.
        if (showIdsToLastWatched.isNotEmpty()) {
            database.sgShow2Helper().updateLastWatchedMsIfLater(showIdsToLastWatched)
        }
        if (uploadedShowsCount > 0) {
            Timber.d(
                "processTraktShows: uploaded %s flags for %s complete shows.", flag.id,
                uploadedShowsCount
            )
        }
        return true
    }

    /**
     * Sync the watched/collected episodes of the given Trakt show with the local episodes. The
     * given show has to be watched/collected on Trakt.
     *
     * If [isInitialSync], will upload watched/collected episodes that are not
     * watched/collected on Trakt. If `false`, will set them not watched/collected (if not
     * skipped) to mirror the Trakt episode.
     */
    private fun processTraktSeasons(
        isInitialSync: Boolean, showRowId: Long,
        traktShow: BaseShow, flag: Flag
    ): Boolean {
        val traktSeasons = TraktTools.mapSeasonsByNumber(traktShow.seasons)

        val database = SgRoomDatabase.getInstance(context)
        val localSeasons = database.sgSeason2Helper()
            .getSeasonNumbersOfShow(showRowId)

        val seasonsToClear = ArrayList<Long>()
        val syncSeasons: MutableList<SyncSeason> = ArrayList()
        for (localSeason in localSeasons) {
            val seasonId = localSeason.id
            val seasonNumber = localSeason.number
            if (traktSeasons.containsKey(seasonNumber)) {
                // Season watched/collected on Trakt.
                if (flag == Flag.WATCHED) {
                    if (!processWatchedTraktEpisodes(
                            seasonId, seasonNumber,
                            traktSeasons[seasonNumber]!!, syncSeasons, isInitialSync
                        )) {
                        return false
                    }
                } else {
                    if (!processCollectedTraktEpisodes(
                            seasonId, seasonNumber,
                            traktSeasons[seasonNumber]!!, syncSeasons, isInitialSync
                        )) {
                        return false
                    }
                }
            } else {
                // Season not watched/collected on Trakt.
                if (isInitialSync) {
                    // schedule all watched/collected episodes of this season for upload
                    buildSyncSeason(seasonId, seasonNumber, flag)
                        ?.also { syncSeasons.add(it) }
                } else {
                    // Set all watched/collected episodes of season not watched/collected,
                    // clear plays if watched.
                    seasonsToClear.add(seasonId)
                }
            }
        }
        if (seasonsToClear.isNotEmpty()) {
            if (flag == Flag.WATCHED) {
                database.sgEpisode2Helper().setSeasonsNotWatchedExcludeSkipped(seasonsToClear)
            } else {
                database.sgEpisode2Helper().updateCollectedOfSeasons(seasonsToClear, false)
            }
        }
        return if (isInitialSync && syncSeasons.size > 0) {
            // upload watched/collected episodes for this show
            val showTraktId = ShowTools.getShowTraktId(context, showRowId)
                ?: return false // show should have a Trakt id, give up
            upload(traktSync!!, showTraktId, syncSeasons, flag)
        } else {
            true
        }
    }

    private fun processWatchedTraktEpisodes(
        seasonId: Long,
        seasonNumber: Int,
        traktSeason: BaseSeason,
        syncSeasons: MutableList<SyncSeason>,
        isInitialSync: Boolean
    ): Boolean {
        val traktEpisodes = TraktTools.buildTraktEpisodesMap(traktSeason.episodes)

        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val localEpisodes = helper.getEpisodesForTraktSync(seasonId)

        val batch = ArrayList<SgEpisode2WatchedUpdate>()
        val syncEpisodes: MutableList<SyncEpisode> = ArrayList()
        var episodesSetOnePlayCount = 0
        var episodesUnsetCount = 0
        for ((episodeId, episodeNumber, _, watchedFlag, plays) in localEpisodes) {
            val isWatchedLocally = EpisodeTools.isWatched(watchedFlag)
            val traktEpisode = traktEpisodes[episodeNumber]
            if (traktEpisode != null) {
                // Episode watched on Trakt.
                val traktPlays = traktEpisode.plays
                if (watchedFlag != EpisodeFlags.WATCHED) {
                    // Local episode is skipped or not watched.
                    // Set as watched and store plays.
                    val playsToStore = if (traktPlays != null && traktPlays > 0) traktPlays else 1
                    batch.add(
                        SgEpisode2WatchedUpdate(episodeId, EpisodeFlags.WATCHED, playsToStore)
                    )
                    if (playsToStore == 1) {
                        episodesSetOnePlayCount++
                    }
                } else if (watchedFlag == EpisodeFlags.WATCHED) {
                    // Watched locally: update plays if changed.
                    if (traktPlays != null && traktPlays > 0 && traktPlays != plays) {
                        batch.add(
                            SgEpisode2WatchedUpdate(episodeId, EpisodeFlags.WATCHED, traktPlays)
                        )
                    }
                }
            } else {
                // Episode not watched on Trakt.
                // Note: episodes skipped locally are not touched.
                if (isWatchedLocally) {
                    if (isInitialSync) {
                        // Upload to Trakt.
                        // Add an episode for each play, Trakt will create a separate play for each.
                        val syncEpisode = SyncEpisode().number(episodeNumber)
                        for (i in 0 until plays) {
                            syncEpisodes.add(syncEpisode)
                        }
                    } else {
                        // Set as not watched and remove plays if it is currently watched.
                        batch.add(
                            SgEpisode2WatchedUpdate(episodeId, EpisodeFlags.UNWATCHED, 0)
                        )
                        episodesUnsetCount++
                    }
                }
            }
        }
        val localEpisodeCount = localEpisodes.size
        val setWatchedOnePlayWholeSeason = episodesSetOnePlayCount == localEpisodeCount
        val notWatchedWholeSeason = episodesUnsetCount == localEpisodeCount

        // Performance improvement especially on initial syncs:
        // if setting the whole season as (not) watched with 1 play, replace with single db op.
        when {
            setWatchedOnePlayWholeSeason -> helper.setSeasonWatched(seasonId)
            notWatchedWholeSeason -> helper.setSeasonNotWatchedAndRemovePlays(seasonId)
            else -> {
                // Or apply individual episode updates.
                helper.updateEpisodesWatched(batch)
            }
        }
        if (isInitialSync && syncEpisodes.size > 0) {
            syncSeasons.add(
                SyncSeason()
                    .number(seasonNumber)
                    .episodes(syncEpisodes)
            )
        }
        return true
    }

    private fun processCollectedTraktEpisodes(
        seasonId: Long,
        seasonNumber: Int,
        traktSeason: BaseSeason,
        syncSeasons: MutableList<SyncSeason>,
        isInitialSync: Boolean
    ): Boolean {
        val traktEpisodes = TraktTools.buildTraktEpisodesMap(traktSeason.episodes)

        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val localEpisodes = helper.getEpisodesForTraktSync(seasonId)

        val batch = ArrayList<SgEpisode2CollectedUpdate>()
        val syncEpisodes: MutableList<SyncEpisode> = ArrayList()
        var episodesAddCount = 0
        var episodesRemoveCount = 0
        for ((episodeId, episodeNumber, _, _, _, isCollectedLocally) in localEpisodes) {
            val traktEpisode = traktEpisodes[episodeNumber]
            if (traktEpisode != null) {
                // Episode collected on Trakt.
                if (!isCollectedLocally) {
                    // Set as collected if it is currently not.
                    batch.add(SgEpisode2CollectedUpdate(episodeId, true))
                    episodesAddCount++
                }
            } else {
                // Episode not collected on Trakt.
                if (isCollectedLocally) {
                    if (isInitialSync) {
                        // Upload to Trakt.
                        syncEpisodes.add(SyncEpisode().number(episodeNumber))
                    } else {
                        // Set as not collected if it is currently.
                        batch.add(SgEpisode2CollectedUpdate(episodeId, false))
                        episodesRemoveCount++
                    }
                }
            }
        }
        val localEpisodeCount = localEpisodes.size
        val addWholeSeason = episodesAddCount == localEpisodeCount
        val removeWholeSeason = episodesRemoveCount == localEpisodeCount

        // Performance improvement especially on initial syncs:
        // if setting the whole season as (not) collected, replace with single db op.
        if (addWholeSeason || removeWholeSeason) {
            helper.updateCollectedOfSeason(seasonId, addWholeSeason)
        } else {
            // Or apply individual episode updates.
            helper.updateEpisodesCollected(batch)
        }
        if (isInitialSync && syncEpisodes.size > 0) {
            syncSeasons.add(
                SyncSeason()
                    .number(seasonNumber)
                    .episodes(syncEpisodes)
            )
        }
        return true
    }

    /**
     * Uploads all watched/collected episodes for the given show to Trakt.
     */
    private fun uploadShow(
        traktSync: Sync,
        showId: Long,
        showTraktId: Int,
        flag: Flag
    ): Boolean {
        val localSeasons = SgRoomDatabase.getInstance(context)
            .sgSeason2Helper()
            .getSeasonNumbersOfShow(showId)
        val syncSeasons = ArrayList<SyncSeason>()
        for (localSeason in localSeasons) {
            buildSyncSeason(localSeason.id, localSeason.number, flag)
                ?.also { syncSeasons.add(it) }
        }
        return if (syncSeasons.isEmpty()) {
            true // nothing to upload for this show
        } else upload(traktSync, showTraktId, syncSeasons, flag)
    }

    /**
     * Uploads all the given watched/collected episodes of the given show to Trakt.
     *
     * @return Any of the [TraktTools] result codes.
     */
    private fun upload(
        traktSync: Sync,
        showTraktId: Int,
        syncSeasons: List<SyncSeason>,
        flag: Flag
    ): Boolean {
        val syncShow = SyncShow().apply {
            id(ShowIds.trakt(showTraktId))
            seasons = syncSeasons
        }

        // upload
        val syncItems = SyncItems().shows(syncShow)
        try {
            val response = if (flag == Flag.WATCHED) {
                // uploading watched episodes
                traktSync.addItemsToWatchedHistory(syncItems).execute()
            } else {
                // uploading collected episodes
                traktSync.addItemsToCollection(syncItems).execute()
            }
            if (response.isSuccessful) {
                return true
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false
                }
                logAndReport("add episodes to " + flag.id, response)
            }
        } catch (e: Exception) {
            logAndReport("add episodes to " + flag.id, e)
        }
        return false
    }

    /**
     * Returns watched/collected episodes of the given season for uploading.
     * Returns `null` if no episodes of that season are watched or collected.
     */
    private fun buildSyncSeason(seasonId: Long, seasonNumber: Int, flag: Flag): SyncSeason? {
        // query for watched/collected episodes of the given season
        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val episodes: List<SgEpisode2ForSync> = when (flag) {
            Flag.WATCHED -> {
                helper.getWatchedEpisodesForTraktSync(seasonId)
            }
            Flag.COLLECTED -> {
                helper.getCollectedEpisodesForTraktSync(seasonId)
            }
        }
        val syncEpisodes: MutableList<SyncEpisode> = ArrayList()
        for ((_, number, _, _, plays) in episodes) {
            val syncEpisode = SyncEpisode().number(number)

            // Add an episode for each play, Trakt will create a separate play for each.
            // Or only a single one if sending collected flag.
            val count = if (flag == Flag.WATCHED) plays else 1
            for (i in 0 until count) {
                syncEpisodes.add(syncEpisode)
            }
        }
        return if (syncEpisodes.size == 0) {
            null // no episodes watched/collected
        } else SyncSeason().number(seasonNumber).episodes(syncEpisodes)
    }

    enum class Flag(val id: String) {
        COLLECTED("collected"),
        WATCHED("watched");
    }
}