// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2025 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.tools.AddShowTask
import com.battlelancer.seriesguide.util.TaskManager
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.runBlocking
import java.util.LinkedList

class HexagonSync(
    private val context: Context,
    private val hexagonTools: HexagonTools,
    private val movieTools: MovieTools,
    private val progress: SyncProgress
) {

    data class HexagonResult(
        val hasAddedShows: Boolean,
        val success: Boolean
    )

    /**
     * Syncs episodes, shows and movies with Hexagon.
     *
     * Merges shows, episodes and movies after a sign-in. Consecutive syncs will only download
     * changes to shows, episodes and movies.
     *
     * Note: this calls [syncMovies] which may throw [InterruptedException].
     */
    @Throws(InterruptedException::class)
    fun sync(): HexagonResult {
        val tmdbIdsToShowIds = SgApp.getServicesComponent(context).showTools()
            .getTmdbIdsToShowIds()

        //// EPISODES
        progress.publish(SyncProgress.Step.HEXAGON_EPISODES)
        val syncEpisodesSuccessful = syncEpisodes(tmdbIdsToShowIds)
        if (!syncEpisodesSuccessful) {
            progress.recordError()
        }

        //// SHOWS
        progress.publish(SyncProgress.Step.HEXAGON_SHOWS)
        val syncShowsResult = syncShows(tmdbIdsToShowIds)
        if (!syncShowsResult.success) {
            progress.recordError()
        }

        //// MOVIES
        progress.publish(SyncProgress.Step.HEXAGON_MOVIES)
        val syncMoviesSuccessful = syncMovies()
        if (!syncMoviesSuccessful) {
            progress.recordError()
        }

        //// LISTS
        progress.publish(SyncProgress.Step.HEXAGON_LISTS)
        val syncListsSuccessful = syncLists()
        if (!syncListsSuccessful) {
            progress.recordError()
        }

        val success = syncEpisodesSuccessful
                && syncShowsResult.success
                && syncMoviesSuccessful
                && syncListsSuccessful

        return HexagonResult(syncShowsResult.hasAddedShows, success)
    }

    private fun syncEpisodes(tmdbIdsToShowIds: Map<Int, Long>): Boolean {
        val database = SgRoomDatabase.getInstance(context)
        val dbShowHelper = database.sgShow2Helper()
        val showsToMerge = dbShowHelper.getHexagonMergeNotCompleted()

        // try merging episodes for them
        var mergeSuccessful = true

        val dbEpisodeHelper = database.sgEpisode2Helper()
        val episodeSync = HexagonEpisodeSync(context, hexagonTools, dbEpisodeHelper, dbShowHelper)
        for (show in showsToMerge) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false
            }

            // TMDB ID is required, legacy shows with TVDB only data will no longer be synced.
            val showTmdbId = show.tmdbId ?: continue
            if (showTmdbId == 0) continue;

            var success = episodeSync.downloadFlags(show.id, showTmdbId, show.tvdbId)
            if (!success) {
                // try again next time
                mergeSuccessful = false
                continue
            }

            success = episodeSync.uploadFlags(show.id, showTmdbId)
            if (success) {
                // set merge as completed
                dbShowHelper.setHexagonMergeCompleted(show.id)
            } else {
                mergeSuccessful = false
            }
        }

        // download changed episodes and update properties on existing episodes
        val changedDownloadSuccessful = episodeSync.downloadChangedFlags(tmdbIdsToShowIds)

        return mergeSuccessful && changedDownloadSuccessful
    }

    private fun syncShows(tmdbIdsToShowIds: Map<Int, Long>): HexagonResult {
        val hasMergedShows = HexagonSettings.hasMergedShows(context)

        // download shows and apply property changes (if merging only overwrite some properties)
        val showSync = HexagonShowSync(context, hexagonTools)
        val newShows = HashMap<Int, AddShowTask.Show>()
        val downloadSuccessful = showSync.download(tmdbIdsToShowIds, newShows, hasMergedShows)
        if (!downloadSuccessful) {
            return HexagonResult(false, false)
        }

        // if merge required, upload all shows to Hexagon
        if (!hasMergedShows) {
            val uploadSuccessful = showSync.uploadAll()
            if (!uploadSuccessful) {
                return HexagonResult(false, false)
            }
        }

        // add new shows
        val addNewShows = newShows.isNotEmpty()
        if (addNewShows) {
            val newShowsList = LinkedList(newShows.values)
            TaskManager.performAddTask(context, newShowsList, true, !hasMergedShows)
        } else if (!hasMergedShows) {
            // set shows as merged
            HexagonSettings.setHasMergedShows(context)
        }

        return HexagonResult(addNewShows, true)
    }

    /**
     * Note: this uses [runBlocking], so if the calling thread is interrupted this will throw
     * [InterruptedException].
     */
    @Throws(InterruptedException::class)
    private fun syncMovies(): Boolean {
        val hasMergedMovies = HexagonSettings.hasMergedMovies(context)

        // download movies and apply property changes, build list of new movies
        val newCollectionMovies = HashSet<Int>()
        val newWatchlistMovies = HashSet<Int>()
        val newWatchedMoviesToPlays = HashMap<Int, Int>()
        val movieSync = HexagonMovieSync(context, hexagonTools)
        val downloadSuccessful = movieSync.download(
            newCollectionMovies,
            newWatchlistMovies,
            newWatchedMoviesToPlays,
            hasMergedMovies
        )
        if (!downloadSuccessful) {
            return false
        }

        if (!hasMergedMovies) {
            val uploadSuccessful = movieSync.uploadAll()
            if (!uploadSuccessful) {
                return false
            }
        }

        // add new movies with the just downloaded properties
        val addingSuccessful = runBlocking {
            movieTools.addMovies(newCollectionMovies, newWatchlistMovies, newWatchedMoviesToPlays)
        }
        if (!hasMergedMovies) {
            // ensure all missing movies from Hexagon are added before merge is complete
            if (!addingSuccessful) {
                return false
            }
            HexagonSettings.setHasMergedMovies(context)
        }

        return addingSuccessful
    }

    private fun syncLists(): Boolean {
        val hasMergedLists = HexagonSettings.hasMergedLists(context)

        val listsSync = HexagonListsSync(context, hexagonTools)
        if (!listsSync.download(hasMergedLists)) {
            return false
        }

        if (hasMergedLists) {
            // on regular syncs, remove lists gone from hexagon
            if (!listsSync.pruneRemovedLists()) {
                return false
            }
        } else {
            // upload all lists on initial data merge
            if (!listsSync.uploadAll()) {
                return false
            }
        }

        if (!hasMergedLists) {
            HexagonSettings.setHasMergedLists(context)
        }

        return true
    }
}
