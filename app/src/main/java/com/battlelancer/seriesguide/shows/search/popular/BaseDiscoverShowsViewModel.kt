// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.uwetrottmann.tmdb2.Tmdb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

abstract class BaseDiscoverShowsViewModel(application: Application) :
    AndroidViewModel(application) {

    data class Filters(
        val firstReleaseYear: Int?,
        val originalLanguage: String?,
        val watchProviderIds: List<Int>?,
    )

    private val watchProviderIds =
        SgRoomDatabase.getInstance(application).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.SHOWS.id)
    val firstReleaseYear = MutableStateFlow<Int?>(null)
    val originalLanguage = MutableStateFlow<String?>(null)
    val filters = combine(
        watchProviderIds,
        firstReleaseYear,
        originalLanguage
    ) { watchProviderIds: List<Int>, firstReleaseYear: Int?, originalLanguage: String? ->
        Filters(firstReleaseYear, originalLanguage, watchProviderIds)
    }

    private val tmdb = SgApp.getServicesComponent(application).tmdb()

    val items: Flow<PagingData<SearchResult>> = filters
        .flatMapLatest {
            Pager(
                PagingConfig(pageSize = 25, enablePlaceholders = true)
            ) {
                val languageCode = ShowsSettings.getShowsSearchLanguage(application)
                val watchRegion = StreamingSearch.getCurrentRegionOrNull(application)
                buildDataSource(
                    tmdb,
                    languageCode,
                    it.firstReleaseYear,
                    it.originalLanguage,
                    it.watchProviderIds,
                    watchRegion
                )
            }.flow
        }
        .cachedIn(viewModelScope)

    abstract fun buildDataSource(
        tmdb: Tmdb, languageCode: String,
        firstReleaseYear: Int?,
        originalLanguageCode: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): BaseDiscoverShowDataSource

}