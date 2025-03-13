// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann

package com.battlelancer.seriesguide.movies.tools

import android.content.ContentValues
import android.content.Context
import android.text.TextUtils
import com.battlelancer.seriesguide.jobs.FlagJobExecutor
import com.battlelancer.seriesguide.jobs.movies.MovieCollectionJob
import com.battlelancer.seriesguide.jobs.movies.MovieWatchedJob
import com.battlelancer.seriesguide.jobs.movies.MovieWatchlistJob
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieTools.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.ReleaseDate
import com.uwetrottmann.tmdb2.entities.ReleaseDatesResults
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.services.MoviesService
import com.uwetrottmann.trakt5.entities.Ratings
import dagger.Lazy
import timber.log.Timber
import java.text.DateFormat
import java.util.Date
import java.util.LinkedList
import javax.inject.Inject

/**
 * Helps with loading movie details and adding or removing movies from [Lists].
 */
class MovieTools @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tmdbMovies: Lazy<MoviesService>,
    private val trakt: Lazy<SgTrakt>
) {

    enum class Lists {
        COLLECTION,
        WATCHLIST,
        WATCHED
    }

    /**
     * Adds the movie to the given list. If it was not in any list before, adds the movie to the
     * local database first. Returns if the database operation was successful.
     */
    fun addToList(movieTmdbId: Int, list: Lists): Boolean {
        val movieExists = isMovieInDatabase(movieTmdbId)
        return if (movieExists) {
            updateMovie(context, movieTmdbId, list, true)
        } else {
            addMovie(movieTmdbId, list)
        }
    }

    private fun isMovieInDatabase(movieTmdbId: Int): Boolean {
        val count = SgRoomDatabase.getInstance(context).movieHelper().getCount(movieTmdbId)
        return count > 0
    }

    private fun addMovie(movieTmdbId: Int, listToAddTo: Lists): Boolean {
        // get movie info
        val details = getMovieDetailsWithDefaults(movieTmdbId, false).movieDetails
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
        val values = details.toContentValuesInsert()

        // add to database
        context.contentResolver.insert(Movies.CONTENT_URI, values)

        // ensure ratings for new movie are downloaded on next sync
        TraktSettings.resetMoviesLastRatedAt(context)

        return true
    }

    /**
     * Updates existing movie. If movie does not exist in database, will do nothing.
     */
    fun updateMovie(details: MovieDetails, tmdbId: Int) {
        val values = details.toContentValuesUpdate()
        if (values.size() == 0) {
            return  // nothing to update, downloading probably failed :(
        }

        values.put(Movies.LAST_UPDATED, System.currentTimeMillis())

        context.contentResolver.update(Movies.buildMovieUri(tmdbId), values, null, null)
    }

    /**
     * Adds new movies to the database.
     *
     * @param newCollectionMovies     Movie TMDB ids to add to the collection.
     * @param newWatchlistMovies      Movie TMDB ids to add to the watchlist.
     * @param newWatchedMoviesToPlays Movie TMDB ids to set watched mapped to play count.
     */
    fun addMovies(
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
        val movies: MutableList<MovieDetails> = LinkedList()

        // loop through ids
        val iterator: Iterator<Int> = newMovies.iterator()
        while (iterator.hasNext()) {
            val tmdbId = iterator.next()
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("addMovies: no network connection")
                return false
            }

            // download movie data
            val result = getMovieDetails(languageCode, regionCode, tmdbId, false)
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
            movieDetails.plays = (if (isWatched) plays else 0)!!

            movies.add(movieDetails)

            // Already add to the database if we have 10 movies so UI can already update.
            if (movies.size == 10) {
                context.contentResolver.bulkInsert(
                    Movies.CONTENT_URI,
                    buildMoviesContentValues(movies)
                )
                movies.clear() // Start a new batch.
            }
        }

        // Insert remaining new movies into the database.
        if (movies.isNotEmpty()) {
            context.contentResolver.bulkInsert(
                Movies.CONTENT_URI,
                buildMoviesContentValues(movies)
            )
        }

        return true
    }

    data class MovieDetailsResult(
        val movieDetails: MovieDetails,
        val isNotFoundOnTmdb: Boolean
    )

    /**
     * Download movie data from TMDB and if [getTraktRating] ratings from Trakt.
     *
     * Fetching the rating from Trakt requires to look up the Trakt ID first, so skip if not
     * necessary.
     */
    fun getMovieDetails(
        languageCode: String?,
        regionCode: String,
        movieTmdbId: Int,
        getTraktRating: Boolean
    ): MovieDetailsResult {
        val details = MovieDetails()

        // Load movie details from TMDB
        val tmdbResult = getEnhancedMovieFromTmdb(languageCode, regionCode, movieTmdbId)
        details.tmdbMovie(tmdbResult.movie)

        // Optionally load ratings from Trakt
        if (tmdbResult.movie != null) {
            if (getTraktRating) {
                val movieTraktId = TraktTools.lookupMovieTraktId(trakt.get(), movieTmdbId)
                if (movieTraktId != null) {
                    details.traktRatings(loadRatingsFromTrakt(movieTraktId))
                }
            }
        }

        return MovieDetailsResult(details, isNotFoundOnTmdb = tmdbResult.isNotFoundOnTmdb)
    }

    /**
     * Like [getMovieDetails], but uses [MoviesSettings.getMoviesLanguage]
     * and [MoviesSettings.getMoviesRegion].
     */
    fun getMovieDetailsWithDefaults(movieTmdbId: Int, getTraktRating: Boolean): MovieDetailsResult {
        val languageCode = MoviesSettings.getMoviesLanguage(context)
        val regionCode = MoviesSettings.getMoviesRegion(context)
        return getMovieDetails(languageCode, regionCode, movieTmdbId, getTraktRating)
    }

    private fun loadRatingsFromTrakt(movieTraktId: Int): Ratings? {
        try {
            val response = trakt.get().movies()
                .ratings(movieTraktId.toString())
                .execute()
            if (response.isSuccessful) {
                return response.body()
            }
            Errors.logAndReport("get movie rating", response)
        } catch (e: Exception) {
            Errors.logAndReport("get movie rating", e)
        }
        return null
    }

    data class EnhancedTmdbMovieResult(
        val movie: Movie?,
        val isNotFoundOnTmdb: Boolean
    )

    /**
     * Loads movie from TMDB and calls [updateReleaseDateForRegion] using [regionCode] on it.
     *
     * If there is no description for the given [languageCode], fetches the default description.
     * In this case and also if there is no description in the default language, adds a note that
     * there is no description in that language available.
     */
    private fun getEnhancedMovieFromTmdb(
        languageCode: String?,
        regionCode: String,
        movieTmdbId: Int
    ): EnhancedTmdbMovieResult {
        // Try to get movie details for desired language
        val movie = getMovieSummary(
            "get localized movie details",
            languageCode, movieTmdbId, true
        )
        if (movie != null && !TextUtils.isEmpty(movie.overview)) {
            updateReleaseDateForRegion(movie, movie.release_dates, regionCode)
            return EnhancedTmdbMovieResult(movie, isNotFoundOnTmdb = false)
        }

        // fall back to default language if TMDb has no localized text
        val movieFallback = getMovieSummary(
            "get default movie summary",
            null, movieTmdbId, false
        )
        if (movieFallback != null) {
            // add note about non-translated or non-existing overview
            val untranslatedOverview = movieFallback.overview
            var overview = TextTools.textNoTranslationMovieLanguage(
                context, languageCode,
                MoviesSettings.getMoviesLanguage(context)
            )
            if (!TextUtils.isEmpty(untranslatedOverview)) {
                overview += "\n\n" + untranslatedOverview
            }
            movieFallback.overview = overview
            if (movie != null) {
                updateReleaseDateForRegion(movie, movie.release_dates, regionCode)
            }
        }

        return EnhancedTmdbMovieResult(movieFallback, isNotFoundOnTmdb = false)
    }

    fun getMovieSummary(movieTmdbId: Int): Movie? {
        val languageCode = MoviesSettings.getMoviesLanguage(context)
        return getMovieSummary("get local movie summary", languageCode, movieTmdbId, false)
    }

    private fun getMovieSummary(
        action: String,
        language: String?,
        movieTmdbId: Int,
        includeReleaseDates: Boolean
    ): Movie? {
        try {
            val response = tmdbMovies.get()
                .summary(
                    movieTmdbId,
                    language,
                    if (includeReleaseDates)
                        AppendToResponse(AppendToResponseItem.RELEASE_DATES)
                    else
                        null
                )
                .execute()
            if (response.isSuccessful) {
                return response.body()
            } else {
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
        }
        return null
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
            return if (releaseDateMs == Long.MAX_VALUE) null else Date(releaseDateMs)
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

        /**
         * Deletes all movies which are not watched and not in any list.
         */
        fun deleteUnusedMovies(context: Context) {
            val rowsDeleted = context.contentResolver
                .delete(
                    Movies.CONTENT_URI,
                    "${Movies.SELECTION_UNWATCHED} AND ${Movies.SELECTION_NOT_COLLECTION} AND ${Movies.SELECTION_NOT_WATCHLIST}",
                    null
                )
            Timber.d("deleteUnusedMovies: removed %s movies", rowsDeleted)
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

        /**
         * Removes the movie from the given list.
         *
         * If it would not be on any list afterwards, deletes the movie from the local database.
         *
         * @return If the database operation was successful.
         */
        fun removeFromList(context: Context, movieTmdbId: Int, listToRemoveFrom: Lists): Boolean {
            val movieFlags = SgRoomDatabase.getInstance(context).movieHelper()
                .getMovieFlags(movieTmdbId)
            if (movieFlags == null) {
                return false // query failed
            }

            var removeMovie = false
            if (listToRemoveFrom == Lists.COLLECTION) {
                removeMovie = !movieFlags.inWatchlist && !movieFlags.watched
            } else if (listToRemoveFrom == Lists.WATCHLIST) {
                removeMovie = !movieFlags.inCollection && !movieFlags.watched
            } else if (listToRemoveFrom == Lists.WATCHED) {
                removeMovie = !movieFlags.inCollection && !movieFlags.inWatchlist
            }

            // if movie will not be in any list, remove it completely
            return if (removeMovie) {
                deleteMovie(context, movieTmdbId)
            } else {
                // otherwise, just update
                updateMovie(context, movieTmdbId, listToRemoveFrom, false)
            }
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

        private fun buildMoviesContentValues(movies: List<MovieDetails>): Array<ContentValues> {
            return movies.map { it.toContentValuesInsert() }.toTypedArray()
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

        /**
         * Returns `true` if the movie was updated.
         */
        private fun updateMovie(
            context: Context,
            movieTmdbId: Int,
            list: Lists,
            value: Boolean
        ): Boolean {
            val helper = SgRoomDatabase.getInstance(context).movieHelper()

            val rowsUpdated = when (list) {
                Lists.COLLECTION -> helper.updateInCollection(movieTmdbId, value)

                Lists.WATCHLIST -> helper.updateInWatchlist(movieTmdbId, value)

                Lists.WATCHED -> if (value) {
                    helper.setWatchedAndAddPlay(movieTmdbId)
                } else {
                    helper.setNotWatchedAndRemovePlays(movieTmdbId)
                }
            }

            // As some movie lists still use the old ContentProvider, notify the movie URI.
            context.contentResolver.notifyChange(Movies.CONTENT_URI, null)

            return rowsUpdated > 0
        }

        /**
         * Returns `true` if the movie was deleted.
         */
        private fun deleteMovie(context: Context, movieTmdbId: Int): Boolean {
            val rowsDeleted = SgRoomDatabase.getInstance(context).movieHelper()
                .deleteMovie(movieTmdbId)
            Timber.d("deleteMovie: deleted %s movies", rowsDeleted)

            // As some movie lists still use the old ContentProvider, notify the movie URI.
            context.contentResolver.notifyChange(Movies.CONTENT_URI, null)

            return rowsDeleted > 0
        }
    }
}
