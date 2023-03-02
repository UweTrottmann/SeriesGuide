package com.battlelancer.seriesguide.shows.overview

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

    /**
     * This currently does not auto-update, it maybe should at some point (add global LiveData).
     */
    val hasAccessToX = MutableLiveData<Boolean>()

    init {
        updateUserStatus()
    }

    fun setShowId(showId: Long) {
        this.showId.value = showId
    }

    fun updateUserStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val currentState = hasAccessToX.value
            val newState = Utils.hasAccessToX(getApplication())
            if (currentState != newState) {
                hasAccessToX.postValue(newState)
            }
        }
    }

}