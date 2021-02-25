package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowResult
import kotlinx.coroutines.Dispatchers

class AddShowDialogViewModel(
    application: Application,
    showTmdbId: Int,
    initialLanguageCode: String
) : AndroidViewModel(application) {

    data class ShowDetails(
        val show: SgShow2?,
        val localShowId: Long?,
        val doesNotExist: Boolean,
    )

    val languageCode = MutableLiveData<String>()
    val showDetails: LiveData<ShowDetails>

    init {
        this.languageCode.value = initialLanguageCode
        this.showDetails = Transformations.switchMap(languageCode) { languageCode ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                val showTools = SgApp.getServicesComponent(application).showTools()

                val showDetails = showTools.getShowDetails(showTmdbId, languageCode)
                val localShowIdOrNull =
                    showTools.getShowId(showTmdbId, showDetails.show?.tvdbId)

                emit(
                    ShowDetails(
                        showDetails.show,
                        localShowIdOrNull,
                        showDetails.result == ShowResult.DOES_NOT_EXIST
                    )
                )
            }
        }
    }

}

class AddShowDialogViewModelFactory(
    private val application: Application,
    private val showTmdbId: Int,
    private val initialLanguageCode: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AddShowDialogViewModel(application, showTmdbId, initialLanguageCode) as T
    }

}
