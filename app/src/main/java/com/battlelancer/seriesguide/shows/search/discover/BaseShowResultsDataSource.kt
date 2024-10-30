// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.TvShowResultsPage
import java.io.IOException

/**
 * Loads [TvShowResultsPage] in pages from TMDB.
 */
abstract class BaseShowResultsDataSource(
    private val context: Context,
    private val tmdb: Tmdb,
    private val languageCode: String,
) : PagingSource<Int, SearchResult>() {

    abstract val action: String
    abstract suspend fun loadShows(
        tmdb: Tmdb,
        language: String,
        page: Int,
    ): TvShowResultsPage?

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        val pageNumber = params.key ?: 1
        val result = loadShows(
            tmdb,
            languageCode,
            pageNumber
        )
            ?: return if (AndroidUtils.isNetworkConnected(context)) {
                // Not checking for connection until here to allow hitting the response cache.
                buildResultGenericFailure()
            } else {
                buildResultOffline()
            }

        val totalResults = result.total_results
        if (totalResults == null) {
            Errors.logAndReport(action, IllegalStateException("total_results is null"))
            return buildResultGenericFailure()
        }

        val shows = result.results?.filterNotNull()
        return if (shows.isNullOrEmpty()) {
            // return empty list right away if there are no results
            LoadResult.Page(
                data = emptyList(),
                prevKey = null, // Only paging forward.
                nextKey = null
            )
        } else {
            val searchResults = TmdbSearchResultMapper(context, languageCode)
                .mapToSearchResults(shows)
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
