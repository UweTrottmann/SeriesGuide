// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2024 Uwe Trottmann

package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.settings.TmdbSettings.getImageBaseUrl
import com.uwetrottmann.tmdb2.entities.Genre

object TmdbTools {

    enum class ProfileImageSize(
        private val value: String
    ) {
        W45("w45"),
        W185("w185"),
        H632("h632"),
        ORIGINAL("original");

        override fun toString(): String {
            return value
        }
    }

    private const val BASE_URL = "https://www.themoviedb.org/"
    private const val PATH_TV = "tv/"
    private const val PATH_MOVIES = "movie/"
    private const val PATH_PERSON = "person/"

    @JvmStatic
    fun buildEpisodeUrl(showTmdbId: Int, season: Int, episode: Int): String {
        return "$BASE_URL$PATH_TV$showTmdbId/season/$season/episode/$episode"
    }

    @JvmStatic
    fun buildShowUrl(showTmdbId: Int): String {
        return BASE_URL + PATH_TV + showTmdbId
    }

    @JvmStatic
    fun buildMovieUrl(movieTmdbId: Int): String {
        return BASE_URL + PATH_MOVIES + movieTmdbId
    }

    fun buildPersonUrl(personTmdbId: Int): String {
        return BASE_URL + PATH_PERSON + personTmdbId
    }

    /**
     * Build URL to a profile image using the given size spec and current TMDB image url
     * (see [com.battlelancer.seriesguide.settings.TmdbSettings.getImageBaseUrl]).
     */
    fun buildProfileImageUrl(context: Context, path: String?, size: ProfileImageSize): String? {
        return if (path == null) {
            null
        } else {
            getImageBaseUrl(context) + size + path
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
