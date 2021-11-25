package com.battlelancer.seriesguide.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.model.SgWatchProvider
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.uwetrottmann.tmdb2.entities.BaseMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class MoviesSearchViewModel(
    application: Application,
    link: MoviesDiscoverLink
) : AndroidViewModel(application) {

    private val watchProviderIds =
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .getEnabledWatchProviderIdsFlow(SgWatchProvider.Type.MOVIES.id)
    private val queryString = MutableLiveData<String>()
    private val tmdb = SgApp.getServicesComponent(application).tmdb()

    val items: Flow<PagingData<BaseMovie>> =
        watchProviderIds.combine(queryString.asFlow()) { ids, query -> Pair(ids, query) }
            .flatMapLatest {
                Pager(
                    // Note: currently TMDB page is 20 items, on phones around 9 are displayed at once.
                    PagingConfig(pageSize = 20, enablePlaceholders = true)
                ) {
                    val languageCode = DisplaySettings.getMoviesLanguage(application)
                    val regionCode = DisplaySettings.getMoviesRegion(application)
                    val watchRegion = StreamingSearch.getCurrentRegionOrNull(application)
                    TmdbMoviesDataSource(
                        application,
                        tmdb,
                        link,
                        it.second,
                        languageCode,
                        regionCode,
                        it.first,
                        watchRegion
                    )
                }.flow
            }.cachedIn(viewModelScope)

    init {
        queryString.value = ""
    }

    fun updateQuery(query: String) {
        queryString.postValue(query)
    }

}

class MoviesSearchViewModelFactory(
    private val application: Application,
    private val link: MoviesDiscoverLink
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MoviesSearchViewModel(application, link) as T
    }

}
