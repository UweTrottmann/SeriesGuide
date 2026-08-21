// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.traktapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.isRetryError
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import org.threeten.bp.OffsetDateTime

/**
 * Uses third-party [Result] API for result handling. Errors are [TraktError].
 *
 * New code should use [TraktTools4] design patterns. The [Result] API might block future Kotlin
 * version updates or enforce new patterns.
 */
object TraktTools3 {

    sealed class TraktError

    /**
     * The API request might succeed if tried again after a brief delay
     * (e.g. time outs or other temporary network issues).
     */
    object TraktRetry : TraktError()

    /**
     * The API request is unlikely to succeed if retried, at least right now
     * (e.g. API bugs or changes).
     */
    object TraktStop : TraktError()

    /**
     * Look up a show by its TMDB ID, may return `null` if not found.
     */
    fun getShowByTmdbId(showTmdbId: Int, context: Context): Result<Show?, TraktError> {
        val action = "show trakt lookup"
        val trakt = SgApp.getServicesComponent(context).trakt()
        return runCatching {
            trakt.search()
                .idLookup(
                    IdType.TMDB,
                    showTmdbId.toString(),
                    Type.SHOW,
                    Extended.FULL,
                    /* page = */ 1,
                    /* limit = */ 1
                ).execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TraktRetry else TraktStop
        }.andThen {
            if (it.isSuccessful) {
                val result = it.body()?.firstOrNull()
                if (result != null) {
                    if (result.show != null) {
                        return@andThen Ok(result.show)
                    } else {
                        // If there is a result, it should contain a show.
                        Errors.logAndReport(action, it, "show of result is null")
                    }
                }
                return@andThen Ok(null) // Not found.
            } else {
                Errors.logAndReport(action, it)
            }
            return@andThen Err(TraktStop)
        }
    }

    data class LastActivities(
        val episodesLastWatchedAt: OffsetDateTime?,
        val episodesLastCollectedAt: OffsetDateTime?,
        val episodesLastRatedAt: OffsetDateTime?,
        val showsLastRatedAt: OffsetDateTime?,
        val moviesLastCollectedAt: OffsetDateTime?,
        val moviesLastWatchlistedAt: OffsetDateTime?,
        val moviesLastWatchedAt: OffsetDateTime?,
        val moviesLastRatedAt: OffsetDateTime?,
        val notesLastUpdatedAt: OffsetDateTime?,
        val listsLastUpdatedAt: OffsetDateTime?
    )

    fun getLastActivity(context: Context): Result<LastActivities, TraktError> {
        val action = "get last activity"
        return runCatching {
            SgApp.getServicesComponent(context).trakt().sync()
                .lastActivities2()
                .execute()
        }.mapError {
            Errors.logAndReport(action, it)
            if (it.isRetryError()) TraktRetry else TraktStop
        }.andThen { response ->
            if (response.isSuccessful) {
                val lastActivities = response.body()
                val episodes = lastActivities?.episodes
                val shows = lastActivities?.shows
                val movies = lastActivities?.movies
                val notes = lastActivities?.notes
                val lists = lastActivities?.lists
                if (
                    episodes != null
                    && shows != null
                    && movies != null
                    && notes != null
                    && lists != null
                ) {
                    return@andThen Ok(
                        LastActivities(
                            episodesLastWatchedAt = episodes.watched_at,
                            episodesLastCollectedAt = episodes.collected_at,
                            episodesLastRatedAt = episodes.rated_at,
                            showsLastRatedAt = shows.rated_at,
                            moviesLastCollectedAt = movies.collected_at,
                            moviesLastWatchlistedAt = movies.watchlisted_at,
                            moviesLastWatchedAt = movies.watched_at,
                            moviesLastRatedAt = movies.rated_at,
                            notesLastUpdatedAt = notes.updated_at,
                            listsLastUpdatedAt = lists.updated_at
                        )
                    )
                } else {
                    Errors.logAndReport(action, response, "last activity is null")
                }
            } else {
                if (!SgTrakt.isUnauthorized(context, response)) {
                    Errors.logAndReport(action, response)
                }
            }
            return@andThen Err(TraktStop)
        }
    }

}