// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncEpisode
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncSeason
import com.uwetrottmann.trakt5.entities.SyncShow
import com.uwetrottmann.trakt5.enums.Rating

class RateEpisodeTask(
    context: Context,
    rating: Rating,
    private val episodeId: Long
) : BaseRateItemTask(context, rating) {

    override val traktAction: String
        get() = "rate episode"

    override fun buildTraktSyncItems(): SyncItems? {
        val database = SgRoomDatabase.getInstance(context)

        val episode = database.sgEpisode2Helper().getEpisodeNumbers(episodeId) ?: return null

        val showTmdbId = database.sgShow2Helper().getShowTmdbId(episode.showId)
        if (showTmdbId == 0) return null

        return SyncItems()
            .shows(
                SyncShow().id(ShowIds.tmdb(showTmdbId))
                    .seasons(
                        SyncSeason().number(episode.season)
                            .episodes(
                                SyncEpisode().number(episode.episodenumber)
                                    .rating(rating)
                            )
                    )
            )
    }

    override fun doDatabaseUpdate(): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .updateUserRating(episodeId, rating.value)
        return rowsUpdated > 0
    }
}
