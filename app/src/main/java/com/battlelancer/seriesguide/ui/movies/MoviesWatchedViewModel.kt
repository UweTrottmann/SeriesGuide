package com.battlelancer.seriesguide.ui.movies

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
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

    init {
        val selection = SeriesGuideContract.Movies.SELECTION_WATCHED
        val order = MoviesDistillationSettings.getSortQuery(getApplication())
        val queryString =
            "SELECT * FROM ${SeriesGuideDatabase.Tables.MOVIES} WHERE $selection ORDER BY $order"
        val query = SimpleSQLiteQuery(queryString)

        movieList =  SgRoomDatabase.getInstance(getApplication()).movieHelper()
            .getWatchedMovies(query).toLiveData(pageSize = 50)
    }

}
