// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020, 2021, 2023, 2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools

class EpisodeWatchedJob(
    episodeId: Long,
    episodeFlags: Int
) : EpisodeBaseJob(episodeId, episodeFlags, JobAction.EPISODE_WATCHED_FLAG) {

    private fun getLastWatchedEpisodeId(context: Context): Long {
        return if (!EpisodeTools.isUnwatched(flagValue)) {
            // watched or skipped episode
            episodeId
        } else {
            // changed episode to not watched
            var lastWatchedId: Long = -1 // don't change last watched episode by default

            // if modified episode is identical to last watched one (e.g. was just watched),
            // find an appropriate last watched episode
            val database = SgRoomDatabase.getInstance(context)
            val lastWatchedEpisodeId = database.sgShow2Helper()
                .getShowLastWatchedEpisodeId(showId)
            // identical to last watched episode?
            if (episodeId == lastWatchedEpisodeId) {
                if (episode.season == 0) {
                    // keep last watched (= this episode) if we got a special
                    return -1
                }
                lastWatchedId = 0 // re-set if we don't find one

                // get newest watched before this one
                val previousWatchedEpisodeId = database.sgEpisode2Helper()
                    .getPreviousWatchedEpisodeOfShow(
                        showId, episode.season,
                        episode.episodenumber
                    )
                if (previousWatchedEpisodeId > 0) {
                    lastWatchedId = previousWatchedEpisodeId
                }
            }
            lastWatchedId
        }
    }

    override fun applyDatabaseChanges(
        context: Context,
        episodes: List<SgEpisode2Numbers>
    ): Boolean {
        val episodeHelper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val flagValue = flagValue
        val rowsUpdated: Int = when (flagValue) {
            EpisodeFlags.SKIPPED -> episodeHelper.setSkipped(episodeId)
            EpisodeFlags.WATCHED -> episodeHelper.setWatchedAndAddPlay(episodeId)
            EpisodeFlags.UNWATCHED -> episodeHelper.setNotWatchedAndRemovePlays(episodeId)
            else -> throw IllegalArgumentException("Flag value not supported")
        }
        val isSuccessful = rowsUpdated == 1

        if (isSuccessful) {
            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            val unwatched = EpisodeTools.isUnwatched(flagValue)
            updateLastWatched(context, getLastWatchedEpisodeId(context), !unwatched)

            // Add or remove activity entry
            updateActivity(context, episodes)

            ListWidgetProvider.notifyDataChanged(context)
        }
        return isSuccessful
    }

    /**
     * Note: this should mirror the planned database changes in [applyDatabaseChanges].
     */
    override fun getPlaysForNetworkJob(plays: Int): Int {
        return when (flagValue) {
            EpisodeFlags.SKIPPED -> plays
            EpisodeFlags.WATCHED -> plays + 1
            EpisodeFlags.UNWATCHED -> 0
            else -> throw IllegalArgumentException("Flag value not supported")
        }
    }
}
