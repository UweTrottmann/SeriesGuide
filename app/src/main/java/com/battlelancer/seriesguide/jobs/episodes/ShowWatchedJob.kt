// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020-2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import android.text.format.DateUtils
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools

class ShowWatchedJob(
    showId: Long,
    flagValue: Int, currentTime: Long
) : ShowBaseJob(showId, flagValue, JobAction.EPISODE_WATCHED_FLAG) {

    private val currentTimePlusOneHour: Long

    init {
        currentTimePlusOneHour = currentTime + DateUtils.HOUR_IN_MILLIS
    }

    override fun applyLocalChanges(context: Context, requiresNetworkJob: Boolean): Boolean {
        if (!super.applyLocalChanges(context, requiresNetworkJob)) {
            return false
        }



        return true
    }

    override fun applyDatabaseChanges(
        context: Context,
        episodes: List<SgEpisode2Numbers>
    ): Boolean {
        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val rowsUpdated: Int = when (flagValue) {
            EpisodeFlags.UNWATCHED -> helper.setShowNotWatchedAndRemovePlays(showId)
            EpisodeFlags.WATCHED -> helper.setShowWatchedAndAddPlay(showId, currentTimePlusOneHour)
            else -> {
                // Note: Skip not supported for whole show.
                throw IllegalArgumentException("Flag value not supported")
            }
        }
        val isSuccessful = rowsUpdated >= 0 // -1 means error

        if (isSuccessful) {
            val lastWatchedEpisodeId =
                if (EpisodeTools.isUnwatched(flagValue)) {
                    0L /* just reset */
                } else {
                    -1L /* we don't care */
                }

            // set a new last watched episode
            // set last watched time to now if marking as watched or skipped
            updateLastWatched(context, lastWatchedEpisodeId, !EpisodeTools.isUnwatched(flagValue))

            // Add or remove activity entries
            updateActivity(context, episodes)

            ListWidgetProvider.notifyDataChanged(context)
        }
        return isSuccessful
    }

    override fun getAffectedEpisodes(context: Context): List<SgEpisode2Numbers> {
        val helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        return if (EpisodeTools.isUnwatched(flagValue)) {
            // set unwatched
            // include watched or skipped episodes
            helper.getWatchedOrSkippedEpisodeNumbersOfShow(showId)
        } else {
            // set watched or skipped
            // do NOT mark watched episodes again to avoid trakt adding a new watch
            // only mark episodes that have been released until within the hour
            helper.getNotWatchedOrSkippedEpisodeNumbersOfShow(
                showId,
                currentTimePlusOneHour
            )
        }
    }

    /**
     * Note: this should mirror the planned database changes in [applyDatabaseChanges].
     */
    override fun getPlaysForNetworkJob(plays: Int): Int {
        return when (flagValue) {
            EpisodeFlags.WATCHED -> plays + 1
            EpisodeFlags.UNWATCHED -> 0
            else -> {
                // Note: Skip not supported for whole show.
                throw IllegalArgumentException("Flag value not supported")
            }
        }
    }
}
