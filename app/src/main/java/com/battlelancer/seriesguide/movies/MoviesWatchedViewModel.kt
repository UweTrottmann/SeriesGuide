// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

open class MoviesWatchedViewModel(application: Application) :
    AndroidViewModel(application) {

    private val uiMovieBuilder = UiMovieBuilder(application)

    private val queryString = MutableLiveData<String>()
    val items: Flow<PagingData<UiMovie>> = queryString.asFlow().flatMapLatest {
        Pager(
            PagingConfig(pageSize = 50)
        ) {
            SgRoomDatabase.getInstance(getApplication())
                .movieHelper()
                .getMovies(SimpleSQLiteQuery(it))
        }.flow.map { pagingData ->
            pagingData.map { sgMovie -> uiMovieBuilder.buildFrom(sgMovie) }
        }
    }.cachedIn(viewModelScope)

    init {
        updateQueryString()
    }

    open val selection: String
        get() = SeriesGuideContract.Movies.SELECTION_WATCHED

    fun updateQueryString() {
        val order = MoviesDistillationSettings.getSortQuery(getApplication())
        queryString.value =
            "SELECT * FROM ${SeriesGuideDatabase.Tables.MOVIES} WHERE $selection ORDER BY $order"
    }

}
