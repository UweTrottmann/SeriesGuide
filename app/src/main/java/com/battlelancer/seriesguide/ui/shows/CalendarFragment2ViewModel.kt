package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.content.Context
import android.os.AsyncTask
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.TimeTools
import java.util.Calendar
import java.util.LinkedList

class CalendarFragment2ViewModel(application: Application) : AndroidViewModel(application) {

    private val queryLiveData = MutableLiveData<String>()
    private val upcomingEpisodesRawLiveData: LiveData<List<EpisodeWithShow>>
    val upcomingEpisodesLiveData = MediatorLiveData<List<CalendarItem>>()

    init {
        upcomingEpisodesRawLiveData = Transformations.switchMap(queryLiveData) { queryString ->
            SgRoomDatabase.getInstance(getApplication()).episodeHelper()
                .getEpisodesWithShow(SimpleSQLiteQuery(queryString, null))
        }

        upcomingEpisodesLiveData.addSource(upcomingEpisodesRawLiveData) { episodes ->
            // calculate actually displayed values on a background thread
            AsyncTask.THREAD_POOL_EXECUTOR.execute {
                val mapped = LinkedList<CalendarItem>()

                var previousHeaderTime: Long = 0
                episodes.forEachIndexed { index, episode ->
                    // insert header if first item or previous item has different header time
                    val headerTime =
                        calculateHeaderTime(getApplication(), episode.episode_firstairedms)
                    if (index == 0 || headerTime != previousHeaderTime) {
                        mapped.add(CalendarItem(headerTime, null))
                    }
                    previousHeaderTime = headerTime

                    mapped.add(CalendarItem(headerTime, episode))
                }

                upcomingEpisodesLiveData.postValue(mapped)
            }
        }
    }

    /**
     * Builds the calendar query based on given settings, updates the associated LiveData which
     * will update the query results.
     *
     * @param type A [CalendarType], defaults to UPCOMING.
     */
    fun updateCalendarQuery(
        type: CalendarFragment2.CalendarType,
        isOnlyCollected: Boolean,
        isOnlyFavorites: Boolean,
        isOnlyUnwatched: Boolean
    ) {
        // go an hour back in time, so episodes move to recent one hour late
        val recentThreshold = TimeTools.getCurrentTime(getApplication()) - DateUtils.HOUR_IN_MILLIS

        val query: StringBuilder
        val sortOrder: String
        if (CalendarFragment2.CalendarType.RECENT == type) {
            query = StringBuilder("episode_firstairedms!=-1 AND episode_firstairedms<$recentThreshold AND series_hidden=0")
            sortOrder = CalendarQuery.SORTING_RECENT
        } else {
            query = StringBuilder("episode_firstairedms>=$recentThreshold AND series_hidden=0")
            sortOrder = CalendarQuery.SORTING_UPCOMING
        }

        // append only favorites selection if necessary
        if (isOnlyFavorites) {
            query.append(" AND ").append(SeriesGuideContract.Shows.SELECTION_FAVORITES)
        }

        // append no specials selection if necessary
        val isNoSpecials = DisplaySettings.isHidingSpecials(getApplication())
        if (isNoSpecials) {
            query.append(" AND ").append(SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS)
        }

        // append unwatched selection if necessary
        if (isOnlyUnwatched) {
            query.append(" AND ").append(SeriesGuideContract.Episodes.SELECTION_UNWATCHED)
        }

        // only show collected episodes
        if (isOnlyCollected) {
            query.append(" AND ").append(SeriesGuideContract.Episodes.SELECTION_COLLECTED)
        }

        queryLiveData.value = "${EpisodeWithShow.select} " +
                "LEFT OUTER JOIN series ON episodes.series_id=series._id " +
                "WHERE $query " +
                "ORDER BY $sortOrder " +
                "LIMIT 50" // good compromise between performance and showing most info
    }

    private fun calculateHeaderTime(context: Context, releaseTime: Long): Long {
        val actualRelease = TimeTools.applyUserOffset(context, releaseTime)

        val calendar = Calendar.getInstance()
        calendar.time = actualRelease
        // not midnight because upcoming->recent is delayed 1 hour
        // so header would display wrong relative time close to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    data class CalendarItem(val headerTime: Long, val episode: EpisodeWithShow?)

}
