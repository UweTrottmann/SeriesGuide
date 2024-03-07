// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags

/**
 * Set episodes watched up to (== including) a specific one excluding those with no release date.
 */
class EpisodeWatchedUpToJob(
    override val showId: Long,
    private val episodeFirstAired: Long,
    private val episodeNumber: Int
) : BaseEpisodesJob(EpisodeFlags.WATCHED, JobAction.EPISODE_WATCHED_FLAG) {

    override fun applyDatabaseChanges(
        context: Context,
        episodes: List<SgEpisode2Numbers>
    ): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .setWatchedUpToAndAddPlay(showId, episodeFirstAired, episodeNumber)
        val isSuccessful = rowsUpdated >= 0 // -1 means error

        if (isSuccessful) {
            // we don't care about the last watched episode value
            // always update last watched time, this type only marks as watched
            updateLastWatched(context, -1, true)

            // Add or remove activity entries
            updateActivity(context, episodes)

            ListWidgetProvider.notifyDataChanged(context)
        }
        return isSuccessful
    }

    override fun getAffectedEpisodes(context: Context): List<SgEpisode2Numbers> {
        return SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .getEpisodeNumbersForWatchedUpTo(showId, episodeFirstAired, episodeNumber)
    }

    /**
     * Note: this should mirror the planned database changes in [applyDatabaseChanges].
     */
    override fun getPlaysForNetworkJob(plays: Int): Int {
        return plays + 1
    }
}
