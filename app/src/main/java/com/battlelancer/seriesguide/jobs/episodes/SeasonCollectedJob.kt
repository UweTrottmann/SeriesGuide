// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020-2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers

class SeasonCollectedJob(
    seasonId: Long,
    private val isCollected: Boolean
) : SeasonBaseJob(seasonId, if (isCollected) 1 else 0, JobAction.EPISODE_COLLECTION) {
    override fun applyDatabaseChanges(context: Context): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .updateCollectedOfSeason(seasonId, isCollected)
        return rowsUpdated >= 0 // -1 means error.
    }

    override fun getEpisodesForNetworkJob(context: Context): List<SgEpisode2Numbers> {
        return SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .getEpisodeNumbersOfSeason(seasonId)
    }

    override fun getPlaysForNetworkJob(plays: Int): Int {
        return plays // Collected change does not change plays.
    }
}
