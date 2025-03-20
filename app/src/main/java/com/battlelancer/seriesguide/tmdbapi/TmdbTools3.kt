// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.tmdbapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbError
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.isRetryError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvShow
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.enumerations.ExternalSource

/**
 * Uses third-party [Result] API for result handling. Errors are [TmdbError].
 *
 * New code should use [TmdbTools4] design patterns. The [Result] API might block future Kotlin
 * version updates or enforce new patterns.
 */
object TmdbTools3 {

    sealed class TmdbError

    /**
     * The API request might succeed if tried again after a brief delay
     * (e.g. time outs or other temporary network issues).
     */
    object TmdbRetry : TmdbError()

    /**
     * The API request is unlikely to succeed if retried, at least right now
     * (e.g. API bugs or changes).
     */
    object TmdbStop : TmdbError()

    /**
     * Tries to find the TMDB id for the given show's TheTVDB id. Returns null value if not found.
     */
    fun findShowTmdbId(context: Context, showTvdbId: Int): Result<Int?, TmdbError> {
        val action = "find tvdb show"
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        return runCatching {
            tmdb.findService()
                .find(showTvdbId, ExternalSource.TVDB_ID, null)
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val tvResults = it.body()?.tv_results
                if (tvResults != null) {
                    if (tvResults.isNotEmpty()) {
                        val showId = tvResults[0].id
                        if (showId != null && showId > 0) {
                            return@andThen Ok(showId) // found it!
                        } else {
                            Errors.logAndReport(action, it, "show id is invalid")
                        }
                    } else {
                        return@andThen Ok(null) // not found
                    }
                } else {
                    Errors.logAndReport(action, it, "tv_results is null")
                }
            } else {
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

    /**
     * Returns null value if the show no longer exists (TMDB returned HTTP 404).
     */
    fun getShowAndExternalIds(
        showTmdbId: Int,
        language: String,
        context: Context
    ): Result<TvShow?, TmdbError> {
        val action = "show n ids"
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        return runCatching {
            tmdb.tvService()
                .tv(showTmdbId, language, AppendToResponse(AppendToResponseItem.EXTERNAL_IDS))
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val tvShow = it.body()
                if (tvShow != null) {
                    return@andThen Ok(tvShow)
                } else {
                    Errors.logAndReport(action, it, "show is null")
                }
            } else {
                // Explicitly indicate if result is null because show no longer exists.
                if (it.code() == 404) return@andThen Ok(null)
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

    fun getShowTrailerYoutubeId(
        context: Context,
        showTmdbId: Int,
        languageCode: String
    ): Result<String?, TmdbError> {
        val action = "get show trailer"
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        return runCatching {
            tmdb.tvService()
                .videos(showTmdbId, languageCode)
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val results = it.body()?.results
                if (results != null) {
                    return@andThen Ok(TmdbTools2.extractTrailer(it.body()))
                } else {
                    Errors.logAndReport(action, it, "results is null")
                }
            } else {
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

    fun getSeason(
        showTmdbId: Int,
        seasonNumber: Int,
        language: String,
        context: Context
    ): Result<List<TvEpisode>, TmdbError> {
        val action = "get season"
        val tmdb = SgApp.getServicesComponent(context).tmdb()
        return runCatching {
            tmdb.tvSeasonsService()
                .season(showTmdbId, seasonNumber, language)
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TmdbRetry else TmdbStop
        }.andThen {
            if (it.isSuccessful) {
                val tvSeason = it.body()?.episodes
                if (tvSeason != null) {
                    return@andThen Ok(tvSeason)
                } else {
                    Errors.logAndReport(action, it, "episodes is null")
                }
            } else {
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TmdbStop)
        }
    }

}