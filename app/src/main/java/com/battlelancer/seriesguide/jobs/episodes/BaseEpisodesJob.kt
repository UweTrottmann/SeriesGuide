// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020-2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import androidx.annotation.CallSuper
import com.battlelancer.seriesguide.jobs.BaseFlagJob
import com.battlelancer.seriesguide.jobs.EpisodeInfo
import com.battlelancer.seriesguide.jobs.SgJobInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.shows.history.SgActivityHelper
import com.battlelancer.seriesguide.shows.tools.LatestEpisodeUpdateTask
import com.google.flatbuffers.FlatBufferBuilder

abstract class BaseEpisodesJob(
    protected val flagValue: Int,
    action: JobAction
) : BaseFlagJob(action) {

    protected abstract val showId: Long

    override fun supportsHexagon(): Boolean {
        return true
    }

    override fun supportsTrakt(): Boolean {
        /* No need to create network job for skipped episodes, not supported by trakt.
        Note that a network job might still be created if hexagon is connected. */
        return !EpisodeTools.isSkipped(flagValue)
    }

    /**
     * Builds and executes the database op required to flag episodes in the local database,
     * notifies affected URIs, may update the list widget.
     */
    @CallSuper
    override fun applyLocalChanges(context: Context, requiresNetworkJob: Boolean): Boolean {
        val episodes: List<SgEpisode2Numbers> = getAffectedEpisodes(context)

        // prepare network job
        var networkJobInfo: ByteArray? = null
        if (requiresNetworkJob) {
            networkJobInfo = prepareNetworkJob(episodes)
            if (networkJobInfo == null) {
                return false
            }
        }

        // apply local updates
        val updated = applyDatabaseChanges(context, episodes)
        if (!updated) {
            return false
        }

        // persist network job after successful local updates
        if (requiresNetworkJob) {
            if (!persistNetworkJob(context, networkJobInfo!!)) {
                return false
            }
        }

        // notify some other URIs about updates
        context.contentResolver
            .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null)

        return true
    }

    protected abstract fun applyDatabaseChanges(
        context: Context,
        episodes: List<SgEpisode2Numbers>
    ): Boolean

    /**
     * Note: Ensure episodes are ordered by season number (lowest first),
     * then episode number (lowest first).
     */
    protected abstract fun getAffectedEpisodes(context: Context): List<SgEpisode2Numbers>

    /**
     * Returns the number of plays to upload to Cloud (Trakt currently not supported)
     * based on the current number of plays (before [.applyLocalChanges].
     */
    protected abstract fun getPlaysForNetworkJob(plays: Int): Int

    /**
     * Store affected episodes for network job.
     */
    private fun prepareNetworkJob(episodes: List<SgEpisode2Numbers>): ByteArray? {
        val builder = FlatBufferBuilder(0)

        val episodeInfos = IntArray(episodes.size)
        for (i in episodes.indices) {
            val episode = episodes[i]
            val newPlays = getPlaysForNetworkJob(episode.plays)
            episodeInfos[i] = EpisodeInfo
                .createEpisodeInfo(builder, episode.season, episode.episodenumber, newPlays)
        }

        val episodesVector = SgJobInfo.createEpisodesVector(builder, episodeInfos)
        val jobInfo = SgJobInfo.createSgJobInfo(builder, flagValue, episodesVector, 0, 0, showId)

        builder.finish(jobInfo)
        return builder.sizedByteArray()
    }

    /**
     * Set last watched episode and/or last watched time of a show, then update the episode shown as
     * next.
     *
     * @param lastWatchedEpisodeId The last watched episode for a show to save to the database. -1
     * for no-op.
     * @param setLastWatchedToNow Whether to set the last watched time of a show to now.
     */
    protected fun updateLastWatched(
        context: Context,
        lastWatchedEpisodeId: Long, setLastWatchedToNow: Boolean
    ) {
        if (lastWatchedEpisodeId != -1L || setLastWatchedToNow) {
            SgRoomDatabase.getInstance(context).sgShow2Helper()
                .updateLastWatchedEpisodeIdAndTime(
                    showId, lastWatchedEpisodeId,
                    setLastWatchedToNow
                )
        }
        LatestEpisodeUpdateTask.updateLatestEpisodeFor(context, showId)
    }

    /**
     * Add or remove watch activity entries for episodes. Only used for watch jobs.
     */
    protected fun updateActivity(context: Context, episodes: List<SgEpisode2Numbers>) {
        val showTmdbIdOrZero =
            SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
        val episodeTmdbIds = episodes.mapNotNull { it.tmdbId }

        if (showTmdbIdOrZero == 0 && episodeTmdbIds.isEmpty()) return

        if (EpisodeTools.isWatched(flagValue)) {
            SgActivityHelper.addActivitiesForEpisodes(context, showTmdbIdOrZero, episodeTmdbIds)
        } else if (EpisodeTools.isUnwatched(flagValue)) {
            SgActivityHelper.removeActivitiesForEpisodes(context, episodeTmdbIds)
        }
    }
}
