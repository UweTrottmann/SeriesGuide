package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.sync.NetworkJobProcessor.JobResult
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncEpisode
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.SyncSeason
import com.uwetrottmann.trakt5.entities.SyncShow
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import retrofit2.Call
import java.util.ArrayList
import java.util.LinkedList

class TraktEpisodeJob(
    action: JobAction,
    jobInfo: SgJobInfo,
    private val actionAtMs: Long
) : BaseNetworkEpisodeJob(action, jobInfo) {

    override fun execute(context: Context): JobResult {
        // Do not send if show has no trakt id (was not on trakt last time we checked).
        val showTraktId = ShowTools.getShowTraktId(context, jobInfo.showId())
        val canSendToTrakt = showTraktId != null
        if (!canSendToTrakt) {
            return buildResult(context, NetworkJob.ERROR_TRAKT_NOT_FOUND)
        }
        val result = upload(context, showTraktId!!)
        return buildResult(context, result)
    }

    private fun upload(context: Context, showTraktId: Int): Int {
        val flagValue = jobInfo.flagValue()

        // skipped flag not supported by trakt
        if (EpisodeTools.isSkipped(flagValue)) {
            return NetworkJob.SUCCESS
        }

        val isAddNotDelete = (flagValue
                != EpisodeFlags.UNWATCHED) // 0 for not watched or not collected
        val seasons = getEpisodesForTrakt(isAddNotDelete)
        if (seasons.isEmpty()) {
            return NetworkJob.SUCCESS // nothing to upload, done.
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return NetworkJob.ERROR_TRAKT_AUTH
        }

        // outer wrapper and show are always required
        val show = SyncShow().id(ShowIds.trakt(showTraktId))
        val items = SyncItems().shows(show)
        show.seasons(seasons)

        // determine network call
        val errorLabel: String
        val call: Call<SyncResponse>
        val component = SgApp.getServicesComponent(context)
        val trakt = component.trakt()
        val traktSync = component.traktSync()!!
        when (action) {
            JobAction.EPISODE_WATCHED_FLAG -> if (isAddNotDelete) {
                errorLabel = "set episodes watched"
                call = traktSync.addItemsToWatchedHistory(items)
            } else {
                errorLabel = "set episodes not watched"
                call = traktSync.deleteItemsFromWatchedHistory(items)
            }
            JobAction.EPISODE_COLLECTION -> if (isAddNotDelete) {
                errorLabel = "add episodes to collection"
                call = traktSync.addItemsToCollection(items)
            } else {
                errorLabel = "remove episodes from collection"
                call = traktSync.deleteItemsFromCollection(items)
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

    /**
     * Builds a list of [SyncSeason] objects to submit to Trakt.
     */
    private fun getEpisodesForTrakt(isAddNotDelete: Boolean): List<SyncSeason> {
        // send time of action to avoid adding duplicate plays/collection events at trakt
        // if this job re-runs due to failure, but trakt already applied changes (it happens)
        // also if execution is delayed to due being offline this will ensure
        // the actual action time is stored at trakt
        val instant = Instant.ofEpochMilli(actionAtMs)
        val actionAtDateTime = instant.atOffset(ZoneOffset.UTC)

        val seasons: MutableList<SyncSeason> = ArrayList()

        var currentSeason: SyncSeason? = null
        for (i in 0 until jobInfo.episodesLength()) {
            val episodeInfo = jobInfo.episodes(i)

            val seasonNumber = episodeInfo.season()

            // start new season?
            if (currentSeason == null || currentSeason.number?.let { seasonNumber > it } == true) {
                currentSeason = SyncSeason().number(seasonNumber)
                currentSeason.episodes = LinkedList()
                seasons.add(currentSeason)
            }

            // add episode
            val episode = SyncEpisode().number(episodeInfo.number())
            if (isAddNotDelete) {
                // only send timestamp if adding, not if removing to save data
                if (action == JobAction.EPISODE_WATCHED_FLAG) {
                    episode.watchedAt(actionAtDateTime)
                } else {
                    episode.collectedAt(actionAtDateTime)
                }
            }
            currentSeason.episodes!!.add(episode)
        }
        return seasons
    }

    companion object {
        /**
         * If the [SyncResponse.not_found] indicates any show,
         * season or episode was not found returns `false`.
         */
        private fun isSyncSuccessful(response: SyncResponse?): Boolean {
            val notFound = response?.not_found ?: return true
            if (notFound.shows?.isNotEmpty() == true) {
                // show not found
                return false
            }
            if (notFound.seasons?.isNotEmpty() == true) {
                // show exists, but seasons not found
                return false
            }
            if (notFound.episodes?.isNotEmpty() == true) {
                // show and season exists, but episodes not found
                return false
            }
            return true
        }
    }
}