package com.battlelancer.seriesguide.ui.people

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.tmdb2.entities.Credits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PeopleViewModel(
    application: Application,
    private val tmdbId: Int,
    private val mediaType: PeopleActivity.MediaType
) : AndroidViewModel(application) {

    val credits = MutableLiveData<Credits?>()

    init {
        loadCredits()
    }

    fun loadCredits() {
        viewModelScope.launch(Dispatchers.IO) {
            val newCredits = if (mediaType == PeopleActivity.MediaType.MOVIE) {
                TmdbTools2().getCreditsForMovie(getApplication(), tmdbId)
            } else {
                TmdbTools2().getCreditsForShow(getApplication(), tmdbId)
            }
            credits.postValue(newCredits)
        }
    }

}

class PeopleViewModelFactory(
    private val application: Application,
    private val tmdbId: Int,
    private val mediaType: PeopleActivity.MediaType
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PeopleViewModel(application, tmdbId, mediaType) as T
    }

}
