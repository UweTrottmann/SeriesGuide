// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.shows.search.newepisodes.ShowsNewEpisodesDataSource
import com.battlelancer.seriesguide.shows.search.popular.ShowsPopularDataSource
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.uwetrottmann.tmdb2.Tmdb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Provides a different list of shows ([items]) depending on [DiscoverShowsLink]
 * and some filter options. Or if there is a non-empty [queryString] matching shows.
 */
class ShowsDiscoverPagingViewModel(
    application: Application,
    private val link: DiscoverShowsLink?
) : AndroidViewModel(application) {

    data class Filters(
        val queryString: String,
        val firstReleaseYear: Int?,
        val originalLanguage: String?,
        val watchProviderIds: List<Int>?,
    )

    val queryString = MutableStateFlow("")
    private val watchProviderIds =
        SgRoomDatabase.getInstance(application).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.SHOWS.id)
    val firstReleaseYear = MutableStateFlow<Int?>(null)
    val originalLanguage = MutableStateFlow<String?>(null)
    val filters = combine(
        queryString,
        watchProviderIds,
        firstReleaseYear,
        originalLanguage
    ) { queryString: String, watchProviderIds: List<Int>, firstReleaseYear: Int?, originalLanguage: String? ->
        Filters(queryString, firstReleaseYear, originalLanguage, watchProviderIds)
    }

    private val tmdb = SgApp.getServicesComponent(application).tmdb()

    val items: Flow<PagingData<SearchResult>> = filters
        .flatMapLatest {
            Pager(
                PagingConfig(pageSize = 25, enablePlaceholders = true)
            ) {
                val languageCode = ShowsSettings.getShowsSearchLanguage(application)
                val watchRegion = StreamingSearch.getCurrentRegionOrNull(application)
                if (link == null || it.queryString.isNotEmpty()) {
                    ShowSearchDataSource(
                        getApplication(),
                        tmdb,
                        languageCode = languageCode,
                        query = it.queryString,
                        firstReleaseYear = it.firstReleaseYear
                    )
                } else {
                    buildDiscoverDataSource(
                        tmdb,
                        languageCode,
                        it.firstReleaseYear,
                        it.originalLanguage,
                        it.watchProviderIds,
                        watchRegion
                    )
                }
            }.flow
        }
        .cachedIn(viewModelScope)

    private fun buildDiscoverDataSource(
        tmdb: Tmdb, languageCode: String,
        firstReleaseYear: Int?,
        originalLanguageCode: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): BaseShowResultsDataSource {
        when (link) {
            DiscoverShowsLink.POPULAR -> {
                return ShowsPopularDataSource(
                    getApplication(),
                    tmdb,
                    languageCode,
                    firstReleaseYear,
                    originalLanguageCode,
                    watchProviderIds,
                    watchRegion
                )
            }

            DiscoverShowsLink.NEW_EPISODES -> {
                return ShowsNewEpisodesDataSource(
                    getApplication(),
                    tmdb,
                    languageCode,
                    firstReleaseYear,
                    originalLanguageCode,
                    watchProviderIds,
                    watchRegion
                )
            }

            else -> {
                throw IllegalArgumentException("$link not supported")
            }
        }
    }

    /**
     * This will then load the original list.
     */
    fun removeQuery() {
        queryString.value = ""
    }

    companion object {
        private val KEY_DISCOVER_LINK = object : CreationExtras.Key<Int> {}

        val Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                val link = DiscoverShowsLink.fromId(this[KEY_DISCOVER_LINK]!!)
                ShowsDiscoverPagingViewModel(application, link)
            }
        }

        fun creationExtras(
            defaultExtras: CreationExtras,
            link: DiscoverShowsLink?
        ): CreationExtras =
            MutableCreationExtras(defaultExtras).apply {
                set(KEY_DISCOVER_LINK, link?.id ?: DiscoverShowsLink.NO_LINK_ID)
            }
    }
}