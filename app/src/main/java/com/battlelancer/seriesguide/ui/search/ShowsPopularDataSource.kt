package com.battlelancer.seriesguide.ui.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.services.Shows

class ShowsPopularDataSource(
        private val context: Context,
        private val traktShows: Shows
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

        val shows: List<Show>?
        val totalCount: Int
        val action = "load popular shows"

        try {
            val response = traktShows.popular(page, 25, Extended.FULL).execute()
            if (response.isSuccessful) {
                shows = response.body()
                totalCount = response.headers().get("X-Pagination-Item-Count")?.toInt()
                        ?: throw IllegalStateException("Item count header missing")
            } else {
                Errors.logAndReport(action, response)
                return buildResultGenericFailure()
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            // only check for network here to allow hitting the response cache
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildResultGenericFailure()
            } else {
                buildResultFailure(R.string.offline)
            }
        }

        networkState.postValue(NetworkState.LOADED)
        return if (shows == null || shows.isEmpty()) {
            // return empty list right away if there are no results
            Page(emptyList(), totalCount)
        } else {
            Page(TraktAddLoader.parseTraktShowsToSearchResults(context, shows,
                    DisplaySettings.getSearchLanguageOrFallbackIfAny(context)),
                    totalCount)
        }
    }

    private fun buildResultGenericFailure(): Page {
        networkState.postValue(NetworkState.error(
                context.getString(R.string.api_error_generic, context.getString(R.string.trakt))))
        return Page(null)
    }

    private fun buildResultFailure(@StringRes errorResId: Int): Page {
        networkState.postValue(NetworkState.error(context.getString(errorResId)))
        return Page(null)
    }
}
