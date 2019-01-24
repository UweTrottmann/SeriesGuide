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

    private fun Boolean?.isTrue(): Boolean {
        return this ?: false
    }

    private fun Boolean?.isFalse(): Boolean {
        if (this == null) return false
        return !this
    }

    private fun Boolean?.isNullOrFalse(): Boolean {
        if (this == null) return true
        return !this
    }

    fun updateQuery(
        filter: FilterShowsView.ShowFilter,
        orderClause: String
    ) {
        val selection = StringBuilder()

        // include or exclude favorites?
        filter.isFilterFavorites?.let {
            if (it) {
                selection.append(SeriesGuideContract.Shows.SELECTION_FAVORITES)
            } else {
                selection.append(SeriesGuideContract.Shows.SELECTION_NOT_FAVORITES)
            }
        }

        // include or exclude hidden?
        filter.isFilterHidden?.let {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            if (it) {
                selection.append(SeriesGuideContract.Shows.SELECTION_HIDDEN)
            } else {
                selection.append(SeriesGuideContract.Shows.SELECTION_NO_HIDDEN)
            }
        }

        // unwatched (= next episode is released) and upcoming (= next episode upcoming) filters
        // assumes that no next episode == max next airdate

        val timeInAnHour = TimeTools.getCurrentTime(getApplication()) + DateUtils.HOUR_IN_MILLIS
        // next episode upcoming within <limit> days + 1 hour
        val upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(getApplication())
        val maxAirtime = timeInAnHour + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS

        if (filter.isFilterUnwatched != null || filter.isFilterUpcoming != null) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
        }

        if (filter.isFilterUnwatched.isTrue() && filter.isFilterUpcoming.isTrue()) {
            // unwatched and upcoming
            selection.append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                .append(maxAirtime)
        } else if (
            filter.isFilterUnwatched.isTrue() && filter.isFilterUpcoming.isNullOrFalse()
        ) {
            // unwatched only
            selection.append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                .append(timeInAnHour)
        } else if (
            filter.isFilterUpcoming.isTrue() && filter.isFilterUnwatched.isNullOrFalse()
        ) {
            // upcoming only
            selection
                .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append(">")
                .append(timeInAnHour)
                .append(" AND ")
                .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                .append(maxAirtime)
        } else if (filter.isFilterUnwatched.isFalse()) {
            if (filter.isFilterUpcoming == null) {
                // unwatched excluded (== anything in the future or no next episode)
                selection
                    .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append(">")
                    .append(timeInAnHour)
            } else if (!filter.isFilterUpcoming) {
                // unwatched and upcoming excluded (== further into the future or no next episode)
                selection
                    .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append(">")
                    .append(maxAirtime)
            }
        } else if (filter.isFilterUpcoming.isFalse() && filter.isFilterUnwatched == null) {
            // upcoming excluded
            selection
                .append("(")
                .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                .append(timeInAnHour)
                .append(" OR ")
                .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append(">")
                .append(maxAirtime)
                .append(")")
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