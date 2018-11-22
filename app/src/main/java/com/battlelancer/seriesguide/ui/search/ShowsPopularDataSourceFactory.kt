package com.battlelancer.seriesguide.ui.search

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.uwetrottmann.trakt5.services.Shows

class ShowsPopularDataSourceFactory(
        val context: Context,
        val traktShows: Shows
) : DataSource.Factory<Int, SearchResult>() {

    val dataSourceLiveData = MutableLiveData<ShowsPopularDataSource>()

    override fun create(): DataSource<Int, SearchResult> {
        val source = ShowsPopularDataSource(context, traktShows)
        dataSourceLiveData.postValue(source)
        return source
    }

}