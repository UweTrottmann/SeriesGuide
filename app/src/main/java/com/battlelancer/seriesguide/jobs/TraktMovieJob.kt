package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktTools
import com.battlelancer.seriesguide.util.Errors
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.HistoryType
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import retrofit2.Call

class TraktMovieJob(
    action: JobAction,
    jobInfo: SgJobInfo,
    private val actionAtMs: Long
) : BaseNetworkMovieJob(action, jobInfo) {

    override fun execute(context: Context): NetworkJobResult {
        return buildResult(context, upload(context))
    }

    private fun upload(context: Context): Int {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return ERROR_TRAKT_AUTH
        }

        val movie = SyncMovie().id(MovieIds.tmdb(jobInfo.movieTmdbId()))

        // Send time of action to avoid adding duplicate collection entries at Trakt (does not work
        // for watched entries, separate check for those below) if this job re-runs due to failure,
        // but Trakt already applied changes (it happens).
        // Also if execution is delayed due to being offline this will ensure the actual action time
        // is stored at Trakt.
        val instant = Instant.ofEpochMilli(actionAtMs)
        val actionAtDateTime = instant.atOffset(ZoneOffset.UTC)
        // only send timestamp if adding, not if removing to save data
        // note: timestamp currently not supported for watchlist action
        if (action == JobAction.MOVIE_COLLECTION_ADD) {
            movie.collectedAt(actionAtDateTime)
        } else if (action == JobAction.MOVIE_WATCHED_SET) {
            movie.watchedAt(actionAtDateTime)
            // If sending a watched entry, check if there is already one at actionAtDateTime, then
            // just complete successfully. This is to prevent duplicate watched entries at
            // Trakt if this job re-runs due to failure, but Trakt already applied changes.
            val entryExists = watchedEntryExistsAt(context, jobInfo.movieTmdbId(), actionAtDateTime)
                .getOrElse { return it }
            if (entryExists) {
                return SUCCESS
            }
        }

        val items = SyncItems().movies(movie)

        // determine network call
        val errorLabel: String
        val call: Call<SyncResponse>
        val component = SgApp.getServicesComponent(context)
        val trakt = component.trakt()
        val traktSync = component.traktSync()!!
        when (action) {
            JobAction.MOVIE_COLLECTION_ADD -> {
                errorLabel = "add movie to collection"
                call = traktSync.addItemsToCollection(items)
            }
            JobAction.MOVIE_COLLECTION_REMOVE -> {
                errorLabel = "remove movie from collection"
                call = traktSync.deleteItemsFromCollection(items)
            }
            JobAction.MOVIE_WATCHLIST_ADD -> {
                errorLabel = "add movie to watchlist"
                call = traktSync.addItemsToWatchlist(items)
            }
            JobAction.MOVIE_WATCHLIST_REMOVE -> {
                errorLabel = "remove movie from watchlist"
                call = traktSync.deleteItemsFromWatchlist(items)
            }
            JobAction.MOVIE_WATCHED_SET -> {
                errorLabel = "set movie watched"
                call = traktSync.addItemsToWatchedHistory(items)
            }
            JobAction.MOVIE_WATCHED_REMOVE -> {
                errorLabel = "set movie not watched"
                call = traktSync.deleteItemsFromWatchedHistory(items)
            }
            else -> throw IllegalArgumentException("Action $action not supported.")
        }

        // execute call
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                // check if any items were not found
                if (!isSyncSuccessful(response.body())) {
                    return ERROR_TRAKT_NOT_FOUND
                }
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return ERROR_TRAKT_AUTH
                }
                Errors.logAndReport(
                    errorLabel, response,
                    SgTrakt.checkForTraktError(trakt, response)
                )
                val code = response.code()
                return if (code == 429 /* Rate Limit Exceeded */ || code >= 500) {
                    ERROR_TRAKT_SERVER
                } else {
                    ERROR_TRAKT_CLIENT
                }
            }
        } catch (e: Exception) {
            Errors.logAndReport(errorLabel, e)
            return ERROR_CONNECTION
        }
        return SUCCESS
    }

    private fun watchedEntryExistsAt(
        context: Context,
        movieTmdbId: Int,
        actionAtDateTime: OffsetDateTime
    ): Result<Boolean, Int> {
        val action = "get history for movie"
        val trakt = SgApp.getServicesComponent(context).trakt()

        // Look up Trakt id.
        val movieTraktId = TraktTools.lookupMovieTraktId(trakt, movieTmdbId)
            ?: return Err(ERROR_TRAKT_SERVER)
        if (movieTraktId == -1) return Err(ERROR_TRAKT_NOT_FOUND)

        // Check history if at least one item at actionAtDateTime exists.
        val traktUsers = trakt.users()
        val historyCall = traktUsers.history(
            UserSlug.ME,
            HistoryType.MOVIES,
            movieTraktId,
            1,
            1,
            null,
            actionAtDateTime,
            actionAtDateTime
        )
        return runCatching {
            historyCall.execute()
        }.mapError {
            Errors.logAndReport(action, it)
            ERROR_CONNECTION
        }.andThen { response ->
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return@andThen Ok(body.isNotEmpty())
                } else {
                    Errors.logAndReport(action, response, "body is null")
                    return@andThen Err(ERROR_TRAKT_CLIENT)
                }
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return@andThen Err(ERROR_TRAKT_AUTH)
                }
                Errors.logAndReport(
                    action, response,
                    SgTrakt.checkForTraktError(trakt, response)
                )
                val code = response.code()
                return@andThen if (code == 429 /* Rate Limit Exceeded */ || code >= 500) {
                    Err(ERROR_TRAKT_SERVER)
                } else {
                    Err(ERROR_TRAKT_CLIENT)
                }
            }
        }
    }

    companion object {
        /**
         * If [SyncResponse.not_found] indicates any show,
         * season or episode was not found returns `false`.
         */
        private fun isSyncSuccessful(response: SyncResponse?): Boolean {
            val notFound = response?.not_found ?: return true
            if (notFound.movies?.isNotEmpty() == true) {
                return false // movie not found
            }
            return true
        }
    }
}