// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.collection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.BaseMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn

/**
 * Provides a movie collection [Flow] that can be [refresh]ed.
 */
class MovieCollectionViewModel(
    application: Application,
    collectionId: Int
) : AndroidViewModel(application) {

    sealed class MoviesListUiState {
        data class Success(val movies: List<BaseMovie>) : MoviesListUiState()
        data class Error(val message: String) : MoviesListUiState()
    }

    private val tmdb = SgApp.getServicesComponent(application).tmdb()

    private val moviesRefreshTrigger = Channel<Unit>(capacity = Channel.CONFLATED)
    val movies: Flow<MoviesListUiState> = flow {
        for (trigger in moviesRefreshTrigger) {
            val collection = TmdbTools2().getMovieCollection(
                tmdb,
                collectionId,
                MoviesSettings.getMoviesLanguage(application)
            )
            if (collection == null) {
                val message = if (AndroidUtils.isNetworkConnected(application)) {
                    application.getString(
                        R.string.api_error_generic,
                        application.getString(R.string.tmdb)
                    )
                } else {
                    application.getString(R.string.offline)
                }
                emit(MoviesListUiState.Error(message))
            } else {
                val parts = collection.parts
                parts?.sortBy { it.release_date }
                emit(MoviesListUiState.Success(parts ?: emptyList()))
            }
        }
    }.flowOn(Dispatchers.IO)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    init {
        refresh()
    }

    fun refresh() {
        moviesRefreshTrigger.trySend(Unit)
    }

    companion object {
        private val KEY_COLLECTION_ID = object : CreationExtras.Key<Int> {}

        val Factory = viewModelFactory {
            initializer {
                MovieCollectionViewModel(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!,
                    this[KEY_COLLECTION_ID]!!
                )
            }
        }

        fun creationExtras(defaultExtras: CreationExtras, collectionId: Int) =
            MutableCreationExtras(defaultExtras).apply {
                set(KEY_COLLECTION_ID, collectionId)
            }
    }

}
