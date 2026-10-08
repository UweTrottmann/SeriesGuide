// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.shows.history

import android.content.Context
import android.text.format.DateUtils
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.LanguageTools
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.trakt5.entities.HistoryEntry
import com.uwetrottmann.trakt5.entities.UserSlug
import com.uwetrottmann.trakt5.enums.HistoryType
import com.uwetrottmann.trakt5.services.Users
import retrofit2.Call

/**
 * Loads last 24 hours of trakt watched episodes, or at least one older episode.
 */
open class TraktRecentEpisodeHistoryLoader(context: Context) :
    GenericSimpleLoader<TraktRecentEpisodeHistoryLoader.Result>(context) {

    class Result(
        val items: List<ShowsHistoryAdapter.Item>?,
        val errorText: String? = null
    )

    override fun loadInBackground(): Result {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return buildResultFailure(R.string.trakt_error_credentials)
        }

        var history: List<HistoryEntry>? = null
        try {
            val response = buildCall().execute()
            if (response.isSuccessful) {
                history = response.body()
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return buildResultFailure(R.string.trakt_error_credentials)
                }
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildResultFailure()
            } else {
                buildResultFailure(R.string.offline)
            }
        }

        if (history == null) {
            return buildResultFailure()
        } else if (history.isEmpty()) {
            return Result(null) // no history available (yet)
        }

        val items = mutableListOf<ShowsHistoryAdapter.Item>()
        // add header
        items.add(
            ShowsHistoryAdapter.Item()
                .header(context.getString(R.string.recently_watched), true)
        )
        // add items
        addItems(items, history)
        // add link to more history
        items.add(ShowsHistoryAdapter.Item().moreLink(context.getString(R.string.user_stream)))

        return Result(items)
    }

    protected open fun addItems(
        items: MutableList<ShowsHistoryAdapter.Item>,
        history: List<HistoryEntry>
    ) {
        val tmdbIdsToPoster = SgApp.getServicesComponent(context).showTools().getTmdbIdsToPoster()
        val episodeHelper = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
        val timeDayAgo = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS

        for (entry in history) {
            val episode = entry.episode
            val show = entry.show
            val watchedAt = entry.watched_at
            if (episode == null || show == null || watchedAt == null) {
                // missing required values
                continue
            }

            // only include episodes watched in the last 24 hours
            // however, include at least one older episode if there are none, yet
            if (TimeTools.isBeforeMillis(watchedAt, timeDayAgo) && items.size > 1) {
                break
            }

            // look for a poster
            val showTmdbId = show.ids?.tmdb
            val posterUrl = if (showTmdbId != null) {
                // prefer poster of already added show, fall back to first uploaded poster
                ImageTools.posterUrlOrResolve(
                    tmdbIdsToPoster.get(showTmdbId),
                    showTmdbId,
                    LanguageTools.LANGUAGE_EN,
                    context
                )
            } else {
                null
            }

            val season = episode.season
            val number = episode.number
            val description = if (season == null || number == null) {
                episode.title
            } else {
                TextTools.getNextEpisodeString(context, season, number, episode.title)
            }

            val episodeTmdbIdOrNull = episode.ids?.tmdb
            val localEpisodeIdOrZero = if (episodeTmdbIdOrNull != null) {
                episodeHelper.getEpisodeIdByTmdbId(episodeTmdbIdOrNull)
            } else {
                0
            }

            val item = ShowsHistoryAdapter.Item()
                .displayData(
                    watchedAt.toInstant().toEpochMilli(),
                    show.title,
                    description,
                    posterUrl
                )
                .episodeIds(localEpisodeIdOrZero, showTmdbId ?: 0)
                .recentlyWatchedTrakt(entry.action)
            items.add(item)
        }
    }

    protected open val action: String
        get() = "get user episode history"

    protected open fun buildCall(): Call<List<HistoryEntry>> {
        val traktUsers: Users = SgApp.getServicesComponent(context).traktUsers()!!
        return traktUsers.history(
            UserSlug.ME, HistoryType.EPISODES, 1, MAX_HISTORY_SIZE,
            null, null, null
        )
    }

    private fun buildResultFailure(): Result {
        return Result(
            null,
            context.getString(R.string.api_error_generic, context.getString(R.string.trakt))
        )
    }

    private fun buildResultFailure(@StringRes emptyTextResId: Int): Result {
        return Result(null, context.getString(emptyTextResId))
    }

    companion object {
        const val MAX_HISTORY_SIZE = 10
    }
}
