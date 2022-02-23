package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.sync.NetworkJobProcessor.JobResult
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncResponse
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import retrofit2.Call

class TraktMovieJob(
    action: JobAction,
    jobInfo: SgJobInfo,
    private val actionAtMs: Long
) : BaseNetworkMovieJob(action, jobInfo) {

    override fun execute(context: Context): JobResult {
        return buildResult(context, upload(context))
    }

    private fun upload(context: Context): Int {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return NetworkJob.ERROR_TRAKT_AUTH
        }

        val movie = SyncMovie().id(MovieIds.tmdb(jobInfo.movieTmdbId()))

        // send time of action to avoid adding duplicate plays/collection events at trakt
        // if this job re-runs due to failure, but trakt already applied changes (it happens)
        // also if execution is delayed to due being offline this will ensure
        // the actual action time is stored at trakt
        val instant = Instant.ofEpochMilli(actionAtMs)
        val actionAtDateTime = instant.atOffset(ZoneOffset.UTC)
        // only send timestamp if adding, not if removing to save data
        // note: timestamp currently not supported for watchlist action
        if (action == JobAction.MOVIE_COLLECTION_ADD) {
            movie.collectedAt(actionAtDateTime)
        } else if (action == JobAction.MOVIE_WATCHED_SET) {
            movie.watchedAt(actionAtDateTime)
        }

        val items = SyncItems().movies(movie)

        // determine network call
        val errorLabel: String
        val call: Call<SyncResponse>
        val component = getServicesComponent(context)
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
                    return NetworkJob.ERROR_TRAKT_NOT_FOUND
                }
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return NetworkJob.ERROR_TRAKT_AUTH
                }
                Errors.logAndReport(
                    errorLabel, response,
                    SgTrakt.checkForTraktError(trakt, response)
                )
                val code = response.code()
                return if (code == 429 /* Rate Limit Exceeded */ || code >= 500) {
                    NetworkJob.ERROR_TRAKT_SERVER
                } else {
                    NetworkJob.ERROR_TRAKT_CLIENT
                }
            }
        } catch (e: Exception) {
            Errors.logAndReport(errorLabel, e)
            return NetworkJob.ERROR_CONNECTION
        }
        return NetworkJob.SUCCESS
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