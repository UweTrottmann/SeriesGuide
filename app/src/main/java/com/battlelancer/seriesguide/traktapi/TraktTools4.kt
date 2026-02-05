// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.traktapi

import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.AddNoteRequest
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.Note
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.services.Notes
import com.uwetrottmann.trakt5.services.Sync
import retrofit2.Call
import retrofit2.awaitResponse
import kotlin.collections.set

/**
 * Uses response classes inheriting from a Kotlin sealed interface.
 *
 * Removes any Android specific classes and no longer relies on a third-party library to handle
 * results.
 */
object TraktTools4 {

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
     * Get collected shows mapped by their TMDB ID.
     */
    suspend fun getCollectedShowsByTmdbId(
        traktSync: Sync
    ): TraktNonNullResponse<Map<Int, BaseShow>> {
        val response = fetchAllPages(
            action = "get collected shows",
            reportIsNotVip = true // Should work even if not VIP
        ) { page ->
            traktSync.collectionShows(page, 1000, null)
        }

        return when (response) {
            is TraktNonNullResponse.Success -> TraktNonNullResponse.Success(
                mapByTmdbId(response.data),
                response.pageCount
            )
            is TraktErrorResponse.IsNotVip -> TraktErrorResponse.IsNotVip()
            is TraktErrorResponse.IsUnauthorized -> TraktErrorResponse.IsUnauthorized()
            is TraktErrorResponse.IsAccountLimitExceeded -> TraktErrorResponse.IsAccountLimitExceeded()
            is TraktErrorResponse.Other -> TraktErrorResponse.Other()
        }
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

    fun mapByTmdbId(traktShows: List<BaseShow>?): Map<Int, BaseShow> {
        if (traktShows == null) return emptyMap()

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

}