package com.battlelancer.seriesguide.ui.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.Dispatchers

class ShowViewModel(application: Application) : AndroidViewModel(application) {

    private val showId = MutableLiveData<Long>()
    val show by lazy {
        showId.switchMap {
            SgRoomDatabase.getInstance(getApplication()).sgShow2Helper().getShowLiveData(it)
        }
    }
    val credits by lazy {
        showId.switchMap {
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                emit(TmdbTools2().loadCreditsForShow(application, it))
            }
        }
    }

    fun setShowId(showId: Long) {
        this.showId.value = showId
    }

}