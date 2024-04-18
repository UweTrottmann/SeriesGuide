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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
abstract class BaseDiscoverShowsViewModel(application: Application) :
    AndroidViewModel(application) {

    data class Filters(
        val firstReleaseYear: Int?,
        val originalLanguage: String?,
        val watchProviderIds: List<Int>?,
    )

    val filters = MutableStateFlow(Filters(null, null, null))

    private val watchProviderIds =
        SgRoomDatabase.getInstance(application).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.SHOWS.id)

    private val firstReleaseYear = MutableStateFlow<Int?>(null)
    private val originalLanguage = MutableStateFlow<String?>(null)

    val items: Flow<PagingData<SearchResult>>

    init {
        val tmdb = SgApp.getServicesComponent(application).tmdb()
        items = filters
            .debounce(200) // below 300ms to not be perceived as lag
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

        // Is there a better way? map + merge won't help.
        viewModelScope.launch {
            firstReleaseYear.collect {
                filters.value = filters.value.copy(firstReleaseYear = it)
            }
        }
        viewModelScope.launch {
            originalLanguage.collect {
                filters.value = filters.value.copy(originalLanguage = it)
            }
        }
        viewModelScope.launch {
            watchProviderIds.collect {
                filters.value = filters.value.copy(watchProviderIds = it)
            }
        }
    }

    abstract fun buildDataSource(
        tmdb: Tmdb, languageCode: String,
        firstReleaseYear: Int?,
        originalLanguageCode: String?,
        watchProviderIds: List<Int>?,
        watchRegion: String?
    ): BaseDiscoverShowDataSource

    fun updateYear(year: Int?) {
        firstReleaseYear.value = year
    }

    fun updateLanguage(languageCode: String?) {
        originalLanguage.value = languageCode
    }

}