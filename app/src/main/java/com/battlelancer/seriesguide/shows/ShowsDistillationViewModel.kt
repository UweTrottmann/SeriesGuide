// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.SgWatchProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
