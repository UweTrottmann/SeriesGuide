// SPDX-License-Identifier: Apache-2.0
// Copyright 2018, 2020-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class SeasonsViewModel(
    application: Application,
    private val showId: Long
) : AndroidViewModel(application) {

    private val order = MutableStateFlow(SeasonsSettings.getSeasonSortOrder(getApplication()))

    val seasonsWithStats = order
        .flatMapLatest {
            val helper = SgRoomDatabase.getInstance(application).sgSeason2Helper()
            if (it == SeasonsSettings.SeasonSorting.LATEST_FIRST) {
                helper.getSeasonsOfShowLatestFirst(showId)
            } else {
                helper.getSeasonsOfShowOldestFirst(showId)
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = listOf()
        )

    val remainingCountData = RemainingCountLiveData(application, viewModelScope)

    fun updateOrder() {
        order.value = SeasonsSettings.getSeasonSortOrder(getApplication())
    }

}

class SeasonsViewModelFactory(
    private val application: Application,
    private val showId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SeasonsViewModel(application, showId) as T
    }
}