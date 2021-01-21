package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.DBUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val showId = MutableLiveData<Long>()
    val show by lazy {
        Transformations.switchMap(showId) {
            SgRoomDatabase.getInstance(application).sgShow2Helper().getShowLiveData(it)
        }
    }
    private val episodeTvdbId = MutableLiveData<Int>()
    val episode by lazy {
        Transformations.switchMap(episodeTvdbId) {
            SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper().getEpisodeLiveData(it)
        }
    }

    fun setShowId(showId: Long, showTvdbId: Int) {
        this.showId.value = showId

        viewModelScope.launch(Dispatchers.IO) {
            val tvdbId = DBUtils.updateLatestEpisode(getApplication(), showTvdbId)
            episodeTvdbId.postValue(tvdbId)
        }
    }

}