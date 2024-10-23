// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gets shows with new episodes in the last 7 days.
 */
class ShowsDiscoverLiveData(
    private val context: Context,
    private val scope: CoroutineScope,
    private val isRefreshing: MutableStateFlow<Boolean>,
    private val language: String,
    private val watchProviderIds: List<Int>?,
    private val watchRegion: String?,
    private val firstReleaseYear: Int?,
    private val originalLanguage: String?
) : LiveData<ShowsDiscoverLiveData.Result>() {

    data class Result(
        val searchResults: List<SearchResult>,
        val emptyText: String,
        val successful: Boolean
    )

    private var currentJob: Job? = null

    init {
        refresh()
    }

    /**
     * Loads data. Cancels any ongoing load.
     */
    fun refresh() {
        currentJob?.cancel()
        currentJob = scope.launch {
            try {
                isRefreshing.value = true
                getShowsWithNewEpisodes(
                    language,
                    watchProviderIds,
                    watchRegion,
                    firstReleaseYear,
                    originalLanguage
                )
            } finally {
                // ensure progress update even if cancelled
                isRefreshing.value = false
            }
        }
    }

    private suspend fun getShowsWithNewEpisodes(
        languageCode: String,
        watchProviderIds: List<Int>?,
        watchRegion: String?,
        firstReleaseYear: Int?,
        originalLanguage: String?
    ) = withContext(Dispatchers.IO) {
        val tmdb = SgApp.getServicesComponent(context.applicationContext).tmdb()
        val results = TmdbTools2().getShowsWithNewEpisodes(
            tmdb = tmdb,
            language = languageCode,
            page = 1,
            firstReleaseYear = firstReleaseYear,
            originalLanguage = originalLanguage,
            watchProviderIds = watchProviderIds,
            watchRegion = watchRegion
        )?.results

        val result = if (results != null) {
            val searchResults = TmdbSearchResultMapper(context, languageCode)
                .mapToSearchResults(results)
            buildResultSuccess(searchResults, R.string.add_empty)
        } else {
            buildResultFailure()
        }

        // Note: Do not bother posting results if cancelled.
        if (isActive) {
            postValue(result)
        }
    }

    private fun buildResultSuccess(
        results: List<SearchResult>?, @StringRes emptyTextResId: Int
    ): Result {
        return Result(
            results ?: emptyList(),
            context.getString(emptyTextResId),
            true
        )
    }

    private fun buildResultFailure(): Result {
        // only check for network here to allow hitting the response cache
        val emptyText = if (AndroidUtils.isNetworkConnected(context)) {
            context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
        } else {
            context.getString(R.string.offline)
        }
        return Result(emptyList(), emptyText, false)
    }

}
