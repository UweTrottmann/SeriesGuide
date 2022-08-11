package com.battlelancer.seriesguide.sync

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.github.michaelbull.result.getOrElse
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.LastActivityMore
import com.uwetrottmann.trakt5.services.Sync
import timber.log.Timber

/**
 * Syncs episode watched and collected state, episode ratings, show ratings,
 * watchlist, collection and watched movies and movie ratings with Trakt.
 * Removes movies not in any of these lists on Trakt.
 */
class TraktSync(
    private val context: Context,
    private val movieTools: MovieTools,
    private val traktSync: Sync,
    private val progress: SyncProgress
) {
    /**
     * To not conflict with Hexagon sync, can turn on [onlyRatings] so only
     * ratings are synced.
     */
    fun sync(currentTime: Long, onlyRatings: Boolean): SgSyncAdapter.UpdateResult {
        progress.publish(SyncProgress.Step.TRAKT)
        if (!AndroidUtils.isNetworkConnected(context)) {
            progress.recordError()
            return SgSyncAdapter.UpdateResult.INCOMPLETE
        }

        // Get last activity timestamps.
        val lastActivity = TraktTools2.getLastActivity(context)
            .getOrElse {
                progress.recordError()
                Timber.e("performTraktSync: last activity download failed")
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }

        val ratingsSync = TraktRatingsSync(context, traktSync)
        val tmdbIdsToShowIds = SgApp.getServicesComponent(context).showTools()
            .getTmdbIdsToShowIds()
        if (tmdbIdsToShowIds.isEmpty()) {
            Timber.d("performTraktSync: no local shows, skip shows")
        } else {
            // EPISODES
            if (!onlyRatings) {
                // Download and upload episode watched and collected flags.
                progress.publish(SyncProgress.Step.TRAKT_EPISODES)
                if (!AndroidUtils.isNetworkConnected(context)) {
                    progress.recordError()
                    return SgSyncAdapter.UpdateResult.INCOMPLETE
                }
                if (!syncEpisodes(tmdbIdsToShowIds, lastActivity.episodes, currentTime)) {
                    progress.recordError()
                    return SgSyncAdapter.UpdateResult.INCOMPLETE
                }
            }
            // Download episode ratings.
            progress.publish(SyncProgress.Step.TRAKT_RATINGS)
            if (!AndroidUtils.isNetworkConnected(context)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }
            if (!ratingsSync.downloadForEpisodes(lastActivity.episodes.rated_at)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }

            // SHOWS
            // Download show ratings.
            if (!AndroidUtils.isNetworkConnected(context)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }
            if (!ratingsSync.downloadForShows(lastActivity.shows.rated_at)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }
        }

        // MOVIES
        progress.publish(SyncProgress.Step.TRAKT_MOVIES)
        // Sync watchlist, collection and watched movies.
        if (!onlyRatings) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }
            if (!TraktMovieSync(context, movieTools, traktSync)
                    .syncLists(lastActivity.movies)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }
            // Clean up any useless movies (not watched or not in any list).
            MovieTools.deleteUnusedMovies(context)
        }
        // Download movie ratings.
        progress.publish(SyncProgress.Step.TRAKT_RATINGS)
        if (!AndroidUtils.isNetworkConnected(context)) {
            progress.recordError()
            return SgSyncAdapter.UpdateResult.INCOMPLETE
        }
        if (!ratingsSync.downloadForMovies(lastActivity.movies.rated_at)) {
            progress.recordError()
            return SgSyncAdapter.UpdateResult.INCOMPLETE
        }

        return SgSyncAdapter.UpdateResult.SUCCESS
    }

    /**
     * Downloads and uploads episode watched and collected flags.
     *
     *  Do **NOT** call if there are no local shows to avoid unnecessary work.
     */
    private fun syncEpisodes(
        tmdbIdsToShowIds: Map<Int, Long>,
        lastActivity: LastActivityMore,
        currentTime: Long
    ): Boolean {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return false // Auth was removed.
        }

        // Download flags.
        // If initial sync, upload any flags missing on Trakt
        // otherwise clear all local flags not on Trakt.
        val isInitialSync = !TraktSettings.hasMergedEpisodes(context)

        // Watched episodes.
        val episodeSync = TraktEpisodeSync(context, traktSync)
        if (!episodeSync
                .syncWatched(tmdbIdsToShowIds, lastActivity.watched_at, isInitialSync)) {
            return false // failed, give up.
        }

        // Collected episodes.
        if (!episodeSync
                .syncCollected(tmdbIdsToShowIds, lastActivity.collected_at, isInitialSync)) {
            return false
        }

        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        if (isInitialSync) {
            // Success, set initial sync as complete.
            editor.putBoolean(TraktSettings.KEY_HAS_MERGED_EPISODES, true)
        }
        // Success, set last sync time to now.
        editor.putLong(TraktSettings.KEY_LAST_FULL_EPISODE_SYNC, currentTime)
        editor.apply()
        return true
    }

}