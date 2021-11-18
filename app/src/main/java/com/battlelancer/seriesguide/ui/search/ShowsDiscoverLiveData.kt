package com.battlelancer.seriesguide.ui.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gets search results for the given query, or if the query is blank gets shows with new episodes.
 */
class ShowsDiscoverLiveData(
    private val context: Context,
    private val scope: CoroutineScope
) : LiveData<ShowsDiscoverLiveData.Result>() {

    data class Result(
        val searchResults: List<SearchResult>,
        val emptyText: String,
        val isResultsForQuery: Boolean,
        val successful: Boolean
    )

    private var query: String = ""
    private var language: String = context.getString(R.string.show_default_language)
    private var watchProviderIds: List<Int>? = null
    private var currentJob: Job? = null

    /**
     * Schedules loading, give two letter ISO 639-1 [language] code.
     * Set [forceLoad] to load new set of results even if language has not changed.
     * Returns if it will load.
     */
    fun load(
        query: String,
        language: String,
        watchProviderIds: List<Int>?,
        forceLoad: Boolean
    ): Boolean {
        return if (
            forceLoad
            || this.query != query
            || this.language != language
            || this.watchProviderIds != watchProviderIds
            || currentJob == null
        ) {
            this.query = query
            this.language = language

            currentJob?.cancel()
            currentJob = scope.launch(Dispatchers.IO) {
                fetchDiscoverData(query, language, watchProviderIds)
            }
            true
        } else {
            false
        }
    }

    private suspend fun fetchDiscoverData(
        query: String,
        language: String,
        watchProviderIds: List<Int>?
    ) = withContext(Dispatchers.IO) {
        val result = if (query.isBlank()) {
            // No query: load a list of shows with new episodes in the last 7 days.
            getShowsWithNewEpisodes(language, watchProviderIds)
        } else {
            // Have a query: search.
            searchShows(query, language)
        }
        // Note: Do not bother posting results if cancelled.
        if (isActive) {
            postValue(result)
        }
    }

    private suspend fun getShowsWithNewEpisodes(
        language: String,
        watchProviderIds: List<Int>?
    ): Result = withContext(Dispatchers.IO) {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        val languageActual = language
        val watchRegion = StreamingSearch.getCurrentRegionOrNull(context)
        val results =
            TmdbTools2().getShowsWithNewEpisodes(tmdb, language, watchProviderIds, watchRegion)
        return@withContext if (results != null) {
            val searchResults = SearchTools.mapTvShowsToSearchResults(languageActual, results)
            SearchTools.markLocalShowsAsAddedAndPreferLocalPoster(context, searchResults)
            buildResultSuccess(searchResults, R.string.add_empty, false)
        } else {
            buildResultFailure(false)
        }
    }

    private suspend fun searchShows(
        query: String,
        language: String
    ): Result = withContext(Dispatchers.IO) {
        val results = TmdbTools2().searchShows(query, language, context)
        return@withContext if (results != null) {
            val searchResults = SearchTools.mapTvShowsToSearchResults(language, results)
            SearchTools.markLocalShowsAsAddedAndPreferLocalPoster(context, searchResults)
            buildResultSuccess(searchResults, R.string.no_results, true)
        } else {
            buildResultFailure(true)
        }
    }

    private fun buildResultSuccess(
        results: List<SearchResult>?, @StringRes emptyTextResId: Int,
        isResultsForQuery: Boolean
    ): Result {
        return Result(
            results ?: emptyList(), context.getString(emptyTextResId),
            isResultsForQuery,
            true
        )
    }

    private fun buildResultFailure(
        isResultsForQuery: Boolean
    ): Result {
        // only check for network here to allow hitting the response cache
        val emptyText = if (AndroidUtils.isNetworkConnected(context)) {
            context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
        } else {
            context.getString(R.string.offline)
        }
        return Result(emptyList(), emptyText, isResultsForQuery, false)
    }

}
