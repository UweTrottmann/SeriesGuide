package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.persistence.db.SimpleSQLiteQuery
import android.text.format.DateUtils
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.util.TimeTools

class ShowsViewModel(application: Application) : AndroidViewModel(application) {

    private val queryString = MutableLiveData<String>()
    val shows: LiveData<List<SgShow>>

    init {
        shows = Transformations.switchMap(
            queryString
        ) { queryString ->
            SgRoomDatabase.getInstance(getApplication()).showHelper()
                .queryShows(SimpleSQLiteQuery(queryString, null))
        }
    }

    fun updateQuery(
        isFilterFavorites: Boolean,
        isFilterUnwatched: Boolean,
        isFilterUpcoming: Boolean,
        isFilterHidden: Boolean,
        orderClause: String
    ) {
        val selection = StringBuilder()

        // restrict to favorites?
        if (isFilterFavorites) {
            selection.append(SeriesGuideContract.Shows.FAVORITE).append("=1")
        }

        val timeInAnHour = TimeTools.getCurrentTime(getApplication()) + DateUtils.HOUR_IN_MILLIS

        // restrict to shows with a next episode?
        if (isFilterUnwatched) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            selection.append(SeriesGuideContract.Shows.SELECTION_WITH_RELEASED_NEXT_EPISODE)

            // exclude shows with upcoming next episode
            if (!isFilterUpcoming) {
                selection.append(" AND ")
                    .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                    .append(timeInAnHour)
            }
        }
        // restrict to shows with an upcoming (yet to air) next episode?
        if (isFilterUpcoming) {
            if (selection.isNotEmpty()) {
                selection.append(" AND ")
            }
            // Display shows upcoming within <limit> days + 1 hour
            val upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(getApplication())
            val latestAirtime = timeInAnHour + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS

            selection.append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append("<=")
                .append(latestAirtime)

            // exclude shows with no upcoming next episode if not filtered for unwatched, too
            if (!isFilterUnwatched) {
                selection.append(" AND ")
                    .append(SeriesGuideContract.Shows.NEXTAIRDATEMS).append(">=")
                    .append(timeInAnHour)
            }
        }

        // special: if hidden filter is disabled, exclude hidden shows
        if (selection.isNotEmpty()) {
            selection.append(" AND ")
        }
        selection.append(SeriesGuideContract.Shows.HIDDEN)
            .append(if (isFilterHidden) "=1" else "=0")

        queryString.value = "SELECT * FROM ${SeriesGuideDatabase.Tables.SHOWS}" +
                " WHERE $selection" +
                " ORDER BY $orderClause"
    }

    fun reRunQuery() {
        queryString.value = queryString.value
    }

}