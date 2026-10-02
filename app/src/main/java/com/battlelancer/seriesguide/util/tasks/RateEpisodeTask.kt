// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.services.Sync

/**
 * See [BaseRateItemTask]
 */
class RateEpisodeTask(
    context: Context,
    rating: Rating?,
    private val episodeId: Long
) : BaseRateItemTask<RateEpisodeTask.EpisodeTraktIds>(context, rating) {

    data class EpisodeTraktIds(
        val showTmdbId: Int,
        val season: Int,
        val episode: Int
    )

    override fun loadTraktIds(): EpisodeTraktIds? {
        val database = SgRoomDatabase.getInstance(context)

        val episode = database.sgEpisode2Helper().getEpisodeNumbers(episodeId) ?: return null

        val showTmdbId = database.sgShow2Helper().getShowTmdbId(episode.showId)
        if (showTmdbId == 0) return null

        return EpisodeTraktIds(showTmdbId, episode.season, episode.episodenumber)
    }

    override suspend fun sendToTrakt(
        traktSync: Sync,
        traktIds: EpisodeTraktIds
    ): TraktNonNullResponse<SyncResponse> {
        return TraktTools4.rateEpisode(
            traktSync,
            traktIds.showTmdbId,
            traktIds.season,
            traktIds.episode,
            rating
        )
    }

    override fun doDatabaseUpdate(): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
            .updateUserRating(episodeId, rating?.value ?: 0)
        return rowsUpdated > 0
    }
}
