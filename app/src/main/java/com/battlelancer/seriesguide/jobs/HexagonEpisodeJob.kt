package com.battlelancer.seriesguide.jobs

import android.content.Context
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync
import com.battlelancer.seriesguide.sync.NetworkJobProcessor.JobResult
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools
import com.battlelancer.seriesguide.util.Errors
import com.google.api.client.http.HttpResponseException
import com.uwetrottmann.seriesguide.backend.episodes.model.SgCloudEpisode
import com.uwetrottmann.seriesguide.backend.episodes.model.SgCloudEpisodeList
import java.io.IOException
import java.util.ArrayList

class HexagonEpisodeJob(
    private val hexagonTools: HexagonTools,
    action: JobAction,
    jobInfo: SgJobInfo
) : BaseNetworkEpisodeJob(action, jobInfo) {

    override fun execute(context: Context): JobResult {
        val showTmdbIdOrZero = SgRoomDatabase.getInstance(context).sgShow2Helper()
            .getShowTmdbId(jobInfo.showId())
        if (showTmdbIdOrZero <= 0) {
            // Can't run this job (for now), report error and remove.
            return buildResult(context, NetworkJob.ERROR_HEXAGON_CLIENT)
        }

        val uploadWrapper = SgCloudEpisodeList()
        uploadWrapper.showTmdbId = showTmdbIdOrZero

        // upload in small batches
        var smallBatch: MutableList<SgCloudEpisode> = ArrayList()
        val episodes = getEpisodesForHexagon()
        while (episodes.isNotEmpty()) {
            // batch small enough?
            if (episodes.size <= HexagonEpisodeSync.MAX_BATCH_SIZE) {
                smallBatch = episodes
            } else {
                // build smaller batch
                for (count in 0 until HexagonEpisodeSync.MAX_BATCH_SIZE) {
                    if (episodes.isEmpty()) {
                        break
                    }
                    smallBatch.add(episodes.removeAt(0))
                }
            }

            // upload
            uploadWrapper.episodes = smallBatch

            try {
                val episodesService = hexagonTools.episodesService
                    ?: return buildResult(context, NetworkJob.ERROR_HEXAGON_AUTH)
                episodesService.saveSgEpisodes(uploadWrapper).execute()
            } catch (e: HttpResponseException) {
                Errors.logAndReportHexagon("save episodes", e)
                val code = e.statusCode
                return if (code in 400..499) {
                    buildResult(context, NetworkJob.ERROR_HEXAGON_CLIENT)
                } else {
                    buildResult(context, NetworkJob.ERROR_HEXAGON_SERVER)
                }
            } catch (e: IOException) {
                Errors.logAndReportHexagon("save episodes", e)
                return buildResult(context, NetworkJob.ERROR_CONNECTION)
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("save episodes", e)
                return buildResult(context, NetworkJob.ERROR_CONNECTION)
            }

            // prepare for next batch
            smallBatch.clear()
        }
        return buildResult(context, NetworkJob.SUCCESS)
    }

    /**
     * Builds a list of episodes ready to upload to hexagon. However, the show id is not set.
     * It should be set in the wrapping entity.
     */
    private fun getEpisodesForHexagon(): MutableList<SgCloudEpisode> {
        val isWatchedNotCollected = when (action) {
            JobAction.EPISODE_WATCHED_FLAG -> true
            JobAction.EPISODE_COLLECTION -> false
            else -> throw IllegalArgumentException("Action $action not supported.")
        }

        val episodes: MutableList<SgCloudEpisode> = ArrayList()
        for (i in 0 until jobInfo.episodesLength()) {
            val episodeInfo = jobInfo.episodes(i)

            val episode = SgCloudEpisode()
            episode.seasonNumber = episodeInfo.season()
            episode.episodeNumber = episodeInfo.number()
            if (isWatchedNotCollected) {
                episode.watchedFlag = jobInfo.flagValue()
                // Always upload (regardless if watched, skipped or not watched).
                // Also ensures legacy data slowly adds the new plays field.
                episode.plays = episodeInfo.plays()
            } else {
                episode.isInCollection = EpisodeTools.isCollected(jobInfo.flagValue())
            }
            episodes.add(episode)
        }
        return episodes
    }
}