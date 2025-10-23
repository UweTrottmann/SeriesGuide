// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.switchMap
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class EpisodeDetailsViewModel(
    episodeId: Long,
    application: Application
) : AndroidViewModel(application) {

    val episode = SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
        .getEpisodeLiveData(episodeId)
    val showId = MutableLiveData<Long>()
    val show = showId.switchMap {
        SgRoomDatabase.getInstance(getApplication()).sgShow2Helper()
            .getShowLiveData(it)
    }

}

class EpisodeDetailsViewModelFactory(
    private val episodeId: Long,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EpisodeDetailsViewModel(episodeId, application) as T
    }
}
