// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class SeasonsViewModel(
    application: Application,
    private val showId: Long
): AndroidViewModel(application) {

    private val order = MutableLiveData<SeasonsSettings.SeasonSorting>()
    val seasons = order.switchMap {
        val helper = SgRoomDatabase.getInstance(application).sgSeason2Helper()
        if (it == SeasonsSettings.SeasonSorting.LATEST_FIRST) {
            helper.getSeasonsOfShowLatestFirst(showId)
        } else {
            helper.getSeasonsOfShowOldestFirst(showId)
        }
    }
    val remainingCountData = RemainingCountLiveData(application, viewModelScope)

    init {
        updateOrder()
    }

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