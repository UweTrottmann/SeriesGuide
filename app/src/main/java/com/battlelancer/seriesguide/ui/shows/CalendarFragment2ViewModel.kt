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
import com.battlelancer.seriesguide.provider.SeriesGuideContract
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

            // go an hour back in time, so episodes move to recent one hour late
            val recentThreshold =
                TimeTools.getCurrentTime(getApplication()) - DateUtils.HOUR_IN_MILLIS

            val query: StringBuilder
            val sortOrder: String
            if (CalendarFragment2.CalendarType.RECENT == type) {
                query =
                    StringBuilder("episode_firstairedms!=-1 AND episode_firstairedms<$recentThreshold AND series_hidden=0")
                sortOrder = CalendarQuery.SORTING_RECENT
            } else {
                query = StringBuilder("episode_firstairedms>=$recentThreshold AND series_hidden=0")
                sortOrder = CalendarQuery.SORTING_UPCOMING
            }

            // append only favorites selection if necessary
            if (CalendarSettings.isOnlyFavorites(getApplication())) {
                query.append(" AND ").append(SeriesGuideContract.Shows.SELECTION_FAVORITES)
            }

            // append no specials selection if necessary
            if (DisplaySettings.isHidingSpecials(getApplication())) {
                query.append(" AND ").append(SeriesGuideContract.Episodes.SELECTION_NO_SPECIALS)
            }

            // append unwatched selection if necessary
            if (CalendarSettings.isHidingWatchedEpisodes(getApplication())) {
                query.append(" AND ").append(SeriesGuideContract.Episodes.SELECTION_UNWATCHED)
            }

            // only show collected episodes
            if (CalendarSettings.isOnlyCollected(getApplication())) {
                query.append(" AND ").append(SeriesGuideContract.Episodes.SELECTION_COLLECTED)
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

}
