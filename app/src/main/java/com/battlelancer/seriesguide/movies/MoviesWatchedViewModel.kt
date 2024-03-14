// SPDX-License-Identifier: Apache-2.0
// Copyright 2019, 2021-2024 Uwe Trottmann

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
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

open class MoviesWatchedViewModel(application: Application) :
    AndroidViewModel(application) {

    private val queryString = MutableLiveData<String>()
    val items: Flow<PagingData<SgMovie>> = queryString.asFlow().flatMapLatest {
        Pager(
            PagingConfig(pageSize = 50)
        ) {
            SgRoomDatabase.getInstance(getApplication())
                .movieHelper()
                .getMovies(SimpleSQLiteQuery(it))
        }.flow
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
