package com.battlelancer.seriesguide.ui.search

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.uwetrottmann.tmdb2.Tmdb

class ShowsPopularDataSourceFactory(
        private val context: Context,
        private val tmdb: Tmdb
) : DataSource.Factory<Int, SearchResult>() {

    val dataSourceLiveData = MutableLiveData<ShowsPopularDataSource>()

    override fun create(): DataSource<Int, SearchResult> {
        val source = ShowsPopularDataSource(context, tmdb)
        dataSourceLiveData.postValue(source)
        return source
    }

}