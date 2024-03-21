// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.shows.search.discover.SearchTools
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.Tmdb
import java.io.IOException

/**
 * Loads popular shows in pages from TMDB.
 */
class ShowsPopularDataSource(
    private val context: Context,
    private val tmdb: Tmdb,
    private val languageCode: String,
    private val firstReleaseYear: Int?,
    private val originalLanguage: String?,
    private val watchProviderIds: List<Int>?,
    private val watchRegion: String?
) : PagingSource<Int, SearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        val pageNumber = params.key ?: 1
        val action = "load popular shows"
        val result = TmdbTools2().getPopularShows(
            tmdb,
            languageCode,
            pageNumber,
            firstReleaseYear,
            originalLanguage,
            watchProviderIds,
            watchRegion
        )
        if (result == null) {
            // Not checking for connection until here to allow hitting the response cache.
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildResultGenericFailure()
            } else {
                buildResultOffline()
            }
        }

        val totalResults = result.total_results
        if (totalResults == null) {
            Errors.logAndReport(action, IllegalStateException("total_results is null"))
            return buildResultGenericFailure()
        }

        val shows = result.results?.filterNotNull()
        return if (shows == null || shows.isEmpty()) {
            // return empty list right away if there are no results
            LoadResult.Page(
                data = emptyList(),
                prevKey = null, // Only paging forward.
                nextKey = null
            )
        } else {
            val searchResults = SearchTools.mapTvShowsToSearchResults(languageCode, shows)
            SearchTools.markLocalShowsAsAddedAndPreferLocalPoster(context, searchResults)
            LoadResult.Page(
                data = searchResults,
                prevKey = null, // Only paging forward.
                nextKey = pageNumber + 1
            )
        }
    }

    private fun buildResultGenericFailure(): LoadResult.Error<Int, SearchResult> {
        val message =
            context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
        return LoadResult.Error(IOException(message))
    }

    private fun buildResultOffline(): LoadResult.Error<Int, SearchResult> {
        val message = context.getString(R.string.offline)
        return LoadResult.Error(IOException(message))
    }

    override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
        // Always start loading at the previous page.
        // As refresh is currently only triggered by swipe-to-refresh will always be the first
        // page (prevKey == null).
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }
}
