// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import android.content.Context
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.details.MovieDetails
import com.battlelancer.seriesguide.movies.tools.MovieTools.Companion.updateReleaseDateForRegion
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4.TmdbErrorResponse
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4.TmdbErrorResponse.IsNotFound
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4.TmdbNonNullResponse.Success
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.TextTools
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.services.MoviesService
import com.uwetrottmann.trakt5.entities.Ratings

/**
 * Helps download movie details.
 *
 * See also [MoviePosterDownloader].
 */
class MovieDownloader(
    private val context: Context,
    private val tmdbMovies: MoviesService,
    private val trakt: SgTrakt,
) {

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
    suspend fun getMovieDetails(
        languageCode: String,
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
                val movieTraktId = TraktTools.lookupMovieTraktId(trakt, movieTmdbId)
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
    suspend fun getMovieDetailsWithDefaults(
        movieTmdbId: Int,
        getTraktRating: Boolean
    ): MovieDetailsResult {
        val languageCode = MoviesSettings.getMoviesLanguage(context)
        val regionCode = MoviesSettings.getMoviesRegion(context)
        return getMovieDetails(languageCode, regionCode, movieTmdbId, getTraktRating)
    }

    private fun loadRatingsFromTrakt(movieTraktId: Int): Ratings? {
        try {
            val response = trakt.movies()
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
    ) {
        constructor(movie: Movie) : this(movie, false)
        constructor(isNotFoundOnTmdb: Boolean) : this(null, isNotFoundOnTmdb)
    }

    /**
     * Loads movie from TMDB and calls [updateReleaseDateForRegion] using [regionCode] on it.
     *
     * If there is no description for the given [languageCode], fetches the default description.
     * In this case and also if there is no description in the default language, adds a note that
     * there is no description in that language available.
     */
    private suspend fun getEnhancedMovieFromTmdb(
        languageCode: String,
        regionCode: String,
        movieTmdbId: Int
    ): EnhancedTmdbMovieResult {
        // Try to get movie details for desired language
        val movieResult = TmdbTools4.getMovieSummary(
            tmdbMovies,
            movieTmdbId,
            languageCode,
            includeReleaseDates = true,
            "get localized movie summary"
        )
        when (movieResult) {
            is Success -> {
                val movie = movieResult.data
                updateReleaseDateForRegion(movie, movie.release_dates, regionCode)

                // The title will never be empty, TMDB returns the title in the default language if
                // there is no translation. However, the overview might be empty if not translated.
                // So if there is no overview, try to get the default one.
                if (movie.overview.isNullOrEmpty()) {
                    movie.overview = getMovieDefaultOverviewFromTmdb(languageCode, movieTmdbId)
                }

                return EnhancedTmdbMovieResult(movie)
            }

            is IsNotFound -> {
                return EnhancedTmdbMovieResult(isNotFoundOnTmdb = true)
            }

            is TmdbErrorResponse.Other -> {
                return EnhancedTmdbMovieResult(isNotFoundOnTmdb = false)
            }
        }
    }

    private suspend fun getMovieDefaultOverviewFromTmdb(
        originalLanguageCode: String,
        movieTmdbId: Int
    ): String {
        // Try with default language if TMDb has no localized overview
        val fallbackResult = TmdbTools4.getMovieSummary(
            tmdbMovies,
            movieTmdbId,
            language = null,
            includeReleaseDates = false,
            "get default movie summary"
        )

        // Add note about non-translated or non-existing overview
        var overviewWithNote = TextTools.textNoTranslationMovieLanguage(
            context, originalLanguageCode,
            MoviesSettings.getMoviesLanguage(context)
        )

        // Add default overview
        if (fallbackResult is Success) {
            val fallbackMovie = fallbackResult.data
            val defaultOverview = fallbackMovie.overview
            if (!defaultOverview.isNullOrEmpty()) {
                overviewWithNote += "\n\n" + defaultOverview
            }
        }

        return overviewWithNote
    }

}