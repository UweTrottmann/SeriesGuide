package com.battlelancer.seriesguide.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.model.SgMovie
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase

class MoviesWatchedViewModel(application: Application) :
    AndroidViewModel(application) {
    val movieList: LiveData<PagedList<SgMovie>>
    private val queryString = MutableLiveData<String>()

    init {
        updateQueryString()

        movieList = Transformations.switchMap(queryString) { queryString ->
            SgRoomDatabase.getInstance(getApplication())
                .movieHelper()
                .getWatchedMovies(SimpleSQLiteQuery(queryString))
                .toLiveData(pageSize = 50)
        }
    }

    fun updateQueryString() {
        val selection = SeriesGuideContract.Movies.SELECTION_WATCHED
        val order = MoviesDistillationSettings.getSortQuery(getApplication())
        queryString.value =
            "SELECT * FROM ${SeriesGuideDatabase.Tables.MOVIES} WHERE $selection ORDER BY $order"
    }

}
