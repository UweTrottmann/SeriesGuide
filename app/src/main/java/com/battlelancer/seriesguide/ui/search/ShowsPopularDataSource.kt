package com.battlelancer.seriesguide.ui.search

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.Tmdb

/**
 * Loads popular shows in pages from TMDB.
 */
class ShowsPopularDataSource(
        private val context: Context,
        private val tmdb: Tmdb
) : PageKeyedDataSource<Int, SearchResult>() {

    val networkState = MutableLiveData<NetworkState>()

    data class Page(
            val items: List<SearchResult>?,
            val totalCount: Int = -1
    )

    override fun loadInitial(params: LoadInitialParams<Int>,
            callback: LoadInitialCallback<Int, SearchResult>) {
        val page = loadPage(1)
        if (page.items != null) {
            val nextPage = if (page.items.isEmpty()) null else 2
            callback.onResult(page.items, 0, page.totalCount, null, nextPage)
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, SearchResult>) {
        val pageNumber = params.key
        val page = loadPage(pageNumber)
        if (page.items != null) {
            val previousPage = if (pageNumber > 1) pageNumber - 1 else null
            callback.onResult(page.items, previousPage)
        }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, SearchResult>) {
        val pageNumber = params.key
        val page = loadPage(pageNumber)
        if (page.items != null) {
            val nextPage = if (page.items.isEmpty()) null else pageNumber + 1
            callback.onResult(page.items, nextPage)
        }
    }

    private fun loadPage(page: Int): Page {
        networkState.postValue(NetworkState.LOADING)

        val languageCode = DisplaySettings.getShowsSearchLanguage(context)
        val action = "load popular shows"

        val response = try {
            tmdb.tvService().popular(
                page,
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
        } finally {
            networkState.postValue(NetworkState.LOADED)
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
        val totalResults = body.total_results
        if (totalResults == null) {
            Errors.logAndReport(action, IllegalStateException("total_results is null"))
            return buildResultGenericFailure()
        }

        val shows = body.results?.filterNotNull()
        return if (shows == null || shows.isEmpty()) {
            // return empty list right away if there are no results
            Page(emptyList(), totalResults)
        } else {
            val searchResults = SearchTools.mapTvShowsToSearchResults(languageCode, shows)
            SearchTools.markLocalShowsAsAddedAndPreferLocalPoster(context, searchResults)
            Page(searchResults, totalResults)
        }
    }

    private fun buildResultGenericFailure(): Page {
        networkState.postValue(NetworkState.error(
                context.getString(R.string.api_error_generic, context.getString(R.string.trakt))))
        return Page(null)
    }

    private fun buildResultOffline(): Page {
        networkState.postValue(NetworkState.error(context.getString(R.string.offline)))
        return Page(null)
    }
}
