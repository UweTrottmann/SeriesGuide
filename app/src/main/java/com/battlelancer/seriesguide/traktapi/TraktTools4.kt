// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.traktapi

import com.battlelancer.seriesguide.traktapi.TraktTools4.awaitTraktCall
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.AddNoteRequest
import com.uwetrottmann.trakt5.entities.BaseMovie
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.Note
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.ExtendedShowsWatched
import com.uwetrottmann.trakt5.services.Notes
import com.uwetrottmann.trakt5.services.Sync
import retrofit2.Call
import retrofit2.awaitResponse
import timber.log.Timber

/**
 * Uses response classes inheriting from a Kotlin sealed interface.
 *
 * Removes any Android specific classes and no longer relies on a third-party library to handle
 * results.
 */
object TraktTools4 {

    // 250 is the maximum limit according to the Trakt [Upcoming API Changes: Pagination & Sorting Updates](https://github.com/trakt/trakt-api/discussions/681)
    // discussion.
    private const val MAX_LIMIT = 250

    sealed interface TraktResponse<T> {
        data class Success<T>(
            /**
             * If T is [Void] this is always `null`.
             */
            val data: T?,
            /**
             * If returned, the number of available pages for a paginated endpoint.
             */
            val pageCount: Int?
        ) : TraktResponse<T>
    }

    sealed interface TraktNonNullResponse<T> {
        data class Success<T>(
            val data: T,
            /**
             * If returned, the number of available pages for a paginated endpoint.
             */
            val pageCount: Int?
        ) : TraktNonNullResponse<T>
    }

    sealed interface TraktErrorResponse {
        class IsNotVip<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class IsUnauthorized<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class IsAccountLimitExceeded<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class Other<T> : TraktResponse<T>, TraktNonNullResponse<T>
    }

    /**
     * If [noSeasons] is `true`, only show info is available. Starting 2026-05-30 also full info.
     * If it's `false`, seasons and episodes are available. Starting 2026-05-30 also full info.
     *
     * See the Trakt [Upcoming API Changes: Watched Endpoints Pagination & Extended Defaults](https://github.com/trakt/trakt-api/discussions/775)
     * discussion about details and updates.
     */
    suspend fun getWatchedShows(
        traktSync: Sync,
        noSeasons: Boolean
    ): TraktNonNullResponse<List<BaseShow>> {
        return fetchAllPages(
            action = "get watched shows",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            traktSync.watchedShows(
                page,
                MAX_LIMIT,
                if (noSeasons) {
                    // As of 2026-05-30 this should be the default, still request until then
                    // https://github.com/trakt/trakt-api/discussions/775
                    @Suppress("DEPRECATION")
                    ExtendedShowsWatched.NOSEASONS
                } else {
                    // This should only work starting 2026-05-30, but already request it
                    // https://github.com/trakt/trakt-api/discussions/775
                    ExtendedShowsWatched.PROGRESS
                }
            )
        }
    }

    suspend fun getWatchedShowsByTmdbId(
        traktSync: Sync
    ): TraktNonNullResponse<Map<Int, BaseShow>> {
        val response = getWatchedShows(traktSync, noSeasons = false)
        return mapResponseData(response) { mapByTmdbId(it) }
    }

    suspend fun getCollectedShows(
        traktSync: Sync
    ): TraktNonNullResponse<List<BaseShow>> {
        return fetchAllPages(
            action = "get collected shows",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            traktSync.collectionShows(page, MAX_LIMIT, null)
        }
    }

    suspend fun getCollectedShowsByTmdbId(
        traktSync: Sync
    ): TraktNonNullResponse<Map<Int, BaseShow>> {
        val response = getCollectedShows(traktSync)
        return mapResponseData(response) { mapByTmdbId(it) }
    }

