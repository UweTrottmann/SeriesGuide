package com.battlelancer.seriesguide.sync

import android.content.ContentProviderOperation
import android.content.OperationApplicationException
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.movies.database.SgMovieFlags
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.BaseMovie
import com.uwetrottmann.trakt5.entities.LastActivityMore
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncResponse
import retrofit2.Response
import timber.log.Timber
import kotlin.collections.set

/**
 * Syncs movie collection and watchlist and watched movies with the connected Trakt profile.
 */
class TraktMovieSync(
    private val traktSync: TraktSync
) {

    private val context = traktSync.context
    private val movieTools = traktSync.movieTools

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
     */
    fun syncLists(activity: LastActivityMore): Boolean {
        if (activity.collected_at == null) {
            Timber.e("syncLists: null collected_at")
            return false
        }
        if (activity.watchlisted_at == null) {
            Timber.e("syncLists: null watchlisted_at")
            return false
        }
        if (activity.watched_at == null) {
            Timber.e("syncLists: null watched_at")
            return false
        }

        val merging = !TraktSettings.hasMergedMovies(context)
        if (!merging && !TraktSettings.isMovieListsChanged(
                context, activity.collected_at, activity.watchlisted_at, activity.watched_at
            )) {
            Timber.d("syncLists: no changes")
            return true
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false
        }

        // Download Trakt state.
        val collection = downloadCollection() ?: return false
        val watchlist = downloadWatchlist() ?: return false
        val watchedWithPlays = downloadWatched() ?: return false

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

            if (merging) {
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
        if (merging) {
            // Upload movies not in Trakt collection, watchlist or watched history.
            if (uploadFlagsNotOnTrakt(
                    toCollectOnTrakt,
                    toWatchlistOnTrakt,
                    toSetWatchedOnTrakt
                )) {
                // set merge successful
                PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, true)
                    .apply()
            } else {
                return false
            }
        }

        // add movies from trakt missing locally
        // all local movies were removed from trakt collection, watchlist, and watched list
        // so they only contain movies missing locally
        val addingSuccessful = movieTools.addMovies(collection, watchlist, watchedWithPlays)
        if (addingSuccessful) {
            // store last activity timestamps
            TraktSettings.storeLastMoviesChangedAt(
                context,
                activity.collected_at,
                activity.watchlisted_at,
                activity.watched_at
            )
            // if movies were added, ensure ratings for them are downloaded next
            if (collection.isNotEmpty() || watchlist.isNotEmpty() || watchedWithPlays.isNotEmpty()) {
                TraktSettings.resetMoviesLastRatedAt(context)
            }
        }
        return addingSuccessful
    }

    private fun downloadCollection(): MutableSet<Int>? {
        return try {
            val response = traktSync.sync
                .collectionMovies(null)
                .execute()
            val collection = verifyListResponse(
                response,
                "null collection response", ACTION_GET_COLLECTION
            )
            toTmdbIdSet(collection)
        } catch (e: Exception) {
            Errors.logAndReport(ACTION_GET_COLLECTION, e)
            null
        }
    }

    private fun downloadWatchlist(): MutableSet<Int>? {
        return try {
            val response = traktSync.sync
                .watchlistMovies(null)
                .execute()
            val watchlist = verifyListResponse(
                response,
                "null watchlist response", ACTION_GET_WATCHLIST
            )
            toTmdbIdSet(watchlist)
        } catch (e: Exception) {
            Errors.logAndReport(ACTION_GET_WATCHLIST, e)
            null
        }
    }

    private fun downloadWatched(): MutableMap<Int, Int>? {
        return try {
            val response = traktSync.sync
                .watchedMovies(null)
                .execute()
            val watched = verifyListResponse(
                response,
                "null watched response", ACTION_GET_WATCHED
            )
            mapTmdbIdToPlays(watched)
        } catch (e: Exception) {
            Errors.logAndReport(ACTION_GET_WATCHED, e)
            null
        }
    }

    private fun verifyListResponse(
        response: Response<List<BaseMovie>>,
        nullResponse: String,
        action: String
    ): List<BaseMovie>? {
        return if (response.isSuccessful) {
            val movies = response.body()
            if (movies == null) {
                Timber.e(nullResponse)
            }
            movies
        } else {
            if (SgTrakt.isUnauthorized(context, response)) {
                return null
            }
            Errors.logAndReport(action, response)
            null
        }
    }

    private fun toTmdbIdSet(movies: List<BaseMovie>?): MutableSet<Int>? {
        if (movies == null) {
            return null
        }
        val tmdbIdSet: MutableSet<Int> = HashSet()
        for (movie in movies) {
            val tmdbId = movie.movie?.ids?.tmdb
                ?: continue  // skip invalid values
            tmdbIdSet.add(tmdbId)
        }
        return tmdbIdSet
    }

    private fun mapTmdbIdToPlays(movies: List<BaseMovie>?): MutableMap<Int, Int>? {
        if (movies == null) {
            return null
        }
        val map: MutableMap<Int, Int> = HashMap()
        for (movie in movies) {
            val tmdbId = movie.movie?.ids?.tmdb
                ?: continue  // skip invalid values
            map[tmdbId] = movie.plays
        }
        return map
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

        if (!AndroidUtils.isNetworkConnected(context)) {
            return false // Fail, no connection is available.
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

    companion object {
        private const val ACTION_GET_COLLECTION = "get movie collection"
        private const val ACTION_GET_WATCHLIST = "get movie watchlist"
        private const val ACTION_GET_WATCHED = "get watched movies"
    }
}