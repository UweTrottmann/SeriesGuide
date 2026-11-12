// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating

/**
 * See [BaseRateItemTask]
 */
class RateEpisodeTask(
    context: Context,
    rating: Rating?,
    private val episodeId: Long
) : BaseRateItemTask(context, rating) {

    private var showTmdbId = 0
    private var seasonNumber = 0
    private var episodeNumber = 0

    override fun loadTraktIds(): Boolean {
        val database = SgRoomDatabase.getInstance(context)

        val episode = database.sgEpisode2Helper().getEpisodeNumbers(episodeId) ?: return false

        showTmdbId = database.sgShow2Helper().getShowTmdbId(episode.showId)
        if (showTmdbId == 0) return false

        seasonNumber = episode.season
        episodeNumber = episode.episodenumber
        return true
    }

    override suspend fun sendToTrakt(traktTools: TraktTools4): TraktNonNullResponse<SyncResponse> {
        return traktTools.rateEpisode(
            showTmdbId,
            seasonNumber,
            episodeNumber,
            rating
        )
    }

    override fun doDatabaseUpdate(): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .updateUserRating(episodeId, rating?.value ?: 0)
        return rowsUpdated > 0
    }
}
