package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.SgApp
import kotlinx.coroutines.flow.Flow

class ShowsPopularViewModel(application: Application) : AndroidViewModel(application) {

    val items: Flow<PagingData<SearchResult>>

    init {
        val tmdb = SgApp.getServicesComponent(application).tmdb()
        items = Pager(
            PagingConfig(pageSize = 25, enablePlaceholders = true)
        ) {
            ShowsPopularDataSource(application, tmdb)
        }.flow
            .cachedIn(viewModelScope)
    }

}