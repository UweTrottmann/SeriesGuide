package com.battlelancer.seriesguide.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.Dispatchers

class MovieDetailsModel(
    movieTmdbId: Int,
    application: Application
) : AndroidViewModel(application) {

    init {
        // Set original value for region.
        StreamingSearch.initRegionLiveData(application)
    }

    private val watchInfoMediator = MediatorLiveData<StreamingSearch.WatchInfo>().apply {
        addSource(StreamingSearch.regionLiveData) {
            value = StreamingSearch.WatchInfo(movieTmdbId, it)
        }
    }
    val watchProvider by lazy {
        StreamingSearch.getWatchProviderLiveData(
            watchInfoMediator,
            viewModelScope.coroutineContext,
            getApplication(),
            isMovie = true
        )
    }

    val credits = liveData(context = viewModelScope.coroutineContext + Dispatchers.IO) {
        emit(TmdbTools2().getCreditsForMovie(application, movieTmdbId))
    }

}

class MovieDetailsModelFactory(
    private val movieTmdbId: Int,
    private val application: Application
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MovieDetailsModel(movieTmdbId, application) as T
    }
}
