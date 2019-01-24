package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.os.AsyncTask
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.util.TimeTools

class ShowsViewModel(application: Application) : AndroidViewModel(application) {

    private val queryString = MutableLiveData<String>()
    private val sgShowsLiveData: LiveData<List<SgShow>>
    val showItemsLiveData = MediatorLiveData<List<ShowsAdapter.ShowItem>>()

    init {
        sgShowsLiveData = Transformations.switchMap(queryString) { queryString ->
            SgRoomDatabase.getInstance(getApplication()).showHelper()
                .queryShows(SimpleSQLiteQuery(queryString, null))
        }

        showItemsLiveData.addSource(sgShowsLiveData) { sgShows ->
            // calculate actually displayed values on a background thread
            AsyncTask.THREAD_POOL_EXECUTOR.execute {
                val mapped = sgShows?.map {
                    ShowsAdapter.ShowItem.map(it, getApplication())
                }
                showItemsLiveData.postValue(mapped)
            }
        }
    }

    fun updateQuery(
        showFilter: FilterShowsView.ShowFilter,
        orderClause: String
    ) {
        val selection = StringBuilder()

        // include or exclude favorites?
        showFilter.isFilterFavorites?.let {
            if (it) {
                selection.append(SeriesGuideContract.Shows.SELECTION_FAVORITES)
            } else {
                selection.append(SeriesGuideContract.Shows.SELECTION_NOT_FAVORITES)
            }
        }

        val timeInAnHour = TimeTools.getCurrentTime(getApplication()) + DateUtils.HOUR_IN_MILLIS

        // restrict to shows with a next episode?
        if (showFilter.isFilterUnwatched != null && showFilter.isFilterUnwatched) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            selection.append(SeriesGuideContract.Shows.SELECTION_WITH_RELEASED_NEXT_EPISODE)

            // exclude shows with upcoming next episode
            if (showFilter.isFilterUpcoming == null) {
                selection.append(" AND ")
                    .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                    .append(timeInAnHour)
            }
        }
        // restrict to shows with an upcoming (yet to air) next episode?
        if (showFilter.isFilterUpcoming != null && showFilter.isFilterUpcoming) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            // Display shows upcoming within <limit> days + 1 hour
            val upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(getApplication())
            val latestAirtime = timeInAnHour + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS

            selection.append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                .append(latestAirtime)

            // exclude shows with no upcoming next episode if not filtered for unwatched, too
            if (showFilter.isFilterUnwatched == null) {
                selection.append(" AND ")
                    .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append(">=")
                    .append(timeInAnHour)
            }
        }

        // include or exclude hidden?
        showFilter.isFilterHidden?.let {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            if (it) {
                selection.append(SeriesGuideContract.Shows.SELECTION_HIDDEN)
            } else {
                selection.append(SeriesGuideContract.Shows.SELECTION_NO_HIDDEN)
            }
        }

        queryString.value = if (selection.isNotEmpty()) {
            "SELECT * FROM ${SeriesGuideDatabase.Tables.SHOWS} WHERE $selection ORDER BY $orderClause"
        } else {
            "SELECT * FROM ${SeriesGuideDatabase.Tables.SHOWS} ORDER BY $orderClause"
        }
    }

    fun reRunQuery() {
        queryString.value = queryString.value
    }

}