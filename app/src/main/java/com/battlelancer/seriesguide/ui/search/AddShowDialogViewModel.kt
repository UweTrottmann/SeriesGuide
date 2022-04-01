package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.ui.shows.ShowTools2
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
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
        // Set original value for region.
        StreamingSearch.initRegionLiveData(application)

        this.languageCode.value = initialLanguageCode
        this.showDetails = Transformations.switchMap(languageCode) { languageCode ->
            liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
                val showTools = SgApp.getServicesComponent(application).showTools()
                showTools.getShowDetails(showTmdbId, languageCode)
                    .onFailure {
                        emit(
                            ShowDetails(
                                null,
                                showTools.getShowId(showTmdbId, null),
                                it == ShowTools2.GetShowDoesNotExist
                            )
                        )
                    }
                    .onSuccess {
                        emit(
                            ShowDetails(
                                it.show,
                                showTools.getShowId(showTmdbId, it.show?.tvdbId),
                                false
                            )
                        )
                    }
            }
        }
    }

    private val watchInfoMediator = MediatorLiveData<StreamingSearch.WatchInfo>().apply {
        addSource(StreamingSearch.regionLiveData) {
            value = StreamingSearch.WatchInfo(showTmdbId, it)
        }
    }
    val watchProvider by lazy {
        StreamingSearch.getWatchProviderLiveData(
            watchInfoMediator,
            viewModelScope.coroutineContext,
            getApplication()
        )
    }

}

class AddShowDialogViewModelFactory(
    private val application: Application,
    private val showTmdbId: Int,
    private val initialLanguageCode: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddShowDialogViewModel(application, showTmdbId, initialLanguageCode) as T
    }

}
