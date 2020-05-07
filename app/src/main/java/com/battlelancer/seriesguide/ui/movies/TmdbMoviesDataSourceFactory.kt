package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import com.uwetrottmann.tmdb2.entities.BaseMovie

class TmdbMoviesDataSourceFactory(
    private val context: Context,
    private val link: MoviesDiscoverLink
) : DataSource.Factory<Int, BaseMovie>() {

    val sourceLiveData = MutableLiveData<TmdbMoviesDataSource>()
    private var query: String? = null

    override fun create(): DataSource<Int, BaseMovie> {
        val source = TmdbMoviesDataSource(
            context, link, query
        )
        sourceLiveData.postValue(source)
        return source
    }

    fun changeQueryThenInvalidateSource(query: String?) {
        this.query = query
        sourceLiveData.value?.invalidate()
    }

}