package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EpisodesViewModel(
    application: Application,
    seasonId: Long
) : AndroidViewModel(application) {

    val episodeCountLiveData = EpisodeCountLiveData(application)
    var seasonTvdbId: Int = 0
    var seasonNumber: Int = 0
    var showTvdbId: Int = 0

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val db = SgRoomDatabase.getInstance(getApplication())
            db.sgSeason2Helper().getSeasonNumbers(seasonId)?.also {
                seasonTvdbId = it.tvdbId ?: 0
                seasonNumber = it.number
                showTvdbId = db.sgShow2Helper().getShowTvdbId(it.showId)
            }
        }
    }

}

class EpisodesViewModelFactory(
    private val application: Application,
    private val seasonId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return EpisodesViewModel(application, seasonId) as T
    }

}
