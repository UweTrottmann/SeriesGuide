package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.TimeTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar

class CalendarFragment2ViewModel(application: Application) : AndroidViewModel(application) {

    private val queryLiveData = MutableLiveData<String>()

    private val calendarItemPagingConfig = PagingConfig(
        pageSize = 50,
        enablePlaceholders = false /* some items may have a header, so their height differs */
    )
    val items: Flow<PagingData<CalendarItem>> =
        queryLiveData.asFlow().flatMapLatest {
            Pager(calendarItemPagingConfig) {
                SgRoomDatabase.getInstance(getApplication()).sgEpisode2Helper()
                    .getEpisodesWithShowDataSource(SimpleSQLiteQuery(it, null))
            }.flow.map { pagingData ->
                val calendar = Calendar.getInstance()
                pagingData.map { episode ->
                    val headerTime = calculateHeaderTime(
                        getApplication(),
                        calendar,
                        episode.episode_firstairedms
                    )
                    CalendarItem(
                        headerTime,
                        episode
                    )
                }
            }
        }.cachedIn(viewModelScope)

    /**
     * Builds the calendar query based on given settings, updates the associated LiveData which
     * will update the query results.
     */
    suspend fun updateCalendarQuery(isUpcomingElseRecent: Boolean) =
        withContext(Dispatchers.Default) {
            Timber.d("updateCalendarQuery")
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
