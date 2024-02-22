// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import com.battlelancer.seriesguide.streaming.StreamingSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShowsDistillationViewModel(application: Application) : AndroidViewModel(application) {

    val watchProvidersFlow: StateFlow<List<SgWatchProvider>> =
        SgRoomDatabase.getInstance(application).sgWatchProviderHelper()
            .usedWatchProvidersFlow(SgWatchProvider.Type.SHOWS.id)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = listOf()
            )

    val watchProviderRegionFlow: StateFlow<String> = StreamingSearch.regionLiveData.asFlow()
        .map {
            // Simpler to just use the existing API that reads from SharedPreferences
            StreamingSearch.getCurrentRegionOrSelectString(application)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = StreamingSearch.getCurrentRegionOrSelectString(application)
        )

    fun changeWatchProviderFilter(watchProvider: SgWatchProvider, filter: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
                .setFilterLocal(watchProvider._id, filter)
        }
    }

    fun removeWatchProviderFilter() {
        viewModelScope.launch(Dispatchers.IO) {
            SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
                .setFilterLocalFalseAll(SgWatchProvider.Type.SHOWS.id)
        }
    }

}
