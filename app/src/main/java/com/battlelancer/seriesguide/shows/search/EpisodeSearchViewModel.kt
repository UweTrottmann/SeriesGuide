// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase

class EpisodeSearchViewModel(application: Application) : AndroidViewModel(application) {

    data class SearchData(val searchTerm: String?, val showTitle: String?)

    val searchData = MutableLiveData<SearchData>()
    val episodes = searchData.switchMap { searchData ->
        SeriesGuideDatabase.searchForEpisodes(
            application,
            searchData.searchTerm,
            searchData.showTitle
        )
    }

}