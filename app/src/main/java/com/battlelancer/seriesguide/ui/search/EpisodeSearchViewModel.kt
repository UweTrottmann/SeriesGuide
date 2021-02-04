package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase

class EpisodeSearchViewModel(application: Application) : AndroidViewModel(application) {

    data class SearchData(val searchTerm: String?, val showTitle: String?)

    val searchData = MutableLiveData<SearchData>()
    val episodes = Transformations.switchMap(searchData) { searchData ->
        SeriesGuideDatabase.searchForEpisodes(
            application,
            searchData.searchTerm,
            searchData.showTitle
        )
    }

}