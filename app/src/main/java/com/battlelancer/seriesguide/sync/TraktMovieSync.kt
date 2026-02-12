// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2017 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import android.content.ContentProviderOperation
import android.content.OperationApplicationException
import com.battlelancer.seriesguide.movies.database.SgMovieFlags
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse.Success
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.entities.LastActivityMore
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import retrofit2.Response
import timber.log.Timber

/**
 * Syncs movie collection and watchlist and watched movies with the connected Trakt profile.
 */
class TraktMovieSync(
    private val traktSync: TraktSync
) {

    private val context = traktSync.context
    private val movieTools: MovieTools = traktSync.movieTools

    /**
     * Updates the local movie database against trakt movie watchlist, collection and watched
     * movies. Adds or updates movies in the database. Movies not in any list or not watched must be
     * removed afterwards.
     *
     * When syncing the first time, will upload any local movies missing from trakt collection
     * or watchlist or are not watched on Trakt instead of removing them locally.
     *
     * Performs **synchronous network access**, make sure to run this on a background
     * thread.
     *
     * Note: this uses [runBlocking], so if the calling thread is interrupted this will throw
     * [InterruptedException].
     */
    @Throws(InterruptedException::class)
    fun syncLists(activity: LastActivityMore): Boolean {
        val collectedAt = activity.collected_at
        if (collectedAt == null) {
            Timber.e("syncLists: null collected_at")
            return false
        }
        val watchlistedAt = activity.watchlisted_at
        if (watchlistedAt == null) {
            Timber.e("syncLists: null watchlisted_at")
            return false
        }
        val watchedAt = activity.watched_at
        if (watchedAt == null) {
            Timber.e("syncLists: null watched_at")
            return false
        }

        val isInitialSync = TraktSettings.isInitialSyncMovies(context)
        if (!isInitialSync
            && !TraktSettings.isMovieListsChanged(context, collectedAt, watchlistedAt, watchedAt)) {
            Timber.d("syncLists: no changes")
            return true
        }

        // Download Trakt state.
        val collection = runBlocking(Dispatchers.Default) {
            downloadCollection()
        } ?: return false
        val watchlist = runBlocking(Dispatchers.Default) {
            downloadWatchlist()
        } ?: return false
        val watchedWithPlays = runBlocking(Dispatchers.Default) {
            downloadWatched()
        } ?: return false

        // Loop through local movies to build updates.
        val localMovies: List<SgMovieFlags> = try {
            SgRoomDatabase.getInstance(context).movieHelper().getMovieFlags()
        } catch (e: Exception) {
            Errors.logAndReport("syncLists: query local movies", e)
            return false
        }

        val toCollectOnTrakt: MutableSet<Int> = HashSet() // only when merging
        val toWatchlistOnTrakt: MutableSet<Int> = HashSet() // only when merging
        val toSetWatchedOnTrakt: MutableSet<Int> = HashSet() // only when merging
        val batch = ArrayList<ContentProviderOperation>()

        for ((tmdbId, inCollection, inWatchlist, watched, plays) in localMovies) {
            // Is local movie in Trakt collection, watchlist or watched?
            val inCollectionOnTrakt = collection.remove(tmdbId)
            val inWatchlistOnTrakt = watchlist.remove(tmdbId)
            val playsOnTrakt = watchedWithPlays.remove(tmdbId)
            val isWatchedOnTrakt = playsOnTrakt != null

            if (isInitialSync) {
                // Mark movie for upload if missing from Trakt collection or watchlist
                // or if not watched on Trakt.
                // Note: If watches were removed on Trakt in the meanwhile, this would re-add them.
                // But this should be the exception and not losing watches should be the
                // desired behavior for most users.

                if (inCollection && !inCollectionOnTrakt) {
                    toCollectOnTrakt.add(tmdbId)
                }
                if (inWatchlist && !inWatchlistOnTrakt) {
                    toWatchlistOnTrakt.add(tmdbId)
                }
                if (watched && !isWatchedOnTrakt) {
                    toSetWatchedOnTrakt.add(tmdbId)
                }

                // Add to local collection or watchlist, but do NOT remove.
                // Mark as watched, but do NOT remove watched flag.
                // Will take care of removing unneeded (not watched or in any list) movies
                // in later sync step.
                if (inCollectionOnTrakt || inWatchlistOnTrakt || isWatchedOnTrakt) {
                    val builder = ContentProviderOperation.newUpdate(Movies.buildMovieUri(tmdbId))
                    var changed = false

                    if (!inCollection && inCollectionOnTrakt) {
                        builder.withValue(Movies.IN_COLLECTION, true)
                        changed = true
                    }
                    if (!inWatchlist && inWatchlistOnTrakt) {
                        builder.withValue(Movies.IN_WATCHLIST, true)
                        changed = true
                    }
                    if (!watched && isWatchedOnTrakt) {
                        builder.withValue(Movies.WATCHED, true)
                        builder.withValue(
                            Movies.PLAYS,
                            if (playsOnTrakt!! >= 1) playsOnTrakt else 1
                        )
                        changed = true
                    }

                    if (changed) {
                        batch.add(builder.build())
                    }
                }
            } else {
                // Performance: only add op if any flag differs or if watched and plays have changed.
                if (inCollection != inCollectionOnTrakt
                    || inWatchlist != inWatchlistOnTrakt
                    || watched != isWatchedOnTrakt
                    || (isWatchedOnTrakt && playsOnTrakt!! >= 1 && plays != playsOnTrakt)) {
                    // Mirror Trakt collection, watchlist, watched flag and plays.
                    // Note: unneeded (not watched or in any list) movies
                    // are removed in a later sync step.
                    val op = ContentProviderOperation
                        .newUpdate(Movies.buildMovieUri(tmdbId))
                        .withValue(Movies.IN_COLLECTION, inCollectionOnTrakt)
                        .withValue(Movies.IN_WATCHLIST, inWatchlistOnTrakt)
                        .withValue(Movies.WATCHED, isWatchedOnTrakt)
                    val playsValue: Int = if (isWatchedOnTrakt) {
                        if (playsOnTrakt!! >= 1) playsOnTrakt else 1
                    } else 0
                    op.withValue(Movies.PLAYS, playsValue)
                    batch.add(op.build())
                }
            }
        }

        // apply updates to existing movies
        try {
            DBUtils.applyInSmallBatches(context, batch)
            Timber.d("syncLists: updated %s", batch.size)
        } catch (e: OperationApplicationException) {
            Timber.e(e, "syncLists: database updates failed")
            return false
        }
        batch.clear() // release for gc

        // merge on first run
        if (isInitialSync) {
            // Upload movies not in Trakt collection, watchlist or watched history.
            if (uploadFlagsNotOnTrakt(
                    toCollectOnTrakt,
                    toWatchlistOnTrakt,
                    toSetWatchedOnTrakt
                )) {
                // set merge successful
                TraktSettings.setInitialSyncMoviesCompleted(context)
            } else {
                return false
            }
        }

        // add movies from trakt missing locally
        // all local movies were removed from trakt collection, watchlist, and watched list
        // so they only contain movies missing locally
        val addingSuccessful = runBlocking {
            movieTools.addMovies(collection, watchlist, watchedWithPlays)
        }
        if (addingSuccessful) {
            // store last activity timestamps
            TraktSettings.storeLastMoviesChangedAt(
                context,
                collectedAt,
                watchlistedAt,
                watchedAt
            )
            // if movies were added, ensure ratings for them are downloaded next
            if (collection.isNotEmpty() || watchlist.isNotEmpty() || watchedWithPlays.isNotEmpty()) {
                TraktSettings.resetMoviesLastRatedAt(context)
            }
        }
        return addingSuccessful
    }

    private suspend fun downloadCollection(): MutableSet<Int>? {
        return when (val response = TraktTools4.getCollectedMoviesByTmdbId(traktSync.sync)) {
            is Success -> response.data
            else -> null
        }
    }

    private suspend fun downloadWatchlist(): MutableSet<Int>? {
        return when (val response = TraktTools4.getMoviesOnWatchlistByTmdbId(traktSync.sync)) {
            is Success -> response.data
            else -> null
        }
    }

    private suspend fun downloadWatched(): MutableMap<Int, Int>? {
        return when (val response = TraktTools4.getWatchedMoviesByTmdbId(traktSync.sync)) {
            is Success -> response.data
            else -> null
        }
    }

    /**
     * Uploads the given movies to the appropriate list(s)/history on Trakt.
     */
    private fun uploadFlagsNotOnTrakt(
        toCollectOnTrakt: Set<Int>,
        toWatchlistOnTrakt: Set<Int>,
        toSetWatchedOnTrakt: Set<Int>
    ): Boolean {
        if (toCollectOnTrakt.isEmpty() && toWatchlistOnTrakt.isEmpty() && toSetWatchedOnTrakt.isEmpty()) {
            Timber.d("uploadLists: nothing to upload")
            return true
        }

        // Upload.
        var action = ""
        val items = SyncItems()
        var response: Response<SyncResponse?>? = null
        try {
            if (toCollectOnTrakt.isNotEmpty()) {
                val moviesToCollect = convertToSyncMovieList(toCollectOnTrakt)
                action = "add movies to collection"
                items.movies(moviesToCollect)
                response = traktSync.sync.addItemsToCollection(items).execute()
            }
            if (response == null || response.isSuccessful) {
                if (toWatchlistOnTrakt.isNotEmpty()) {
                    val moviesToWatchlist = convertToSyncMovieList(toWatchlistOnTrakt)
                    action = "add movies to watchlist"
                    items.movies(moviesToWatchlist)
                    response = traktSync.sync.addItemsToWatchlist(items).execute()
                }
            }
            if (response == null || response.isSuccessful) {
                if (toSetWatchedOnTrakt.isNotEmpty()) {
                    val moviesToSetWatched = convertToSyncMovieList(toSetWatchedOnTrakt)
                    // Note: not setting a watched date (because not having one),
                    // so Trakt will use the date of this upload.
                    action = "add movies to watched history"
                    items.movies(moviesToSetWatched)
                    response = traktSync.sync.addItemsToWatchedHistory(items).execute()
                }
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            return false
        }
        if (response != null && !response.isSuccessful) {
            traktSync.handleUnsuccessfulResponse(response, action)
            return false
        }

        Timber.d(
            "uploadLists: success, uploaded %s to collection, %s to watchlist, %s set watched",
            toCollectOnTrakt.size, toWatchlistOnTrakt.size, toSetWatchedOnTrakt.size
        )
        return true
    }

    private fun convertToSyncMovieList(movieTmdbIds: Set<Int>): List<SyncMovie> =
        movieTmdbIds.map { SyncMovie().id(MovieIds.tmdb(it)) }
}