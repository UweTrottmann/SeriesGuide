package com.battlelancer.seriesguide.ui.episodes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.Constants.EpisodeSorting
import com.battlelancer.seriesguide.provider.SgEpisode2Info
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EpisodesViewModel(
    application: Application,
    private val seasonId: Long
) : AndroidViewModel(application) {

    data class Counts(
        val unwatchedEpisodes: Int,
        val uncollectedEpisodes: Int
    )

    var showId: Long = 0
    private val order = MutableLiveData<EpisodeSorting>()
    val episodes = Transformations.switchMap(order) {
        SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
            .getEpisodeInfoOfSeasonLiveData(SgEpisode2Info.buildQuery(seasonId, it))
    }
    val episodeCounts = MutableLiveData<Counts>()
    var selectedItemId: Long = -1

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val db = SgRoomDatabase.getInstance(getApplication())
            db.sgSeason2Helper().getSeasonNumbers(seasonId)?.also {
                showId = it.showId
            }
        }
        updateOrder()
    }

    fun updateOrder() {
        order.value = DisplaySettings.getEpisodeSortOrder(getApplication())
    }

    fun updateCounts() = viewModelScope.launch(Dispatchers.IO) {
        val helper = SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
        val unwatchedEpisodes = helper.countNotWatchedReleasedEpisodesOfSeason(
            seasonId,
            TimeTools.getCurrentTime(getApplication())
        )
        val uncollectedEpisodes = helper.countNotCollectedEpisodesOfSeason(seasonId)
        episodeCounts.postValue(
            Counts(unwatchedEpisodes, uncollectedEpisodes)
        )
    }
}

class EpisodesViewModelFactory(
    private val application: Application,
    private val seasonId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EpisodesViewModel(application, seasonId) as T
    }

}
