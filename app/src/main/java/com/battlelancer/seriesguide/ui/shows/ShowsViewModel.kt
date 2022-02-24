package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgShow2ForLists
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class ShowsViewModel(application: Application) : AndroidViewModel(application) {

    private val queryString = MutableLiveData<String>()
    private val sgShowsLiveData: LiveData<MutableList<SgShow2ForLists>> =
        Transformations.switchMap(queryString) { queryString ->
            SgRoomDatabase.getInstance(getApplication()).sgShow2Helper()
                .getShowsLiveData(SimpleSQLiteQuery(queryString, null))
        }
    val showItemsLiveData = MediatorLiveData<MutableList<ShowsAdapter.ShowItem>?>()
    private val showItemsLiveDataSemaphore = Semaphore(1)

    init {
        showItemsLiveData.addSource(sgShowsLiveData) { sgShows ->
            // calculate actually displayed values on a background thread
            viewModelScope.launch(Dispatchers.IO) {
                // Use Semaphore with 1 permit to ensure results are delivered in order and never
                // processed in parallel.
                showItemsLiveDataSemaphore.withPermit {
                    val mapped = sgShows?.mapTo(ArrayList(sgShows.size)) {
                        ShowsAdapter.ShowItem.map(it, getApplication())
                    }
                    showItemsLiveData.postValue(mapped)
                }
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
                selection.append(SgShow2Columns.SELECTION_FAVORITES)
            } else {
                selection.append(SgShow2Columns.SELECTION_NOT_FAVORITES)
            }
        }

        // include or exclude continuing/upcoming/pilot/in production shows?
        filter.isFilterContinuing?.let {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            if (it) {
                selection.append(SgShow2Columns.SELECTION_STATUS_CONTINUING)
            } else {
                selection.append(SgShow2Columns.SELECTION_STATUS_NO_CONTINUING)
            }
        }

        // include or exclude hidden?
        filter.isFilterHidden?.let {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            if (it) {
                selection.append(SgShow2Columns.SELECTION_HIDDEN)
            } else {
                selection.append(SgShow2Columns.SELECTION_NO_HIDDEN)
            }
        }

        // unwatched (= next episode is released) and upcoming (= next episode upcoming) filters
        // assumes that no next episode == NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE

        val timeInAnHour = TimeTools.getCurrentTime(getApplication()) + DateUtils.HOUR_IN_MILLIS
        // next episode upcoming within <limit> days + 1 hour, or all future
        val upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(getApplication())
        val maxTimeUpcoming = if (upcomingLimitInDays != -1) {
            timeInAnHour + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS
        } else {
            -1 // any future release date
        }

        if (filter.isFilterUnwatched != null || filter.isFilterUpcoming != null) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
        }

        if (filter.isFilterUnwatched.isTrue() && filter.isFilterUpcoming.isTrue()) {
            // unwatched and upcoming
            selection.append(SgShow2Columns.SELECTION_HAS_NEXT_EPISODE)
            if (maxTimeUpcoming != -1L) {
                selection.append(" AND ")
                    .append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(maxTimeUpcoming)
            }
        } else if (
            filter.isFilterUnwatched.isTrue() && filter.isFilterUpcoming.isNullOrFalse()
        ) {
            // unwatched only
            selection
                .append(SgShow2Columns.SELECTION_HAS_NEXT_EPISODE)
                .append(" AND ")
                .append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(timeInAnHour)
        } else if (
            filter.isFilterUpcoming.isTrue() && filter.isFilterUnwatched.isNullOrFalse()
        ) {
            // upcoming only
            selection
                .append(SgShow2Columns.NEXTAIRDATEMS).append(">")
                .append(timeInAnHour)
            if (maxTimeUpcoming != -1L) {
                selection.append(" AND ")
                    .append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(maxTimeUpcoming)
            }
        } else if (filter.isFilterUnwatched.isFalse()) {
            if (filter.isFilterUpcoming == null) {
                // all released episodes watched (== anything in the future or no next episode)
                // Warning: use parentheses with OR to ensure precedence!
                selection
                    .append("(")
                    .append(SgShow2Columns.NEXTAIRDATEMS).append(">")
                    .append(timeInAnHour)
                    .append(" OR ")
                    .append(SgShow2Columns.SELECTION_NO_NEXT_EPISODE)
                    .append(")")
            } else if (!filter.isFilterUpcoming) {
                // all released episodes watched plus exclude any upcoming, ignoring upcoming range
                // (== no next episode)
                selection.append(SgShow2Columns.SELECTION_NO_NEXT_EPISODE)
            }
        } else if (filter.isFilterUpcoming.isFalse() && filter.isFilterUnwatched == null) {
            // exclude any upcoming, ignoring upcoming range
            selection.append(SgShow2Columns.NEXTAIRDATEMS).append("<=").append(timeInAnHour)
        }

        queryString.value = if (selection.isNotEmpty()) {
            "SELECT * FROM ${Tables.SG_SHOW} WHERE $selection ORDER BY $orderClause"
        } else {
            "SELECT * FROM ${Tables.SG_SHOW} ORDER BY $orderClause"
        }
    }

}