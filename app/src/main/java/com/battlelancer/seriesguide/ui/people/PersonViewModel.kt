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
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.tmdb2.entities.Person
import kotlinx.coroutines.Dispatchers
import retrofit2.Response

class PersonViewModel(
    application: Application,
    private val personTmdbId: Int
) : AndroidViewModel(application) {

    val languageCode = MutableLiveData<String>()
    val personLiveData = languageCode.switchMap {
        liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
            val peopleService = SgApp.getServicesComponent(getApplication()).peopleService()
            val response: Response<Person?>
            try {
                response = peopleService.summary(personTmdbId, it).execute()
                if (response.isSuccessful) {
                    emit(response.body())
                    return@liveData
                } else {
                    Errors.logAndReport("get person summary", response)
                }
            } catch (e: Exception) {
                Errors.logAndReport("get person summary", e)
            }
            emit(null)
        }
    }

}

class PersonViewModelFactory(
    private val application: Application,
    private val personTmdbId: Int
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return PersonViewModel(application, personTmdbId) as T
    }
}
