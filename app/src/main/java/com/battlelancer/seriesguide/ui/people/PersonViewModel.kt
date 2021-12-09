package com.battlelancer.seriesguide.ui.people

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.Dispatchers

class PersonViewModel(
    application: Application,
    private val personTmdbId: Int
) : AndroidViewModel(application) {

    val languageCode = MutableLiveData<String>()
    val personLiveData = languageCode.switchMap {
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val peopleService = SgApp.getServicesComponent(getApplication()).peopleService()!!
            val personOrNull = TmdbTools2().getPerson(peopleService, personTmdbId, it)
            emit(personOrNull)
        }
    }

}

class PersonViewModelFactory(
    private val application: Application,
    private val personTmdbId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PersonViewModel(application, personTmdbId) as T
    }
}
