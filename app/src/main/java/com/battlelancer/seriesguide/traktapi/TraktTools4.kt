// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.AddNoteRequest
import com.uwetrottmann.trakt5.entities.Note
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.services.Notes
import retrofit2.Call
import retrofit2.awaitResponse

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
            val data: T?
        ) : TraktResponse<T>
    }

    sealed interface TraktNonNullResponse<T> {
        data class Success<T>(val data: T) : TraktNonNullResponse<T>
    }

    sealed interface TraktErrorResponse {
        class IsNotVip<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class IsUnauthorized<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class IsAccountLimitExceeded<T> : TraktResponse<T>, TraktNonNullResponse<T>
        class Other<T> : TraktResponse<T>, TraktNonNullResponse<T>
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
                SgTrakt.isAccountLimitExceeded(response) -> {
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

        return TraktResponse.Success(body)
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
                    TraktNonNullResponse.Success(data)
                }
            }
        }
    }

}