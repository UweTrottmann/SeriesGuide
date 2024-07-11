// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.streaming.StreamingSearch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber

class ShowsDiscoverViewModel(application: Application) : AndroidViewModel(application) {

    val isRefreshing = MutableStateFlow(false)

    // Replay so combine gets an initial value on subscribing
    private val refresh = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val watchProviderIds =
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.SHOWS.id)

    val data =
        combine(
            refresh, watchProviderIds
        ) { _: Unit, watchProviderIds: List<Int> ->
            watchProviderIds
        }.flatMapLatest {
            val languageCode = ShowsSettings.getShowsSearchLanguage(getApplication())
            val watchRegion = StreamingSearch.getCurrentRegionOrNull(getApplication())
            ShowsDiscoverLiveData(
                application,
                viewModelScope,
                isRefreshing,
                languageCode,
                it,
                watchRegion,
                null,
                null
            ).asFlow()
        }.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    init {
        refresh.tryEmit(Unit)
    }

    /**
     * Returns whether new data will be loaded.
     */
    fun refreshData() {
        viewModelScope.launch {
            refresh.emit(Unit)
        }
    }

    fun changeResultsLanguage(languageCode: String) {
        ShowsSettings.saveShowsSearchLanguage(getApplication(), languageCode)
        Timber.d("Set search language to %s", languageCode)
        refreshData()
    }
}