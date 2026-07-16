// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MovieDetailsModel(
    private val movieTmdbId: Int,
    application: Application
) : AndroidViewModel(application) {

    val trailerVideoId: MutableStateFlow<String?> = MutableStateFlow(null)

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

    init {
        // Set original value for region.
        StreamingSearch.initRegionLiveData(application)

        loadTrailerVideoId()
    }

    fun loadTrailerVideoId() {
        viewModelScope.launch(Dispatchers.Default) {
            // Try to get cached value from database before doing a network request
            val movieDbHelper = SgRoomDatabase.getInstance(getApplication()).movieHelper()
            movieDbHelper.getMovieTrailer(movieTmdbId)
                ?.also { trailerVideoId.value = it }

            TmdbTools2().getMovieTrailerYoutubeId(getApplication(), movieTmdbId)
                ?.also {
                    movieDbHelper.updateMovieTrailer(movieTmdbId, it)
                    trailerVideoId.value = it
                }
        }
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
