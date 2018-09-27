package com.battlelancer.seriesguide.ui.search

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import com.battlelancer.seriesguide.SgApp

class ShowsPopularViewModel(application: Application) : AndroidViewModel(application) {

    private val dataSourceLiveData: LiveData<TraktPopularDataSource>
    val items: LiveData<PagedList<SearchResult>>
    val networkState: LiveData<NetworkState>

    init {
        val traktShows = SgApp.getServicesComponent(application).trakt().shows()
        val sourceFactory = TraktPopularDataSourceFactory(application, traktShows)

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