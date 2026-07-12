// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.tmdbapi

import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.FindResults
import com.uwetrottmann.tmdb2.enumerations.ExternalSource
import com.uwetrottmann.tmdb2.services.FindService

class TmdbFindTools(
    private val findService: FindService,
    private val languageCode: String?
) : TmdbTools4() {

    /**
     * Tries to look up a movie using an IMDB ID.
     */
    suspend fun findMovieByImdbId(
        imdbId: String,
    ): TmdbNonNullResponse<BaseMovie> {
        val response: TmdbNonNullResponse<FindResults> = awaitTmdbCall(
            findService.find(imdbId, ExternalSource.IMDB_ID, languageCode),
            "find movie via imdb id"
        )

        return when (response) {
            is TmdbNonNullResponse.Success -> {
                val movie = response.data.movie_results?.firstOrNull()
                if (movie == null) {
                    TmdbErrorResponse.IsNotFound()
                } else {
                    TmdbNonNullResponse.Success(movie)
                }
            }

            // This should never return 404 Not Found, but map it in case it ever does
            is TmdbErrorResponse.IsNotFound -> TmdbErrorResponse.IsNotFound()
            is TmdbErrorResponse.Other -> TmdbErrorResponse.Other()
        }
    }

}