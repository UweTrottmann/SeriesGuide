package com.battlelancer.seriesguide.ui.shows

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.shows.CalendarFragment.ACTIVITY_DAY_LIMIT
import com.battlelancer.seriesguide.util.TimeTools

class CalendarFragment2ViewModel(application: Application) : AndroidViewModel(application) {

    val upcomingEpisodesLiveData: LiveData<List<EpisodeWithShow>>

    init {
        val timeThreshold =
            System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS * ACTIVITY_DAY_LIMIT
//        val recentThreshold = TimeTools.getCurrentTime(getApplication()) - DateUtils.HOUR_IN_MILLIS
        val recentThreshold = TimeTools.getCurrentTime(getApplication()) - DateUtils.DAY_IN_MILLIS * ACTIVITY_DAY_LIMIT

        upcomingEpisodesLiveData = SgRoomDatabase.getInstance(getApplication()).episodeHelper()
            .getUpcomingEpisodes(recentThreshold, timeThreshold)
    }

}
