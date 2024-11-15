// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

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
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.AddNoteRequest
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.LastActivity
import com.uwetrottmann.trakt5.entities.LastActivityMore
import com.uwetrottmann.trakt5.entities.LastActivityUpdated
import com.uwetrottmann.trakt5.entities.Note
import com.uwetrottmann.trakt5.entities.Ratings
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import com.uwetrottmann.trakt5.services.Notes
import retrofit2.Call
import retrofit2.Response
import retrofit2.awaitResponse

object TraktTools2 {

    sealed class TraktResponse<T> {
        data class Success<T>(val data: T) : TraktResponse<T>()
        class IsNotVip<T> : TraktResponse<T>()
        class IsUnauthorized<T> : TraktResponse<T>()
        class Error<T> : TraktResponse<T>()
    }

    /**
     * Adds or updates the note for the given show.
     */
    suspend fun saveNoteForShow(
        traktNotes: Notes,
        showTmdbId: Int,
        noteText: String
    ): TraktResponse<Note> {
        // Note: calling the add endpoint for an existing note will update it
        return awaitTraktCall(
            traktNotes.addNote(
                AddNoteRequest(
                    Show().apply {
                        ids = ShowIds.tmdb(showTmdbId)
                    },
                    noteText
                )
            ), "update note"
        )
    }

    suspend fun deleteNote(
        trakt: SgTrakt,
        noteId: Long
    ): TraktResponse<Void> {
        return awaitTraktCall(trakt.notes().deleteNote(noteId), "delete note")
    }

    private suspend fun <T> awaitTraktCall(call: Call<T>, action: String): TraktResponse<T> {
        try {
            val response = call.awaitResponse()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return TraktResponse.Success(body)
                } else {
                    Errors.logAndReport(action, response, "body is null")
                }
            } else {
                if (TraktV2.isNotVip(response)) return TraktResponse.IsNotVip()
                if (TraktV2.isUnauthorized(response)) return TraktResponse.IsUnauthorized()
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
        }
        return TraktResponse.Error()
    }

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
                    1,
                    1
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

    enum class ServiceResult {
        SUCCESS,
        AUTH_ERROR,
        API_ERROR
    }

    @JvmStatic
    fun getCollectedOrWatchedShows(
        isCollectionNotWatched: Boolean,
        context: Context
    ): Pair<Map<Int, BaseShow>?, ServiceResult> {
        val traktSync = SgApp.getServicesComponent(context).traktSync()!!
        val action = if (isCollectionNotWatched) "get collection" else "get watched"
        try {
            val response: Response<List<BaseShow>> = if (isCollectionNotWatched) {
                traktSync.collectionShows(null).execute()
            } else {
                traktSync.watchedShows(null).execute()
            }
            if (response.isSuccessful) {
                return Pair(mapByTmdbId(response.body()), ServiceResult.SUCCESS)
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return Pair(null, ServiceResult.AUTH_ERROR)
                }
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
        }
        return Pair(null, ServiceResult.API_ERROR)
    }

    @JvmStatic
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

    fun getEpisodeRatings(
        context: Context,
        showTraktId: String,
        seasonNumber: Int,
        episodeNumber: Int
    ): Pair<Double, Int>? {
        val ratings: Ratings =
            SgTrakt.executeCall(
                SgApp.getServicesComponent(context).trakt()
                    .episodes().ratings(showTraktId, seasonNumber, episodeNumber),
                "get episode rating"
            ) ?: return null
        val rating = ratings.rating
        val votes = ratings.votes
        return if (rating != null && votes != null) {
            Pair(rating, votes)
        } else {
            null
        }
    }

    data class LastActivities(
        val episodes: LastActivityMore,
        val shows: LastActivity,
        val movies: LastActivityMore,
        val notes: LastActivityUpdated,
    )

    fun getLastActivity(context: Context): Result<LastActivities, TraktError> {
        val action = "get last activity"
        return runCatching {
            SgApp.getServicesComponent(context).trakt().sync()
                .lastActivities()
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
                if (episodes != null && shows != null && movies != null && notes != null) {
                    return@andThen Ok(
                        LastActivities(
                            episodes = episodes,
                            shows = shows,
                            movies = movies,
                            notes = notes
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
