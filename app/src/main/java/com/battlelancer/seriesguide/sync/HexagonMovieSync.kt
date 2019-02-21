package com.battlelancer.seriesguide.sync

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.preference.PreferenceManager
import android.text.TextUtils
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.ui.movies.MovieTools
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.google.api.client.util.DateTime
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.backend.movies.model.Movie
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList
import timber.log.Timber
import java.io.IOException
import java.util.ArrayList

internal class HexagonMovieSync(
    private val context: Context,
    private val hexagonTools: HexagonTools
) {

    /**
     * Downloads movies from hexagon, updates existing movies with new properties, removes
     * movies that are neither in collection or watchlist or watched.
     *
     *
     *  Adds movie tmdb ids of new movies to the respective collection, watchlist or watched set.
     */
    fun download(
        newCollectionMovies: MutableSet<Int>,
        newWatchlistMovies: MutableSet<Int>,
        newWatchedMovies: MutableSet<Int>,
        hasMergedMovies: Boolean
    ): Boolean {
        var movies: List<Movie>?
        var hasMoreMovies = true
        var cursor: String? = null
        val currentTime = System.currentTimeMillis()
        val lastSyncTime = DateTime(HexagonSettings.getLastMoviesSyncTime(context))
        val localMovies = MovieTools.getMovieTmdbIdsAsSet(context)
        if (localMovies == null) {
            Timber.e("download: querying for local movies failed.")
            return false
        }

        if (hasMergedMovies) {
            Timber.d("download: movies changed since %s", lastSyncTime)
        } else {
            Timber.d("download: all movies")
        }

        var updatedCount = 0
        var removedCount = 0

        while (hasMoreMovies) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("download: no network connection")
                return false
            }

            try {
                // get service each time to check if auth was removed
                val moviesService = hexagonTools.moviesService ?: return false

                val request = moviesService.get()  // use default server limit
                if (hasMergedMovies) {
                    request.updatedSince = lastSyncTime
                }
                if (!TextUtils.isEmpty(cursor)) {
                    request.cursor = cursor
                }

                val response = request.execute()
                if (response == null) {
                    // nothing more to do
                    Timber.d("download: response was null, done here")
                    break
                }

                movies = response.movies

                if (response.cursor != null) {
                    cursor = response.cursor
                } else {
                    hasMoreMovies = false
                }
            } catch (e: IOException) {
                Errors.logAndReportHexagon("get movies", e)
                return false
            }

            if (movies == null || movies.isEmpty()) {
                // nothing more to do
                break
            }

            val batch = ArrayList<ContentProviderOperation>()
            for (movie in movies) {
                if (localMovies.contains(movie.tmdbId)) {
                    // movie is in database
                    if (movie.isInCollection == false
                        && movie.isInWatchlist == false
                        && movie.isWatched == false) {
                        // if no longer in watchlist, collection or watched: remove movie
                        // note: this is backwards compatible with watched movies downloaded
                        // by trakt as those will have a null watched flag on Cloud
                        batch.add(
                            ContentProviderOperation.newDelete(
                                SeriesGuideContract.Movies.buildMovieUri(movie.tmdbId)
                            ).build()
                        )
                        removedCount++
                    } else {
                        // update collection, watchlist and watched flags
                        val values = ContentValues().apply {
                            putIfNotNull(
                                movie.isInCollection,
                                SeriesGuideContract.Movies.IN_COLLECTION
                            )
                            putIfNotNull(
                                movie.isInWatchlist,
                                SeriesGuideContract.Movies.IN_WATCHLIST
                            )
                            putIfNotNull(
                                movie.isWatched,
                                SeriesGuideContract.Movies.WATCHED
                            )
                        }
                        batch.add(
                            ContentProviderOperation.newUpdate(
                                SeriesGuideContract.Movies.buildMovieUri(movie.tmdbId)
                            ).withValues(values).build()
                        )
                        updatedCount++
                    }
                } else {
                    // schedule movie to be added
                    if (movie.isInCollection == true) {
                        newCollectionMovies.add(movie.tmdbId)
                    }
                    if (movie.isInWatchlist == true) {
                        newWatchlistMovies.add(movie.tmdbId)
                    }
                    if (movie.isWatched == true) {
                        newWatchedMovies.add(movie.tmdbId)
                    }
                }
            }

            try {
                DBUtils.applyInSmallBatches(context, batch)
            } catch (e: OperationApplicationException) {
                Timber.e(e, "download: applying movie updates failed")
                return false
            }

        }

        Timber.d("download: updated %d and removed %d movies", updatedCount, removedCount)

        // set new last sync time
        if (hasMergedMovies) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(HexagonSettings.KEY_LAST_SYNC_MOVIES, currentTime)
                .apply()
        }

        return true
    }

    private fun ContentValues.putIfNotNull(value: Boolean?, key: String) {
        if (value != null) {
            put(key, if (value) 1 else 0)
        }
    }

    /**
     * Uploads all local movies to Hexagon.
     */
    fun uploadAll(): Boolean {
        Timber.d("uploadAll: uploading all movies")

        val movies = buildMovieList()
        if (movies == null) {
            Timber.e("uploadAll: movie query was null")
            return false
        }
        if (movies.isEmpty()) {
            // nothing to do
            Timber.d("uploadAll: no movies to upload")
            return true
        }

        val movieList = MovieList()
        movieList.movies = movies

        try {
            // get service each time to check if auth was removed
            val moviesService = hexagonTools.moviesService ?: return false
            moviesService.save(movieList).execute()
        } catch (e: IOException) {
            Errors.logAndReportHexagon("save movies", e)
            return false
        }

        return true
    }

    private fun buildMovieList(): List<Movie>? {
        val movies = ArrayList<Movie>()

        // query for movies in lists or that are watched
        val moviesInListsOrWatched = context.contentResolver.query(
            SeriesGuideContract.Movies.CONTENT_URI,
            SeriesGuideContract.Movies.PROJECTION_IN_LIST_OR_WATCHED,
            SeriesGuideContract.Movies.SELECTION_IN_LIST_OR_WATCHED, null, null
        ) ?: return null

        while (moviesInListsOrWatched.moveToNext()) {
            val movie = Movie()
            movie.tmdbId = moviesInListsOrWatched.getInt(0)
            movie.isInCollection = moviesInListsOrWatched.getIntAsBoolean(1)
            movie.isInWatchlist = moviesInListsOrWatched.getIntAsBoolean(2)
            movie.isWatched = moviesInListsOrWatched.getIntAsBoolean(3)
            movies.add(movie)
        }

        moviesInListsOrWatched.close()

        return movies
    }

    private fun Cursor.getIntAsBoolean(columnIndex: Int): Boolean {
        return getInt(columnIndex) == 1
    }

}
