// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.SeasonWatchInfoViewModel

class OverviewViewModel(
    showId: Long,
    application: Application
) : SeasonWatchInfoViewModel(application) {

    val show by lazy {
        SgRoomDatabase.getInstance(application).sgShow2Helper().getShowLiveData(showId)
    }
    private val episodeId = MutableLiveData<Long>()
    val episode by lazy {
        episodeId.switchMap {
            SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper().getEpisodeLiveData(it)
        }
    }

    fun setEpisodeId(episodeId: Long) {
        this.episodeId.value = episodeId
    }

}

class OverviewViewModelFactory(
    val showId: Long,
    val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OverviewViewModel(showId, application) as T
    }
}
