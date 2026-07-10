// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import android.content.Context
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4.TmdbNonNullResponse.Success
import com.uwetrottmann.tmdb2.services.MoviesService

/**
 * Helps download movie poster details.
 *
 * See also [MovieDownloader].
 */
class MoviePosterDownloader(
    private val context: Context,
    private val tmdbMovies: MoviesService
) {

    /**
     * Downloads movie info for [MoviesSettings.getMoviesLanguage] and returns only the poster path.
     */
    suspend fun getMoviePosterPath(movieTmdbId: Int): String? {
        val languageCode = MoviesSettings.getMoviesLanguage(context)
        val result = TmdbTools4().getMovieSummary(
            tmdbMovies,
            movieTmdbId,
            languageCode,
            includeReleaseDates = false,
            "get movie poster"
        )
        return if (result is Success) {
            result.data.poster_path
        } else {
            null
        }
    }

}