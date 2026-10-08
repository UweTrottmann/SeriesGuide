// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse.Success
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.GenericSimpleLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.LinkedList

/**
 * Loads either the connected Trakt user's watched, collected or watchlist-ed shows.
 */
class TraktAddLoader(
    context: Context,
    private val type: Type,
    private val traktTools: TraktTools4
) : GenericSimpleLoader<TraktAddLoader.Result>(context) {

    enum class Type {
        WATCHED, COLLECTION, WATCHLIST
    }

    class Result {
        var results: List<SearchResult>
        var emptyText: String

        constructor(results: List<SearchResult>, emptyText: String) {
            this.results = results
            this.emptyText = emptyText
        }

        constructor(results: List<SearchResult>, context: Context, @StringRes emptyTextResId: Int) {
            this.results = results
            this.emptyText = context.getString(emptyTextResId)
        }
    }

    override fun loadInBackground(): Result {
        val response = runBlocking(Dispatchers.Default) {
            when (type) {
                Type.WATCHED -> traktTools.getWatchedShows(noSeasons = true)
                Type.COLLECTION -> traktTools.getCollectedShows()
                Type.WATCHLIST -> traktTools.getShowsOnWatchlist()
            }
        }

        val shows = when (response) {
            is Success -> response.data
            is TraktTools4.TraktErrorResponse.IsUnauthorized -> {
                return buildResultFailure(R.string.trakt_error_credentials)
            }

            else -> {
                // Wait to check for network until here to allow hitting the response cache
                return if (AndroidUtils.isNetworkConnected(context)) {
                    buildResultGenericFailure()
                } else {
                    buildResultFailure(R.string.offline)
                }
            }
        }

        // return empty list right away if there are no results
        if (shows.isEmpty()) {
            return buildResultSuccess(emptyList())
        }


        val searchResults =
            TraktSearchResultMapper(context, ShowsSettings.getShowsSearchLanguage(context))
                .mapToSearchResults(shows)
        return buildResultSuccess(searchResults)
    }

    private fun buildResultSuccess(results: List<SearchResult>): Result {
        return Result(results, context, R.string.add_empty)
    }

    private fun buildResultGenericFailure(): Result {
        return Result(
            LinkedList(),
            context.getString(
                R.string.api_error_generic,
                context.getString(R.string.trakt)
            )
        )
    }

    private fun buildResultFailure(@StringRes errorResId: Int): Result {
        return Result(LinkedList(), context, errorResId)
    }

}