    /**
     * Fetches all pages from a paginated Trakt API endpoint.
     *
     * @param action Description of the action for error logging
     * @param reportIsNotVip Whether to report "not VIP" errors
     * @param callProvider Function that creates a Call for a given page number
     * @return All items from all pages combined, or an error response
     */
    private suspend fun <T> fetchAllPages(
        action: String,
        reportIsNotVip: Boolean = false,
        callProvider: (page: Int) -> Call<List<T>>
    ): TraktNonNullResponse<List<T>> {
        val allItems = mutableListOf<T>()
        var currentPage = 1
        var totalPageCount: Int?

        do {
            val response = awaitTraktCallNonNull(
                callProvider(currentPage),
                action,
                reportIsNotVip = reportIsNotVip
            )

            when (response) {
                is TraktNonNullResponse.Success -> {
                    allItems.addAll(response.data)
                    totalPageCount = response.pageCount
                    if (totalPageCount == null) {
                        Timber.w("Page count header not found for '$action'")
                    }
                    currentPage++
                }

                is TraktErrorResponse.IsNotVip -> return TraktErrorResponse.IsNotVip()
                is TraktErrorResponse.IsUnauthorized -> return TraktErrorResponse.IsUnauthorized()
                is TraktErrorResponse.IsAccountLimitExceeded -> return TraktErrorResponse.IsAccountLimitExceeded()
                is TraktErrorResponse.Other -> return TraktErrorResponse.Other()
            }
        } while (totalPageCount != null && currentPage <= totalPageCount)

        return TraktNonNullResponse.Success(allItems, totalPageCount)
    }

    private fun mapByTmdbId(traktShows: List<BaseShow>): Map<Int, BaseShow> {
        val traktShowsMap = HashMap<Int, BaseShow>(traktShows.size)
        for (traktShow in traktShows) {
            val tmdbId = traktShow.show?.ids?.tmdb
            if (tmdbId == null || traktShow.seasons.isNullOrEmpty()) {
                continue  // trakt show misses required data, skip.
            }
            traktShowsMap[tmdbId] = traktShow
        }
        return traktShowsMap
    }

    suspend fun getShowsOnWatchlist(
        traktSync: Sync
    ): TraktNonNullResponse<List<BaseShow>> {
        return fetchAllPages(
            action = "get shows on watchlist",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            // Use Extended.FULL to get show metadata
            traktSync.watchlistShows(page, MAX_LIMIT, Extended.FULL)
        }
    }

    suspend fun getWatchedMoviesByTmdbId(
        traktSync: Sync
    ): TraktNonNullResponse<MutableMap<Int, Int>> {
        val response = fetchAllPages(
            action = "get watched movies",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            traktSync.watchedMovies(page, MAX_LIMIT, null)
        }
        return mapResponseData(response) { mapMoviesToTmdbIdWithPlays(it) }
    }

    private fun mapMoviesToTmdbIdWithPlays(traktMovies: List<BaseMovie>): MutableMap<Int, Int> {
        val map: MutableMap<Int, Int> = HashMap(traktMovies.size)
        for (movie in traktMovies) {
            val tmdbId = movie.movie?.ids?.tmdb
                ?: continue // skip invalid values
            map[tmdbId] = movie.plays
        }
        return map
    }

    suspend fun getCollectedMoviesByTmdbId(
        traktSync: Sync
    ): TraktNonNullResponse<MutableSet<Int>> {
        val response = fetchAllPages(
            action = "get collected movies",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            traktSync.collectionMovies(page, MAX_LIMIT, null)
        }
        return mapResponseData(response) { mapMoviesToTmdbIdSet(it) }
    }

    suspend fun getMoviesOnWatchlistByTmdbId(
        traktSync: Sync
    ): TraktNonNullResponse<MutableSet<Int>> {
        val response = fetchAllPages(
            action = "get movie watchlist",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            traktSync.watchlistMovies(page, MAX_LIMIT, null)
        }
        return mapResponseData(response) { mapMoviesToTmdbIdSet(it) }
    }

    private fun mapMoviesToTmdbIdSet(traktMovies: List<BaseMovie>): MutableSet<Int> {
        val tmdbIdSet: MutableSet<Int> = HashSet(traktMovies.size)
        for (movie in traktMovies) {
            val tmdbId = movie.movie?.ids?.tmdb
                ?: continue // skip invalid values
            tmdbIdSet.add(tmdbId)
        }
        return tmdbIdSet
    }

