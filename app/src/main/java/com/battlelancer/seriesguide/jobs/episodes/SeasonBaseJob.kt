// SPDX-License-Identifier: Apache-2.0
// Copyright 2017, 2018, 2021-2024 Uwe Trottmann
package com.battlelancer.seriesguide.jobs.episodes

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgSeason2Numbers

/**
 * Flagging whole seasons watched or collected.
 */
abstract class SeasonBaseJob(
    val seasonId: Long,
    flagValue: Int,
    action: JobAction
) : BaseEpisodesJob(flagValue, action) {

    private lateinit var season: SgSeason2Numbers
    override fun applyLocalChanges(context: Context, requiresNetworkJob: Boolean): Boolean {
        val season = SgRoomDatabase.getInstance(context).sgSeason2Helper()
            .getSeasonNumbers(seasonId) ?: return false
        this.season = season
        return super.applyLocalChanges(context, requiresNetworkJob)
    }

    fun getSeason(): SgSeason2Numbers {
        return season
    }

    override val showId: Long
        get() = getSeason().showId
}
