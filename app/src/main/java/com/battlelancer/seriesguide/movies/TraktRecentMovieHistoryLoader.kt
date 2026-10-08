// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies

import android.content.Context
import android.text.format.DateUtils
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.shows.history.ShowsHistoryAdapter
import com.battlelancer.seriesguide.shows.history.TraktRecentEpisodeHistoryLoader
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.HistoryType
import retrofit2.Call

/**
 * Loads last 72 hours of trakt watched movies, or at least one older watched movie.
 */
class TraktRecentMovieHistoryLoader(context: Context) :
    TraktRecentEpisodeHistoryLoader(context) {

    override fun addItems(
        items: MutableList<ShowsHistoryAdapter.Item>,
        history: List<HistoryEntry>
    ) {
        // add movies
        val threeDaysAgo = System.currentTimeMillis() - 3 * DateUtils.DAY_IN_MILLIS
        for (entry in history) {
            val movie = entry.movie
            val movieTmdbId = movie?.ids?.tmdb
            val watchedAt = entry.watched_at
            if (movie == null || movieTmdbId == null || watchedAt == null) {
                // missing required values
                continue
            }

            // only include movies watched in the last 72 hours
            // however, include at least one older one if there are none
            if (TimeTools.isBeforeMillis(watchedAt, threeDaysAgo) && items.size > 1) {
                break
            }

            // Poster resolved on demand, see view holder binding.
            items.add(
                ShowsHistoryAdapter.Item()
                    .displayData(
                        watchedAt.toInstant().toEpochMilli(),
                        movie.title,
                        null,
                        null
                    )
                    .tmdbId(movieTmdbId)
                    .recentlyWatchedTrakt(entry.action)
            )
        }
    }

    override val action: String
        get() = "get user movie history"

    override fun buildCall(): Call<List<HistoryEntry>> {
        return SgApp.getServicesComponent(context).trakt().users().history(
            UserSlug.ME, HistoryType.MOVIES, 1, MAX_HISTORY_SIZE,
            null, null, null
        )
    }
}
