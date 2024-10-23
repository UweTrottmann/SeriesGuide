// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.androidutils.GenericSimpleLoader
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.enums.Extended
import retrofit2.Response
import java.util.LinkedList

/**
 * Loads either the connected Trakt user's watched, collected or watchlist-ed shows.
 */
class TraktAddLoader(
    context: Context,
    private val type: Type
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

    private val trakt: TraktV2 = SgApp.getServicesComponent(context).trakt()

    override fun loadInBackground(): Result {
        var shows: List<BaseShow> = emptyList()
        var action: String? = null
        try {
            val response: Response<List<BaseShow>>
            when (type) {
                Type.WATCHED -> {
                    action = "load watched shows"
                    response = trakt.sync().watchedShows(Extended.NOSEASONS).execute()
                }

                Type.COLLECTION -> {
                    action = "load show collection"
                    response = trakt.sync().collectionShows(null).execute()
                }

                Type.WATCHLIST -> {
                    action = "load show watchlist"
                    response = trakt.sync().watchlistShows(Extended.FULL).execute()
                }
            }

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    shows = body
                }
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return buildResultFailure(R.string.trakt_error_credentials)
                }
                Errors.logAndReport(action, response)
                return buildResultGenericFailure()
            }
        } catch (e: Exception) {
            Errors.logAndReport(action!!, e)
            // only check for network here to allow hitting the response cache
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildResultGenericFailure()
            } else {
                buildResultFailure(R.string.offline)
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
