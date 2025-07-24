// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2025 Uwe Trottmann

package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Helper
import com.battlelancer.seriesguide.shows.database.SgEpisode2Numbers

/**
 * Flagging single episodes watched or collected.
 */
abstract class EpisodeBaseJob(
    protected val episodeId: Long,
    flagValue: Int,
    action: JobAction
) : BaseEpisodesJob(flagValue, action) {

    lateinit var episode: SgEpisode2Numbers

    override suspend fun applyLocalChanges(context: Context, requiresNetworkJob: Boolean): Boolean {
        // Gather data needed for later steps.
        val helper: SgEpisode2Helper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val episode: SgEpisode2Numbers = helper.getEpisodeNumbers(episodeId)
            ?: return false
        this.episode = episode

        return super.applyLocalChanges(context, requiresNetworkJob)
    }

    override val showId: Long
        get() = episode.showId

    override fun getAffectedEpisodes(context: Context): List<SgEpisode2Numbers> {
        val list = ArrayList<SgEpisode2Numbers>()
        list.add(episode)
        return list
    }
}