// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.settings.DisplaySettings.isVeryHighDensityScreen
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.uwetrottmann.tmdb2.entities.Genre

object TmdbTools {

    private const val IMAGE_SIZE_SPEC_ORIGINAL = "original"
    const val POSTER_SIZE_SPEC_W154 = "w154"
    const val POSTER_SIZE_SPEC_W342 = "w342"
    /**
     * As of 2024-11:
     *
     * - w300 (300 × 169 px) JPEG is around 9-16 kB
     * - w780 (780 × 439 px) JPEG is around 30-70 kB
     * - w1280 (1280 × 720 px) JPEG is around 60-140 KB
     *
     * Samples:
     *
     * - https://image.tmdb.org/t/p/original/8Tvnx22rzhwArofFPhmcfaBvgjN.jpg (smallest)
     * - https://image.tmdb.org/t/p/original/j4WEC9Jh4AyXF8ynpX3pz633tse.jpg
     * - https://image.tmdb.org/t/p/original/5Bh7EE3p6OOS0NzH22AE0N7DYO8.jpg (largest)
     */
    const val BACKDROP_SMALL_SIZE_SPEC = "w780"

    enum class ProfileImageSize(
        private val value: String
    ) {
        W45("w45"),
        W185("w185"),
        H632("h632"),
        ORIGINAL(IMAGE_SIZE_SPEC_ORIGINAL);

        override fun toString(): String {
            return value
        }
    }

    private const val BASE_URL = "https://www.themoviedb.org"
    private const val PATH_TV = "tv"
    private const val PATH_MOVIES = "movie"
    private const val PATH_PERSON = "person"

    @JvmStatic
    fun buildEpisodeUrl(showTmdbId: Int, season: Int, episode: Int): String {
        return "$BASE_URL/$PATH_TV/$showTmdbId/season/$season/episode/$episode"
    }

    @JvmStatic
    fun buildShowUrl(showTmdbId: Int): String {
        return "$BASE_URL/$PATH_TV/$showTmdbId"
    }

    @JvmStatic
    fun buildMovieUrl(movieTmdbId: Int): String {
        return "$BASE_URL/$PATH_MOVIES/$movieTmdbId"
    }

    fun buildPersonUrl(personTmdbId: Int): String {
        return "$BASE_URL/$PATH_PERSON/$personTmdbId"
    }

    fun buildMovieReleaseDatesUrl(movieTmdbId: Int): String {
        return "${buildMovieUrl(movieTmdbId)}/releases"
    }

    /**
     * Returns base image URL based on screen density.
     */
    fun getPosterBaseUrl(context: Context): String {
        return if (isVeryHighDensityScreen(context)) {
            TmdbSettings.getImageBaseUrl(context) + POSTER_SIZE_SPEC_W342
        } else {
            TmdbSettings.getImageBaseUrl(context) + POSTER_SIZE_SPEC_W154
        }
    }

    fun buildLargePosterUrl(context: Context, path: String): String =
        TmdbSettings.getImageBaseUrl(context) + POSTER_SIZE_SPEC_W342 + path

    fun buildOriginalSizeImageUrl(context: Context, path: String): String =
        TmdbSettings.getImageBaseUrl(context) + IMAGE_SIZE_SPEC_ORIGINAL + path

    fun buildBackdropUrl(context: Context, path: String): String =
        TmdbSettings.getImageBaseUrl(context) + BACKDROP_SMALL_SIZE_SPEC + path

    /**
     * Build URL to a profile image using the given size spec and current TMDB image url
     * (see [TmdbSettings.getImageBaseUrl]).
     */
    fun buildProfileImageUrl(context: Context, path: String?, size: ProfileImageSize): String? {
        return if (path == null) {
            null
        } else {
            TmdbSettings.getImageBaseUrl(context) + size + path
        }
    }

    /**
     * Builds a string listing all given genres by name, separated by comma.
     */
    fun buildGenresString(genres: List<Genre>?): String? {
        if (genres.isNullOrEmpty()) {
            return null
        }
        val genresString = StringBuilder()
        for (i in genres.indices) {
            val genre = genres[i]
            genresString.append(genre.name)
            if (i + 1 < genres.size) {
                genresString.append(", ")
            }
        }
        return genresString.toString()
    }
}
