// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import android.content.Context
import com.battlelancer.seriesguide.jobs.FlagJobExecutor
import com.battlelancer.seriesguide.jobs.movies.MovieCollectionJob
import com.battlelancer.seriesguide.jobs.movies.MovieWatchedJob
import com.battlelancer.seriesguide.jobs.movies.MovieWatchlistJob
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.movies.database.SgMovieFlags
import com.battlelancer.seriesguide.movies.database.toSgMovieForInsert
import com.battlelancer.seriesguide.movies.database.toSgMovieTmdbUpdate
import com.battlelancer.seriesguide.movies.database.toSgMovieTraktUpdate
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.ReleaseDate
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResults
import com.uwetrottmann.tmdb2.services.MoviesService
import timber.log.Timber
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

/**
 * Helps with loading movie details and adding or removing movies from [Lists].
 */
class MovieTools(
    private val context: Context,
    private val databaseHelper: MovieHelper,
    private val listHelper: SgListHelper,
    val downloader: MovieDownloader
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        tmdbMovies: MoviesService,
        trakt: SgTrakt
    ) : this(
        context,
        SgRoomDatabase.getInstance(context).movieHelper(),
        SgRoomDatabase.getInstance(context).sgListHelper(),
        MovieDownloader(context, tmdbMovies, trakt)
    )

    enum class Lists {
        COLLECTION,
        WATCHLIST,
        WATCHED
    }

    /**
     * If the movie is no longer on a custom or built-in list, deletes it from the database.
     * If it is on a custom list and isn't already in the database, adds it.
     *
     * Returns false if a database or network operation failed.
     *
     * @see addToList
     */
    suspend fun addToOrDeleteFromDatabaseAfterCustomListChange(movieTmdbId: Int): Boolean {
        if (isMovieNotOnCustomList(movieTmdbId)) {
            // Movie is no longer on a custom list, but maybe still on a built-in list?
            val movieFlags = databaseHelper.getMovieFlags(movieTmdbId)
                ?: return true // Already deleted from database (like by a concurrent sync)

            return if (movieFlags.isNotOnBuiltInList()) {
                deleteMovie(movieTmdbId)
            } else {
                true // Movie still on built-in list, don't delete
            }
        } else {
            // Movie was added to at least one custom list
            return if (isMovieInDatabase(movieTmdbId)) {
                true // Movie already in database, nothing to do
            } else {
                addMovie(movieTmdbId, null)
            }
        }
    }

    /**
     * Adds any movies that are on a custom list, but not in the database. Deletes any movies from
     * the database that are not on any built-in or custom list.
     */
    suspend fun updateDatabaseAfterCustomListChange(): Boolean {
        var hasError = false

        // Get all movies on custom lists, but not in the database and add them
        val moviesToAdd = listHelper.getTmdbIdsOfMovieListItemsNotInDatabase()
        for (movieTmdbId in moviesToAdd) {
            val success = addMovie(movieTmdbId, null)
            if (!success) {
                hasError = true
                Timber.e("Failed to add movie on custom list to database: tmdbId = %s", movieTmdbId)
            }
        }

        // Get all movies not on a built-in list and if they are not on a custom list, delete them
        val moviesToDelete = databaseHelper.getTmdbIdsOfMoviesNotOnAnyList()
        databaseHelper.deleteMovies(moviesToDelete)

        return !hasError
    }

    /**
     * Adds the movie to the given list. If it was not in any list before, adds the movie to the
     * local database first. Returns if the database operation was successful.
     *
     * @see addToOrDeleteFromDatabaseAfterCustomListChange
     */
    suspend fun addToList(movieTmdbId: Int, list: Lists): Boolean {
        val movieExists = isMovieInDatabase(movieTmdbId)
        return if (movieExists) {
            updateMovie(movieTmdbId, list, true)
        } else {
            addMovie(movieTmdbId, list)
        }
    }

    /**
     * Removes the movie from [listToRemoveFrom] or if the movie wouldn't be on any built-in or
     * custom list after removing it, deletes it from the database instead.
     *
     * @return If the database operation was successful.
     */
    fun removeFromList(movieTmdbId: Int, listToRemoveFrom: Lists): Boolean {
        val movieFlags = databaseHelper.getMovieFlags(movieTmdbId)
            ?: return false // query failed

        val newMovieFlags = when (listToRemoveFrom) {
            Lists.COLLECTION -> movieFlags.copy(inCollection = false)
            Lists.WATCHLIST -> movieFlags.copy(inWatchlist = false)
            Lists.WATCHED -> movieFlags.copy(watched = false)
        }

        return if (newMovieFlags.isNotOnBuiltInList() && isMovieNotOnCustomList(movieTmdbId)) {
            deleteMovie(movieTmdbId)
        } else {
            // otherwise, just update
            updateMovie(movieTmdbId, listToRemoveFrom, false)
        }
    }

    private fun SgMovieFlags.isNotOnBuiltInList(): Boolean =
        !inWatchlist && !inCollection && !watched

    private fun isMovieNotOnCustomList(movieTmdbId: Int): Boolean =
        listHelper
            .getListItemsWithTmdbIdCount(movieTmdbId, ListItemTypes.TMDB_MOVIE) == 0

    private fun isMovieInDatabase(movieTmdbId: Int): Boolean =
        databaseHelper.getCount(movieTmdbId) > 0

    /**
     * Returns `true` if the movie was updated.
     */
    private fun updateMovie(
        movieTmdbId: Int,
        list: Lists,
        value: Boolean
    ): Boolean {
        val rowsUpdated = when (list) {
            Lists.COLLECTION -> databaseHelper.updateInCollection(movieTmdbId, value)

            Lists.WATCHLIST -> databaseHelper.updateInWatchlist(movieTmdbId, value)

            Lists.WATCHED -> if (value) {
                databaseHelper.setWatchedAndAddPlay(movieTmdbId)
            } else {
                databaseHelper.setNotWatchedAndRemovePlays(movieTmdbId)
            }
        }

        return rowsUpdated > 0
    }

    /**
     * Returns `true` if the movie was deleted.
     */
    private fun deleteMovie(movieTmdbId: Int): Boolean {
        val rowsDeleted = databaseHelper.deleteMovie(movieTmdbId)
        Timber.d("deleteMovie: deleted %s movies", rowsDeleted)
        return rowsDeleted > 0
    }

    private suspend fun addMovie(movieTmdbId: Int, listToAddTo: Lists?): Boolean {
        // get movie info
        val details = downloader.getMovieDetailsWithDefaults(movieTmdbId, false).movieDetails
        if (details.tmdbMovie() == null) {
            // abort if minimal data failed to load
            return false
        }

        // build values
        details.isInCollection = listToAddTo == Lists.COLLECTION
        details.isInWatchlist = listToAddTo == Lists.WATCHLIST
        val isWatched = listToAddTo == Lists.WATCHED
        details.isWatched = isWatched
        details.plays = if (isWatched) 1 else 0

        // add to database
        val sgMovie = details.toSgMovieForInsert(movieTmdbId)
        databaseHelper.insertMovie(sgMovie)

        // ensure ratings for new movie are downloaded on next sync
        TraktSettings.resetMoviesLastRatedAt(context)

        return true
    }

    /**
     * Calls [updateMovieWithRowId] if a movie with the given [tmdbId] is in the database.
     */
    fun updateMovieWithTmdbId(tmdbId: Int, details: MovieDetails) {
        val rowId = databaseHelper.getMovieId(tmdbId)
            ?: return // Not in database
        updateMovieWithRowId(rowId, details)
    }

    /**
     * Updates existing movie with [details] from TMDB and, if they are not null, Trakt.
     */
    fun updateMovieWithRowId(rowId: Int, details: MovieDetails) {
        val movieTmdbUpdate = details.toSgMovieTmdbUpdate(rowId)
        if (movieTmdbUpdate != null) {
            databaseHelper.update(movieTmdbUpdate)
        }

        val movieTraktUpdate = details.toSgMovieTraktUpdate(rowId)
        if (movieTraktUpdate != null) {
            databaseHelper.update(movieTraktUpdate)
        }
    }

    /**
     * Adds new movies to the database.
     *
     * @param newCollectionMovies     Movie TMDB ids to add to the collection.
     * @param newWatchlistMovies      Movie TMDB ids to add to the watchlist.
     * @param newWatchedMoviesToPlays Movie TMDB ids to set watched mapped to play count.
     */
    suspend fun addMovies(
        newCollectionMovies: Set<Int>,
        newWatchlistMovies: Set<Int>,
        newWatchedMoviesToPlays: Map<Int, Int?>
    ): Boolean {
        Timber.d(
            "addMovies: %s to collection, %s to watchlist, %s to watched",
            newCollectionMovies.size,
            newWatchlistMovies.size,
            newWatchedMoviesToPlays.size
        )

        // build a single list of tmdb ids
        val newMovies: MutableSet<Int> = HashSet()
        newMovies.addAll(newCollectionMovies)
        newMovies.addAll(newWatchlistMovies)
        newMovies.addAll(newWatchedMoviesToPlays.keys)

        val languageCode = MoviesSettings.getMoviesLanguage(context)
        val regionCode = MoviesSettings.getMoviesRegion(context)
        val moviesToInsert = mutableListOf<SgMovie>()

        // loop through ids
        val iterator: Iterator<Int> = newMovies.iterator()
        while (iterator.hasNext()) {
            val tmdbId = iterator.next()
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("addMovies: no network connection")
                return false
            }

            // download movie data
            val result = downloader.getMovieDetails(languageCode, regionCode, tmdbId, false)
            if (result.isNotFoundOnTmdb) {
                Timber.w("addMovies: movie with TMDB ID %s not found, skipping", tmdbId)
                continue
            }
            val movieDetails = result.movieDetails
            if (movieDetails.tmdbMovie() == null) {
                Timber.e(
                    "addMovies: failed to load details for movie with TMDB ID %s, stopping",
                    tmdbId
                )
                return false
            }

            // set flags
            movieDetails.isInCollection = newCollectionMovies.contains(tmdbId)
            movieDetails.isInWatchlist = newWatchlistMovies.contains(tmdbId)
            val plays = newWatchedMoviesToPlays[tmdbId]
            val isWatched = plays != null
            movieDetails.isWatched = isWatched
            movieDetails.plays = (if (isWatched) plays else 0)

            moviesToInsert.add(movieDetails.toSgMovieForInsert(tmdbId))

            // Already add to the database if we have 10 movies so UI can already update.
            if (moviesToInsert.size == 10) {
                databaseHelper.insertMovies(moviesToInsert)
                moviesToInsert.clear() // Start a new batch.
            }
        }

        // Insert remaining new movies into the database.
        if (moviesToInsert.isNotEmpty()) {
            databaseHelper.insertMovies(moviesToInsert)
        }

        return true
    }

    companion object {

        /**
         * Date format using only numbers.
         */
        fun getMovieShortDateFormat(): DateFormat {
            // use SHORT as in some languages (Portuguese) the MEDIUM string is longer than expected
            return DateFormat.getDateInstance(DateFormat.SHORT)
        }

        /**
         * Return release date or null if unknown from millisecond value stored in the database as
         * [Movies.RELEASED_UTC_MS].
         */
        fun movieReleaseDateFrom(releaseDateMs: Long): Date? {
            return if (releaseDateMs == SgMovie.RELEASED_MS_UNKNOWN) null else Date(releaseDateMs)
        }

        /**
         * Replaces the release date of the movie with one of the given region, if available.
         * Picks the theatrical release or if not available the first date for that region.
         * This is not always the best approach, e.g. when viewing disc or digital releases this might
         * not display the correct date. But this is the best possible right now.
         */
        fun updateReleaseDateForRegion(
            movie: Movie,
            results: ReleaseDatesResults?,
            regionCode: String
        ) {
            results?.results?.find {
                it.iso_3166_1 == regionCode
            }?.let { region ->
                val releaseDates = region.release_dates ?: return // No release dates.

                // Only one date? Pick it.
                if (releaseDates.size == 1) {
                    releaseDates[0].release_date?.let { date ->
                        movie.release_date = date
                    }
                    return
                }

                // Pick the oldest theatrical release, if available.
                val theatricalRelease = releaseDates
                    .filter { it.type == ReleaseDate.TYPE_THEATRICAL }
                    .minOfOrNull { it.release_date }
                if (theatricalRelease != null) {
                    movie.release_date = theatricalRelease
                } else {
                    // Otherwise just get the first one, if available.
                    releaseDates[0]?.release_date?.let { date ->
                        movie.release_date = date
                    }
                }
            }
        }

        fun addToCollection(context: Context, movieTmdbId: Int) {
            FlagJobExecutor.execute(context, MovieCollectionJob(movieTmdbId, true))
        }

        fun addToWatchlist(context: Context, movieTmdbId: Int) {
            FlagJobExecutor.execute(context, MovieWatchlistJob(movieTmdbId, true))
        }

        fun removeFromCollection(context: Context, movieTmdbId: Int) {
            FlagJobExecutor.execute(context, MovieCollectionJob(movieTmdbId, false))
        }

        fun removeFromWatchlist(context: Context, movieTmdbId: Int) {
            FlagJobExecutor.execute(context, MovieWatchlistJob(movieTmdbId, false))
        }

        fun watchedMovie(
            context: Context,
            movieTmdbId: Int,
            currentPlays: Int,
            inWatchlist: Boolean
        ) {
            FlagJobExecutor.execute(
                context,
                MovieWatchedJob(movieTmdbId, true, currentPlays)
            )
            // trakt removes from watchlist automatically, but app would not show until next sync
            // and not mirror on hexagon, so do it manually
            if (inWatchlist) {
                removeFromWatchlist(context, movieTmdbId)
            }
        }

        fun unwatchedMovie(context: Context, movieTmdbId: Int) {
            FlagJobExecutor.execute(context, MovieWatchedJob(movieTmdbId, false, 0))
        }

        /**
         * Returns a set of the TMDb ids of all movies in the local database.
         *
         * @return null if there was an error, empty list if there are no movies.
         */
        fun getMovieTmdbIdsAsSet(context: Context): HashSet<Int>? {
            val localMoviesIds = HashSet<Int>()

            val movies = context.contentResolver.query(
                Movies.CONTENT_URI,
                arrayOf(Movies.TMDB_ID),
                null, null, null
            )
            if (movies == null) {
                return null
            }

            while (movies.moveToNext()) {
                localMoviesIds.add(movies.getInt(0))
            }

            movies.close()

            return localMoviesIds
        }

    }
}
