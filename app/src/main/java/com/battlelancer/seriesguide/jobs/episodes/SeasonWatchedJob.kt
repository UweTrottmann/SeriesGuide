// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020-2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools

/**
 * Sets *all* episodes of a season watched, skipped or not watched. Includes also episodes that do
 * not have a release date, which is common for special episodes. Also it is unexpected if all does
 * not mean all (previously this only marked episodes with release date up to current time + 1 hour).
 */
class SeasonWatchedJob(seasonId: Long, episodeFlags: Int) :
    SeasonBaseJob(seasonId, episodeFlags, JobAction.EPISODE_WATCHED_FLAG) {
    private fun getLastWatchedEpisodeId(context: Context): Long {
        return if (EpisodeTools.isUnwatched(flagValue)) {
            // unwatched season
            // just reset
            0
        } else {
            // watched or skipped season

            // Get the highest episode of the season.
            val highestWatchedId = SgRoomDatabase.getInstance(context)
                .sgEpisode2Helper()
                .getHighestEpisodeOfSeason(seasonId)
            if (highestWatchedId != 0L) {
                highestWatchedId
            } else {
                -1 // do not change
            }
        }
    }

    override fun applyDatabaseChanges(context: Context): Boolean {
        val database = SgRoomDatabase.getInstance(context)
        val helper = database.sgEpisode2Helper()
        val rowsUpdated = when (flagValue) {
            EpisodeFlags.SKIPPED -> helper.setSeasonSkipped(seasonId)
            EpisodeFlags.WATCHED -> helper.setSeasonWatchedAndAddPlay(seasonId)
            EpisodeFlags.UNWATCHED -> helper.setSeasonNotWatchedAndRemovePlays(seasonId)
            else -> throw IllegalArgumentException("Flag value not supported")
        }

        // set a new last watched episode
        // set last watched time to now if marking as watched or skipped
        val unwatched = EpisodeTools.isUnwatched(flagValue)
        updateLastWatched(context, getLastWatchedEpisodeId(context), !unwatched)

        ListWidgetProvider.notifyDataChanged(context)

        return rowsUpdated >= 0 // -1 means error.
    }

    override fun getEpisodesForNetworkJob(context: Context): List<SgEpisode2Numbers> {
        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        return if (EpisodeTools.isUnwatched(flagValue)) {
            // set unwatched
            // include watched or skipped episodes
            helper.getWatchedOrSkippedEpisodeNumbersOfSeason(seasonId)
        } else {
            // set watched or skipped
            // do NOT mark watched episodes again to avoid Trakt adding a new watch
            helper.getNotWatchedOrSkippedEpisodeNumbersOfSeason(seasonId)
        }
    }

    /**
     * Note: this should mirror the planned database changes in [BaseEpisodesJob.applyDatabaseChanges].
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
