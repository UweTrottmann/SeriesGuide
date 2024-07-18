// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class EpisodeSearchViewModel(application: Application) : AndroidViewModel(application) {

    data class SearchData(val searchTerm: String?, val showTitle: String?)

    val searchData = MutableLiveData<SearchData>()
    val episodes = searchData.switchMap { searchData ->
        SgRoomDatabase.getInstance(application)
            .sgEpisode2Helper()
            .searchForEpisodes(
                application,
                searchData.searchTerm,
                searchData.showTitle
            )
    }

}