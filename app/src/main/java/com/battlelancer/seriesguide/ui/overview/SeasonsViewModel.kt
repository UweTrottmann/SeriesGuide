package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.Constants
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings

class SeasonsViewModel(
    application: Application,
    private val showId: Long
): AndroidViewModel(application) {

    private val order = MutableLiveData<Constants.SeasonSorting>()
    val seasons = Transformations.switchMap(order) {
        val helper = SgRoomDatabase.getInstance(application).sgSeason2Helper()
        if (it == Constants.SeasonSorting.LATEST_FIRST) {
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
        order.value = DisplaySettings.getSeasonSortOrder(getApplication())
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