// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2020-2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers

class EpisodeCollectedJob(
    episodeId: Long, private val isCollected: Boolean
) : EpisodeBaseJob(episodeId, if (isCollected) 1 else 0, JobAction.EPISODE_COLLECTION) {
    override fun applyDatabaseChanges(
        context: Context,
        episodes: List<SgEpisode2Numbers>
    ): Boolean {
        val updated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .updateCollected(episodeId, isCollected)
        return updated == 1
    }

    override fun getPlaysForNetworkJob(plays: Int): Int {
        return plays // Collected change does not change plays.
    }
}
