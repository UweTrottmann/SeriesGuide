package com.battlelancer.seriesguide.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.battlelancer.seriesguide.ui.search.NetworkState
import com.uwetrottmann.tmdb2.entities.BaseMovie

class MoviesSearchViewModel(
    application: Application,
    link: MoviesDiscoverLink
) : AndroidViewModel(application) {

    private val factory = TmdbMoviesDataSourceFactory(application, link)
    val pagedMovieList: LiveData<PagedList<BaseMovie>>
    val networkState: LiveData<NetworkState>

    init {
        networkState = Transformations.switchMap(factory.sourceLiveData) {
            it.networkState
        }

        // Note: currently TMDB page is 20 items, on phones around 9 are displayed at once.
        pagedMovieList = LivePagedListBuilder(
            factory,
            PagedList.Config.Builder()
                .setPageSize(20)
                .setEnablePlaceholders(true)
                .build()
        ).build()
    }

    fun updateQuery(query: String?) {
        factory.changeQueryThenInvalidateSource(query)
    }

}

class MoviesSearchViewModelFactory(
    private val application: Application,
    private val link: MoviesDiscoverLink
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MoviesSearchViewModel(application, link) as T
    }

}
