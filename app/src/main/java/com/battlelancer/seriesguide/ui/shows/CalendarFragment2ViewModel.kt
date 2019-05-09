package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.content.Context
import android.os.AsyncTask
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.TimeTools
import java.util.Calendar
import java.util.LinkedList

class CalendarFragment2ViewModel(application: Application) : AndroidViewModel(application) {

    val upcomingEpisodesLiveData = MediatorLiveData<List<CalendarItem>>()
    private val upcomingEpisodesRawLiveData: LiveData<List<EpisodeWithShow>>

    init {
        val recentThreshold = TimeTools.getCurrentTime(getApplication()) - DateUtils.HOUR_IN_MILLIS

        upcomingEpisodesRawLiveData = SgRoomDatabase.getInstance(getApplication()).episodeHelper()
            .getUpcomingEpisodes(recentThreshold)

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
