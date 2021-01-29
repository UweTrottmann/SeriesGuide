package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class OverviewViewModel(
    showId: Long,
    application: Application
) : AndroidViewModel(application) {

    val show by lazy {
        SgRoomDatabase.getInstance(application).sgShow2Helper().getShowLiveData(showId)
    }
    private val episodeId = MutableLiveData<Long>()
    val episode by lazy {
        Transformations.switchMap(episodeId) {
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
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return OverviewViewModel(showId, application) as T
    }
}
