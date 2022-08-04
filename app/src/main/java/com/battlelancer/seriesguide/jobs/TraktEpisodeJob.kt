package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.Errors
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncEpisode
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.SyncSeason
import com.uwetrottmann.trakt5.entities.SyncShow
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.services.Users
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset
import retrofit2.Call
import java.util.LinkedList

class TraktEpisodeJob(
    action: JobAction,
    jobInfo: SgJobInfo,
    private val actionAtMs: Long
) : BaseNetworkEpisodeJob(action, jobInfo) {

    override fun execute(context: Context): NetworkJobResult {
        // Do not send if show has no trakt id (was not on trakt last time we checked).
        val showTraktId = SgApp.getServicesComponent(context)
            .showTools().getShowTraktId(jobInfo.showId())
        val canSendToTrakt = showTraktId != null
        if (!canSendToTrakt) {
            return buildResult(context, ERROR_TRAKT_NOT_FOUND)
        }
        val result = upload(context, showTraktId!!)
        return buildResult(context, result)
    }

    private fun upload(context: Context, showTraktId: Int): Int {
        val flagValue = jobInfo.flagValue()

        // skipped flag not supported by trakt
        if (EpisodeTools.isSkipped(flagValue)) {
            return SUCCESS
        }

        val isAddNotDelete = (flagValue
                != EpisodeFlags.UNWATCHED) // 0 for not watched or not collected
        val seasons = getEpisodesForTrakt(context, showTraktId, isAddNotDelete)
            .getOrElse { return it }
        if (seasons.isEmpty()) {
            return SUCCESS // nothing to upload, done.
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return ERROR_TRAKT_AUTH
        }

        // outer wrapper and show are always required
        val show = SyncShow().id(ShowIds.trakt(showTraktId))
        val items = SyncItems().shows(show)
        show.seasons(seasons)

        // determine network call
        val errorLabel: String
        val call: Call<SyncResponse>

        val trakt = SgApp.getServicesComponent(context).trakt()
        val traktSync = trakt.sync()
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

    /**
     * Builds a list of [SyncSeason] objects to submit to Trakt. When adding watched history
     * entries, checks against Trakt history if an entry exists at that time for an episode
     * and excludes those episodes. This is to prevent duplicate entries due to sending failing
     * due to a network error, but changes getting still applied at Trakt.
     */
    private fun getEpisodesForTrakt(
        context: Context,
        showTraktId: Int,
        isAddNotDelete: Boolean
    ): Result<List<SyncSeason>, Int> {
        val isAddingWatchedEntry = action == JobAction.EPISODE_WATCHED_FLAG && isAddNotDelete

        // Send time of action to avoid adding duplicate collection entries at Trakt (does not work
        // for watched entries, separate check for those below) if this job re-runs due to failure,
        // but Trakt already applied changes (it happens).
        // Also if execution is delayed due to being offline this will ensure the actual action time
        // is stored at Trakt.
        val instant = Instant.ofEpochMilli(actionAtMs)
        val actionAtDateTime = instant.atOffset(ZoneOffset.UTC)

        val seasons: MutableList<SyncSeason> = ArrayList()
        val trakt = SgApp.getServicesComponent(context).trakt()
        val traktUsers = trakt.users()

        // If sending a watched entry, check if there are already episodes watched at
        // actionAtDateTime, then exclude those. This is to prevent duplicate watched entries at
        // Trakt if this job re-runs due to failure, but Trakt already applied changes.
        val watchedEpisodes = mutableListOf<WatchedEpisode>()
        if (isAddingWatchedEntry) {
            var page = 1
            do {
                val historyPage =
                    getHistoryEntryPage(
                        context,
                        trakt,
                        traktUsers,
                        showTraktId,
                        actionAtDateTime,
                        page
                    ).getOrElse { return Err(it) }
                watchedEpisodes.addAll(historyPage.episodes)
                page++
            } while (page <= historyPage.pageCount)
        }

        var currentSeason: SyncSeason? = null
        for (i in 0 until jobInfo.episodesLength()) {
            val episodeInfo = jobInfo.episodes(i)
            val number = episodeInfo.number()
            val seasonNumber = episodeInfo.season()

            if (isAddingWatchedEntry
                && watchedEpisodes.find { it.number == number && it.season == seasonNumber } != null) {
                // Skip, this episode already has an entry at actionAtDateTime.
                continue
            }

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
        return Ok(seasons)
    }

    data class WatchedEpisode(
        val number: Int,
        val season: Int
    )

    data class HistoryPage(
        val episodes: List<WatchedEpisode>,
        val pageCount: Int
    )

    private fun getHistoryEntryPage(
        context: Context,
        trakt: TraktV2,
        traktUsers: Users,
        showTraktId: Int,
        actionAtDateTime: OffsetDateTime,
        page: Int
    ): Result<HistoryPage, Int> {
        val action = "get history of show"
        val historyCall = traktUsers.history(
            UserSlug.ME,
            HistoryType.SHOWS,
            showTraktId,
            page,
            null,
            null,
            actionAtDateTime,
            actionAtDateTime
        )
        return executeTraktCall(
            context,
            trakt,
            historyCall,
            action
        ) { response, body ->
            val episodes = body.map {
                val number = it.episode?.number
                val season = it.episode?.season
                if (number == null || season == null) {
                    Errors.logAndReport(action, response, "episode is null")
                    return@executeTraktCall Err(ERROR_TRAKT_CLIENT)
                }
                WatchedEpisode(number, season)
            }
            val pageCount = response.headers()["x-pagination-page-count"]?.toIntOrNull()
                ?: 1
            Ok(HistoryPage(episodes, pageCount))
        }
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