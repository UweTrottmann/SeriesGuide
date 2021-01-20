package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow
import com.battlelancer.seriesguide.provider.SgRoomDatabase
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
            SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
                .getEpisodesWithShowDataSource(SimpleSQLiteQuery(queryString, null))
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
     */
    suspend fun updateCalendarQuery(isUpcomingElseRecent: Boolean) =
        withContext(Dispatchers.Default) {
            Timber.i("updateCalendarQuery")
            // Post value because not on main thread + also avoids race condition if data is
            // delivered too early causing RecyclerView to jump to next page.
            // However, could not narrow down why that is an issue (it should not be?).
            queryLiveData.postValue(
                SgEpisode2WithShow.buildEpisodesWithShowQuery(
                    getApplication(),
                    isUpcomingElseRecent,
                    isInfiniteCalendar = CalendarSettings.isInfiniteScrolling(getApplication()),
                    isOnlyFavorites = CalendarSettings.isOnlyFavorites(getApplication()),
                    isOnlyUnwatched = CalendarSettings.isHidingWatchedEpisodes(getApplication()),
                    isOnlyCollected = CalendarSettings.isOnlyCollected(getApplication()),
                    isOnlyPremieres = CalendarSettings.isOnlyPremieres(getApplication())
                )
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

    data class CalendarItem(val headerTime: Long, val episode: SgEpisode2WithShow)

}
