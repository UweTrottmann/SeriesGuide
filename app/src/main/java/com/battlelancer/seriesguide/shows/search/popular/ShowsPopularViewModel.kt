// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search.popular

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.shows.search.discover.SearchResult
import com.battlelancer.seriesguide.streaming.StreamingSearch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class ShowsPopularViewModel(application: Application) : AndroidViewModel(application) {

    private val watchProviderIds =
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.SHOWS.id)

    val items: Flow<PagingData<SearchResult>>

    init {
        val tmdb = SgApp.getServicesComponent(application).tmdb()
        items = watchProviderIds.flatMapLatest {
            Pager(
                PagingConfig(pageSize = 25, enablePlaceholders = true)
            ) {
                val languageCode = ShowsSettings.getShowsSearchLanguage(application)
                val watchRegion = StreamingSearch.getCurrentRegionOrNull(application)
                ShowsPopularDataSource(application, tmdb, languageCode, it, watchRegion)
            }.flow
        }.cachedIn(viewModelScope)
    }

}