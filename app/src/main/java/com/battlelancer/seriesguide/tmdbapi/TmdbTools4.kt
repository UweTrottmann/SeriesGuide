// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.tmdbapi

import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.services.MoviesService
import retrofit2.Call
import retrofit2.awaitResponse

/**
 * Uses response classes inheriting from a Kotlin sealed interface.
 *
 * Removes any Android specific classes and no longer relies on a third-party library to handle
 * results.
 */
object TmdbTools4 {

    sealed interface TmdbNonNullResponse<T> {
        data class Success<T>(val data: T) : TmdbNonNullResponse<T>
    }

    sealed interface TmdbErrorResponse {
        class IsNotFound<T> : TmdbNonNullResponse<T>
        class Other<T> : TmdbNonNullResponse<T>
    }

    /**
     * Get movie [MoviesService.summary] from TMDB, optionally [includeReleaseDates].
     *
     * Pass a `null` [language] to get the default language (English).
     *
     * [action] is used for logging and error reporting.
     */
    suspend fun getMovieSummary(
        tmdbMovies: MoviesService,
        movieTmdbId: Int,
        language: String?,
        includeReleaseDates: Boolean,
        action: String
    ): TmdbNonNullResponse<Movie> {
        return awaitTmdbCall(
            tmdbMovies.summary(
                movieTmdbId,
                language,
                if (includeReleaseDates)
                    AppendToResponse(AppendToResponseItem.RELEASE_DATES)
                else
                    null
            ),
            action
        )
    }

    /**
     * Makes the call and returns [TmdbNonNullResponse.Success] with the body if successful or one
     * of [TmdbErrorResponse] otherwise.
     *
     * If there is an error, logs and reports it.
     */
    private suspend fun <T> awaitTmdbCall(
        call: Call<T>,
        action: String
    ): TmdbNonNullResponse<T> {
        val response = try {
            call.awaitResponse()
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            return TmdbErrorResponse.Other()
        }

        if (!response.isSuccessful) {
            // Always log and report an error
            Errors.logAndReport(action, response)
            return when {
                response.code() == 404 -> TmdbErrorResponse.IsNotFound()
                else -> TmdbErrorResponse.Other()
            }
        }

        val body = response.body()

        if (body == null) {
            // Report if there might be a bigger API change
            Errors.logAndReport(action, response, "body is null")
            return TmdbErrorResponse.Other()
        }

        return TmdbNonNullResponse.Success(body)
    }

}