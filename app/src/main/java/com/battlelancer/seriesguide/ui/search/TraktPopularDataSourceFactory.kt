package com.battlelancer.seriesguide.ui.search

import android.arch.lifecycle.MutableLiveData
import android.arch.paging.DataSource
import android.content.Context
import com.uwetrottmann.trakt5.services.Shows

class TraktPopularDataSourceFactory(
        val context: Context,
        val traktShows: Shows
) : DataSource.Factory<Int, SearchResult>() {

    val dataSourceLiveData = MutableLiveData<TraktPopularDataSource>()

    override fun create(): DataSource<Int, SearchResult> {
        val source = TraktPopularDataSource(context, traktShows)
        dataSourceLiveData.postValue(source)
        return source
    }

}