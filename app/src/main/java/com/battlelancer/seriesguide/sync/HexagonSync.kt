// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.CloudAuthInterruptedIOException
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.tools.AddShowTask
import com.battlelancer.seriesguide.util.TaskManager
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.io.InterruptedIOException
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
    ) {
        companion object {
            val FAILED = HexagonResult(hasAddedShows = false, success = false)
        }
    }

    /**
     * Syncs episodes, shows and movies with Hexagon.
     *
     * Merges shows, episodes and movies after a sign-in. Consecutive syncs will only download
     * changes to shows, episodes and movies.
     *
     * If a step fails, other steps are still attempted unless the calling thread is interrupted.
     * In which case [syncMovies] may throw [InterruptedException].
     *
     * If steps fail, they [SyncProgress.recordError].
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

        if (Thread.currentThread().isInterrupted) return HexagonResult.FAILED

        //// SHOWS
        progress.publish(SyncProgress.Step.HEXAGON_SHOWS)
        val syncShowsResult = syncShows(tmdbIdsToShowIds)
        if (!syncShowsResult.success) {
            progress.recordError()
        }

        // Don't set hasAddedShows to avoid rebuilding search table as if interrupted this should
        // finish quickly.
        if (Thread.currentThread().isInterrupted) return HexagonResult.FAILED

        //// MOVIES
        progress.publish(SyncProgress.Step.HEXAGON_MOVIES)
        val syncMoviesSuccessful = syncMovies()
        if (!syncMoviesSuccessful) {
            progress.recordError()
        }

        if (Thread.currentThread().isInterrupted) return HexagonResult.FAILED

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

    /**
     * If an operation fails, network connectivity is lost or the calling thread is interrupted this
     * may only partially complete.
     *
     * @return If everything completed successfully.
     */
    private fun syncEpisodes(tmdbIdsToShowIds: Map<Int, Long>): Boolean {
        val database = SgRoomDatabase.getInstance(context)
        val dbShowHelper = database.sgShow2Helper()
        val showsToMerge = dbShowHelper.getHexagonMergeNotCompleted()

        // try merging episodes for them
        var mergeSuccessful = true

        val dbEpisodeHelper = database.sgEpisode2Helper()
        val episodeSync = HexagonEpisodeSync(context, hexagonTools, dbEpisodeHelper, dbShowHelper)
        for (show in showsToMerge) {
            // Do network and interrupted checks here as well, as otherwise this just continues onto
            // the next show.
            if (!AndroidUtils.isNetworkConnected(context)) return false
            if (Thread.currentThread().isInterrupted) return false

            // TMDB ID is required, legacy shows with TVDB only data will no longer be synced.
            val showTmdbId = show.tmdbId ?: continue
            if (showTmdbId == 0) continue

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
            return HexagonResult.FAILED
        }

        // if merge required, upload all shows to Hexagon
        if (!hasMergedShows) {
            val uploadSuccessful = showSync.uploadAll()
            if (!uploadSuccessful) {
                return HexagonResult.FAILED
            }
        }

        // add new shows
        val addNewShows = newShows.isNotEmpty()
        if (addNewShows) {
            val newShowsList = LinkedList(newShows.values)
            TaskManager.performAddTask(
                context = context,
                shows = newShowsList,
                isSilentMode = true,
                isMergingShows = !hasMergedShows,
                // Don't upload any shows that are being added from Cloud.
                uploadToHexagon = false
            )
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

/**
 * Helper method to execute requests that restores the interrupted state if it was cleared by
 *
 * - the HTTP request interceptor that adds the auth token,
 * - the internal OkHttp client used by the HTTP transport used by Cloud.
 *
 * The [SgSyncAdapter] thread may be interrupted and relies on checking the interrupted state to
 * stop quickly. Note that despite this, any non-suspending Room operation still clears the
 * interrupted state (see its internal `runBlockingUninterruptible`).
 */
@Throws(IOException::class)
fun <T> AbstractGoogleJsonClientRequest<T>.executeRestoringInterrupt(): T {
    try {
        return execute()
    } catch (e: Exception) {
        // FirebaseHttpRequestInitializer returns a CloudAuthInterruptedIOException.
        // com.google.api.client.http.javanet.NetHttpTransport uses java.net.HttpURLConnection
        // which on modern Android versions uses OkHttp where
        // com.android.okhttp.okio.Timeout.throwIfReached returns InterruptedIOException.
        if (e is CloudAuthInterruptedIOException || e is InterruptedIOException) {
            Thread.currentThread().interrupt()
        }
        throw e
    }
}
