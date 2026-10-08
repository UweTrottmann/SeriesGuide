// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util

import android.content.Context
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.movies.tools.MovieDownloader
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4
import com.battlelancer.seriesguide.tmdbapi.TmdbTools4.TmdbNonNullResponse.Success
import com.uwetrottmann.tmdb2.services.MoviesService
import com.uwetrottmann.tmdb2.services.TvService

/**
 * Helps download show and movie details to build poster image URLs.
 *
 * See also [MovieDownloader].
 */
class PosterUrlDownloader(
    private val context: Context,
    private val tmdbTv: TvService,
    private val tmdbMovies: MoviesService
) {

    /**
     * Downloads movie info for [MoviesSettings.getMoviesLanguage] and returns the poster image URL.
     */
    suspend fun getMoviePosterUrl(movieTmdbId: Int): String? {
        val languageCode = MoviesSettings.getMoviesLanguage(context)
        val result = TmdbTools4().getMovieSummary(
            tmdbMovies,
            movieTmdbId,
            languageCode,
            includeReleaseDates = false,
            "get movie poster"
        )
        if (result !is Success) return null
        val posterPath = result.data.poster_path ?: return null
        return TmdbTools.buildLargePosterUrl(context, posterPath)
            .let { ImageTools.buildImageCacheUrl(it) }
    }

    /**
     * Downloads show details for the given [language] and returns the poster image URL.
     */
    suspend fun getShowPosterUrl(showTmdbId: Int, language: String): String? {
        val result = TmdbTools4().getShowDetails(tmdbTv, showTmdbId, language)
        return if (result is Success) {
            ImageTools.tmdbOrTvdbPosterUrl(result.data.poster_path, context, false)
        } else {
            null
        }
    }

}
