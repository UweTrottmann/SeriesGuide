package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class OverviewViewModel(application: Application) : AndroidViewModel(application) {

    private val showId = MutableLiveData<Long>()
    val show = Transformations.switchMap(showId) {
        SgRoomDatabase.getInstance(application).sgShow2Helper().getShowLiveData(it)
    }

    fun setShowId(showId: Long) {
        this.showId.value = showId
    }

}