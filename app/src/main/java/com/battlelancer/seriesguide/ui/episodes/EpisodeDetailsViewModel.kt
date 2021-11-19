package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.StreamingSearch

class EpisodeDetailsViewModel(
    episodeId: Long,
    application: Application
) : AndroidViewModel(application) {

    init {
        // Set original value for region.
        StreamingSearch.initRegionLiveData(application)
    }

    val episode = SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
        .getEpisodeLiveData(episodeId)
    val showId = MutableLiveData<Long>()
    val show = Transformations.switchMap(showId) {
        SgRoomDatabase.getInstance(getApplication()).sgShow2Helper()
            .getShowLiveData(it)
    }

    private val showTmdbId = MutableLiveData<Int>()
    private val watchInfoMediator = StreamingSearch.getWatchInfoMediator(showTmdbId)
    val watchProvider by lazy {
        StreamingSearch.getWatchProviderLiveData(
            watchInfoMediator,
            viewModelScope.coroutineContext,
            getApplication()
        )
    }

    fun setShowTmdbId(showTmdbId: Int?) {
        if (showTmdbId != null) {
            this.showTmdbId.value = showTmdbId
        }
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
