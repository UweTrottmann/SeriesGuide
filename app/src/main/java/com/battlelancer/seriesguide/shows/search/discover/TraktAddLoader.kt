// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import androidx.annotation.StringRes
import androidx.collection.SparseArrayCompat
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
    private val type: DiscoverShowsLink
) : GenericSimpleLoader<TraktAddLoader.Result>(context) {

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
                DiscoverShowsLink.WATCHED -> {
                    action = "load watched shows"
                    response = trakt.sync().watchedShows(Extended.NOSEASONS).execute()
                }

                DiscoverShowsLink.COLLECTION -> {
                    action = "load show collection"
                    response = trakt.sync().collectionShows(null).execute()
                }

                DiscoverShowsLink.WATCHLIST -> {
                    action = "load show watchlist"
                    response = trakt.sync().watchlistShows(Extended.FULL).execute()
                }

                else -> {
                    throw IllegalArgumentException("Unknown type $type")
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

        return buildResultSuccess(
            parseTraktShowsToSearchResults(
                shows,
                SgApp.getServicesComponent(context).showTools().getTmdbIdsToPoster(),
                ShowsSettings.getShowsSearchLanguage(context)
            )
        )
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


    /**
     * Transforms a list of Trakt shows to a list of [SearchResult], marks shows already in
     * the local database as added.
     */
    private fun parseTraktShowsToSearchResults(
        traktShows: List<BaseShow>,
        existingPosterPaths: SparseArrayCompat<String>,
        overrideLanguage: String
    ): List<SearchResult> {
        val results: MutableList<SearchResult> = ArrayList()

        // build list
        for (baseShow in traktShows) {
            val show = baseShow.show
            val tmdbId = show?.ids?.tmdb
                ?: continue // has no TMDB id

            val result = SearchResult().also {
                it.tmdbId = tmdbId
                it.title = show.title
                // Trakt might not return an overview, so use the year if available
                it.overview = if (!show.overview.isNullOrEmpty()) {
                    show.overview
                } else if (show.year != null) {
                    show.year!!.toString()
                } else {
                    ""
                }
                if (existingPosterPaths.indexOfKey(tmdbId) >= 0) {
                    // is already in local database
                    it.state = SearchResult.STATE_ADDED
                    // use the poster fetched for it (or null if there is none)
                    it.posterPath = existingPosterPaths[tmdbId]
                }
                it.language = overrideLanguage
            }
            results.add(result)
        }
        return results
    }
}
