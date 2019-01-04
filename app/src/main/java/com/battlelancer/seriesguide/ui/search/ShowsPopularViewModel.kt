package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.battlelancer.seriesguide.SgApp

class ShowsPopularViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSourceLiveData: LiveData<ShowsPopularDataSource>
    val items: LiveData<PagedList<SearchResult>>
    val networkState: LiveData<NetworkState>

    init {
        val traktShows = SgApp.getServicesComponent(application).trakt().shows()
        val sourceFactory = ShowsPopularDataSourceFactory(application, traktShows)

        dataSourceLiveData = sourceFactory.dataSourceLiveData

        networkState = Transformations.switchMap(sourceFactory.dataSourceLiveData) {
            it.networkState
        }

        items = LivePagedListBuilder(sourceFactory,
                PagedList.Config.Builder()
                        .setPageSize(25)
                        .setEnablePlaceholders(true)
                        .build())
                .build()
    }

    fun refresh() {
        dataSourceLiveData.value?.invalidate()
    }

}