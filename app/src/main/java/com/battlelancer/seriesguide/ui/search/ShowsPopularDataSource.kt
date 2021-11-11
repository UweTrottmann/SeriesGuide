package com.battlelancer.seriesguide.ui.search

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.Tmdb
import java.io.IOException

/**
 * Loads popular shows in pages from TMDB.
 */
class ShowsPopularDataSource(
        private val context: Context,
        private val tmdb: Tmdb
) : PagingSource<Int, SearchResult>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, SearchResult> {
        val pageNumber = params.key ?: 1

        val languageCode = DisplaySettings.getShowsSearchLanguage(context)
        val action = "load popular shows"

        val response = try {
            tmdb.tvService().popular(
                pageNumber,
                languageCode
            ).execute()
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            // Not checking for connection until here to allow hitting the response cache.
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildResultGenericFailure()
            } else {
                buildResultOffline()
            }
        }

        // Check for failures or broken body.
        if (!response.isSuccessful) {
            Errors.logAndReport(action, response)
            return buildResultGenericFailure()
        }
        val body = response.body()
        if (body == null) {
            Errors.logAndReport(action, IllegalStateException("body is null"))
            return buildResultGenericFailure()
        }

        val shows = body.results?.filterNotNull()
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
            context.getString(R.string.api_error_generic, context.getString(R.string.trakt))
        return LoadResult.Error(IOException(message))
    }

    private fun buildResultOffline(): LoadResult.Error<Int, SearchResult> {
        val message = context.getString(R.string.offline)
        return LoadResult.Error(IOException(message))
    }

    override fun getRefreshKey(state: PagingState<Int, SearchResult>): Int? {
        // Try to find the page key of the closest page to anchorPosition, from
        // either the prevKey or the nextKey, but you need to handle nullability
        // here:
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey null -> anchorPage is the initial page, so
        //    just return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}
