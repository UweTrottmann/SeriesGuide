// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2024 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.util.Errors
import com.github.michaelbull.result.getOrElse
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.LastActivityMore
import com.uwetrottmann.trakt5.services.Sync
import retrofit2.Response
import timber.log.Timber

/**
 * Syncs episode watched and collected state, episode ratings, show ratings,
 * watchlist, collection and watched movies and movie ratings with Trakt.
 * Removes movies not in any of these lists on Trakt.
 */
class TraktSync(
    val context: Context,
    val movieTools: MovieTools,
    val sync: Sync,
    val progress: SyncProgress
) {
     private fun noConnection(): Boolean {
        return if (AndroidUtils.isNetworkConnected(context)) {
            false
        } else {
            progress.recordError()
            true
        }
    }

    /**
     * To not conflict with Hexagon sync, can turn on [onlyRatings] so only
     * ratings are synced.
     */
    fun sync(onlyRatings: Boolean): SgSyncAdapter.UpdateResult {
        progress.publish(SyncProgress.Step.TRAKT)
        // While responses might get returned from the disk cache,
        // this is not desirable when syncing, so frequently check for a network connection.
        // Note: looked into creating a separate HTTP client without cache, but it makes sense
        // to keep one as some of the responses are re-used in other parts of the app.
        if (noConnection()) return SgSyncAdapter.UpdateResult.INCOMPLETE

        // Get last activity timestamps.
        val lastActivity = TraktTools2.getLastActivity(context)
            .getOrElse {
                progress.recordError()
                Timber.e("performTraktSync: last activity download failed")
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }

        val ratingsSync = TraktRatingsSync(this)
        val tmdbIdsToShowIds = SgApp.getServicesComponent(context).showTools()
            .getTmdbIdsToShowIds()
        if (tmdbIdsToShowIds.isEmpty()) {
            Timber.d("performTraktSync: no local shows, skip shows")
        } else {
            // EPISODES
            if (!onlyRatings) {
                // Download and upload episode watched and collected flags.
                progress.publish(SyncProgress.Step.TRAKT_EPISODES)
                if (noConnection()) return SgSyncAdapter.UpdateResult.INCOMPLETE
                if (!syncEpisodes(tmdbIdsToShowIds, lastActivity.episodes)) {
                    progress.recordError()
                    return SgSyncAdapter.UpdateResult.INCOMPLETE
                }
            }
            // Download episode ratings.
            progress.publish(SyncProgress.Step.TRAKT_RATINGS)
            if (noConnection()) return SgSyncAdapter.UpdateResult.INCOMPLETE
            if (!ratingsSync.downloadForEpisodes(lastActivity.episodes.rated_at)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }

            // SHOWS
            // Download show ratings.
            if (noConnection()) return SgSyncAdapter.UpdateResult.INCOMPLETE
            if (!ratingsSync.downloadForShows(lastActivity.shows.rated_at)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }
        }

        // MOVIES
        progress.publish(SyncProgress.Step.TRAKT_MOVIES)
        // Sync watchlist, collection and watched movies.
        if (!onlyRatings) {
            if (noConnection()) return SgSyncAdapter.UpdateResult.INCOMPLETE
            if (!TraktMovieSync(this).syncLists(lastActivity.movies)) {
                progress.recordError()
                return SgSyncAdapter.UpdateResult.INCOMPLETE
            }
            // Clean up any useless movies (not watched or not in any list).
            MovieTools.deleteUnusedMovies(context)
        }
        // Download movie ratings.
        progress.publish(SyncProgress.Step.TRAKT_RATINGS)
        if (noConnection()) return SgSyncAdapter.UpdateResult.INCOMPLETE
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
        lastActivity: LastActivityMore
    ): Boolean {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return false // Auth was removed.
        }

        // Download flags.
        // If initial sync, upload any flags missing on Trakt
        // otherwise clear all local flags not on Trakt.
        val isInitialSync = TraktSettings.isInitialSyncEpisodes(context)

        // Watched episodes.
        val episodeSync = TraktEpisodeSync(this)
        if (!episodeSync
                .syncWatched(tmdbIdsToShowIds, lastActivity.watched_at, isInitialSync)) {
            return false // failed, give up.
        }

        // Collected episodes.
        if (!episodeSync
                .syncCollected(tmdbIdsToShowIds, lastActivity.collected_at, isInitialSync)) {
            return false
        }

        if (isInitialSync) {
            // Success, set initial sync as complete.
            TraktSettings.setInitialSyncEpisodesCompleted(context)
        }
        return true
    }

    fun <T> handleUnsuccessfulResponse(response: Response<T>, action: String) {
        // Handle auth error.
        if (SgTrakt.isUnauthorized(context, response)) {
            return // Do not report auth errors.
        }
        when (response.code()) {
            420 -> progress.setImportantErrorIfNone(context.getString(R.string.trakt_error_limit_exceeded))
            423 -> {
                // Note: Even though uploading typically happens after signing in, which should
                // detect locked accounts, it's possible an account becomes locked afterwards.
                progress.setImportantErrorIfNone(context.getString(R.string.trakt_error_account_locked))
            }
        }
        Errors.logAndReport(action, response)
    }

}