    /**
     * Adds or updates the note for the given show.
     *
     * See [awaitTraktCall] for details.
     */
    suspend fun saveNoteForShow(
        traktNotes: Notes,
        showTmdbId: Int,
        noteText: String
    ): TraktNonNullResponse<Note> {
        // Note: calling the add endpoint for an existing note will update it
        return awaitTraktCallNonNull(
            traktNotes.addNote(
                AddNoteRequest(
                    Show().apply {
                        ids = ShowIds.tmdb(showTmdbId)
                    },
                    noteText
                )
            ),
            "update note",
            reportIsNotVip = true // Should work even if not VIP
        )
    }

    /**
     * See [awaitTraktCall] for details.
     */
    suspend fun deleteNote(
        traktNotes: Notes,
        noteId: Long
    ): TraktResponse<Void> {
        return awaitTraktCall(
            traktNotes.deleteNote(noteId),
            "delete note",
            reportIsNotVip = true // Should work even if not VIP
        )
    }

    /**
     * Makes the call and returns [TraktResponse.Success] with the body if successful or one of
     * [TraktErrorResponse] otherwise.
     *
     * If there is an error, logs and reports it. Except for [TraktErrorResponse.IsNotVip] and
     * [TraktErrorResponse.IsUnauthorized].
     *
     * Use [reportIsNotVip] to report this error if it is unexpected.
     */
    private suspend fun <T> awaitTraktCall(
        call: Call<T>,
        action: String,
        reportIsNotVip: Boolean = false,
        logErrorOnNullBody: Boolean = false
    ): TraktResponse<T> {
        val response = try {
            call.awaitResponse()
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            return TraktErrorResponse.Other()
        }

        if (!response.isSuccessful) {
            return when {
                TraktV2.isAccountLimitExceeded(response) -> {
                    Errors.logAndReport(action, response)
                    TraktErrorResponse.IsAccountLimitExceeded()
                }

                TraktV2.isNotVip(response) -> {
                    if (reportIsNotVip) Errors.logAndReport(action, response)
                    TraktErrorResponse.IsNotVip()
                }

                TraktV2.isUnauthorized(response) -> TraktErrorResponse.IsUnauthorized()
                else -> {
                    Errors.logAndReport(action, response)
                    TraktErrorResponse.Other()
                }
            }
        }

        val body = response.body()

        // Report if there might be a bigger API change
        if (logErrorOnNullBody && body == null) {
            Errors.logAndReport(action, response, "body is null")
        }

        // Only returned when using pagination
        val pageCountOrNull = TraktV2.getPageCount(response)

        return TraktResponse.Success(body, pageCountOrNull)
    }

    /**
     * Like [awaitTraktCall], but ensures the response data is not null.
     */
    private suspend fun <T> awaitTraktCallNonNull(
        call: Call<T>,
        action: String,
        reportIsNotVip: Boolean = false
    ): TraktNonNullResponse<T> {
        return when (val response =
            awaitTraktCall(call, action, reportIsNotVip, logErrorOnNullBody = true)) {
            is TraktErrorResponse.Other -> response
            is TraktErrorResponse.IsAccountLimitExceeded -> response
            is TraktErrorResponse.IsNotVip -> response
            is TraktErrorResponse.IsUnauthorized -> response
            is TraktResponse.Success -> {
                val data = response.data
                if (data == null) {
                    TraktErrorResponse.Other()
                } else {
                    TraktNonNullResponse.Success(data, response.pageCount)
                }
            }
        }
    }

    private fun <T, R> mapResponseData(
        response: TraktNonNullResponse<T>,
        transform: (T) -> R
    ): TraktNonNullResponse<R> {
        return when (response) {
            is TraktNonNullResponse.Success -> TraktNonNullResponse.Success(
                transform(response.data),
                response.pageCount
            )

            is TraktErrorResponse.IsNotVip -> TraktErrorResponse.IsNotVip()
            is TraktErrorResponse.IsUnauthorized -> TraktErrorResponse.IsUnauthorized()
            is TraktErrorResponse.IsAccountLimitExceeded -> TraktErrorResponse.IsAccountLimitExceeded()
            is TraktErrorResponse.Other -> TraktErrorResponse.Other()
        }
    }

}