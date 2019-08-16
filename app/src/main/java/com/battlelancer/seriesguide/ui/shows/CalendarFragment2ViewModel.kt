package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

class CalendarFragment2ViewModel(application: Application) : AndroidViewModel(application) {

    private val queryLiveData = MutableLiveData<String>()
    val upcomingEpisodesLiveData: LiveData<PagedList<CalendarItem>>

    private val calendarItemPagingConfig = Config(
        pageSize = 50,
        enablePlaceholders = false /* some items may have a header, so their height differs */
    )

    init {
        upcomingEpisodesLiveData = Transformations.switchMap(queryLiveData) { queryString ->
            SgRoomDatabase.getInstance(getApplication()).episodeHelper()
                .getEpisodesWithShow(SimpleSQLiteQuery(queryString, null))
                .mapByPage { episodes ->
                    val calendar = Calendar.getInstance()
                    episodes.map { episode ->
                        val headerTime = calculateHeaderTime(
                            getApplication(),
                            calendar,
                            episode.episode_firstairedms
                        )
                        CalendarItem(headerTime, episode)
                    }
                }.toLiveData(config = calendarItemPagingConfig)
        }
    }

    /**
     * Builds the calendar query based on given settings, updates the associated LiveData which
     * will update the query results.
     * [type] defaults to [CalendarFragment2.CalendarType.UPCOMING].
     */
    suspend fun updateCalendarQuery(type: CalendarFragment2.CalendarType) =
        withContext(Dispatchers.Default) {
            Timber.i("updateCalendarQuery")

            val isInfiniteCalendar = CalendarSettings.isInfiniteScrolling(getApplication())

            // go an hour back in time, so episodes move to recent one hour late
            val recentThreshold =
                TimeTools.getCurrentTime(getApplication()) - DateUtils.HOUR_IN_MILLIS

            val query: StringBuilder
            val sortOrder: String
            if (CalendarFragment2.CalendarType.RECENT == type) {
                val timeThreshold = if (isInfiniteCalendar) {
                    // Include all past episodes.
                    Long.MIN_VALUE
                } else {
                    // Only episodes from the last few days.
                    System.currentTimeMillis() - CALENDAR_DAY_LIMIT_MS
                }
                query =
                    StringBuilder("${Episodes.SELECTION_HAS_RELEASE_DATE} AND ${Episodes.FIRSTAIREDMS}<$recentThreshold AND ${Episodes.FIRSTAIREDMS}>$timeThreshold AND ${Shows.SELECTION_NO_HIDDEN}")
                sortOrder = CalendarQuery.SORTING_RECENT
            } else /* UPCOMING */ {
                val timeThreshold = if (isInfiniteCalendar) {
                    // Include all future episodes.
                    Long.MAX_VALUE
                } else {
                    // Only episodes from the next few days.
                    System.currentTimeMillis() + CALENDAR_DAY_LIMIT_MS
                }
                query = StringBuilder("${Episodes.FIRSTAIREDMS}>=$recentThreshold AND ${Episodes.FIRSTAIREDMS}<$timeThreshold AND ${Shows.SELECTION_NO_HIDDEN}")
                sortOrder = CalendarQuery.SORTING_UPCOMING
            }

            // append only favorites selection if necessary
            if (CalendarSettings.isOnlyFavorites(getApplication())) {
                query.append(" AND ").append(Shows.SELECTION_FAVORITES)
            }

            // append no specials selection if necessary
            if (DisplaySettings.isHidingSpecials(getApplication())) {
                query.append(" AND ").append(Episodes.SELECTION_NO_SPECIALS)
            }

            // append unwatched selection if necessary
            if (CalendarSettings.isHidingWatchedEpisodes(getApplication())) {
                query.append(" AND ").append(Episodes.SELECTION_UNWATCHED)
            }

            // only show collected episodes
            if (CalendarSettings.isOnlyCollected(getApplication())) {
                query.append(" AND ").append(Episodes.SELECTION_COLLECTED)
            }

            // Post value because not on main thread + also avoids race condition if data is
            // delivered too early causing RecyclerView to jump to next page.
            // However, could not narrow down why that is an issue (it should not be?).
            queryLiveData.postValue(
                "${EpisodeWithShow.select} " +
                        "LEFT OUTER JOIN series ON episodes.series_id=series._id " +
                        "WHERE $query " +
                        "ORDER BY $sortOrder "
            )
        }

    private fun calculateHeaderTime(context: Context, calendar: Calendar, releaseTime: Long): Long {
        val actualRelease = TimeTools.applyUserOffset(context, releaseTime)

        calendar.time = actualRelease
        // not midnight because upcoming->recent is delayed 1 hour
        // so header would display wrong relative time close to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 1)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return calendar.timeInMillis
    }

    /**
     * [episode] is null if this is a header item.
     */
    data class CalendarItem(val headerTime: Long, val episode: EpisodeWithShow)

    companion object {
        private const val CALENDAR_DAY_LIMIT_MS = 31 * DateUtils.DAY_IN_MILLIS
    }

}
