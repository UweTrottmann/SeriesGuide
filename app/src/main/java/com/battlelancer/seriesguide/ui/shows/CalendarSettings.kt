package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Access settings specifically related to the calendar views, e.g. upcoming and recent episodes.
 */
object CalendarSettings {

    const val KEY_HIDE_WATCHED_EPISODES = "com.battlelancer.seriesguide.activity.nowatched"

    const val KEY_INFINITE_SCROLLING_2 = "com.battlelancer.seriesguide.calendar.infinite"

    const val KEY_ONLY_COLLECTED = "com.battlelancer.seriesguide.activity.onlycollected"

    const val KEY_ONLY_FAVORITE_SHOWS = "com.battlelancer.seriesguide.onlyfavorites"

    const val KEY_ONLY_PREMIERES = "com.battlelancer.seriesguide.calendar.onlypremieres"

    /**
     * Whether the calendar should not include watched episodes.
     */
    fun isHidingWatchedEpisodes(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_HIDE_WATCHED_EPISODES, false)
    }

    /**
     * Whether the calendar should be infinite (default) or limited to a number of days.
     */
    fun isInfiniteScrolling(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            KEY_INFINITE_SCROLLING_2,
            true
        )
    }

    /**
     * Whether the calendar should only include collected episodes.
     */
    fun isOnlyCollected(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            KEY_ONLY_COLLECTED,
            false
        )
    }

    /**
     * Whether the calendar should only include episodes from favorite shows.
     */
    fun isOnlyFavorites(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            KEY_ONLY_FAVORITE_SHOWS,
            false
        )
    }

    /**
     * Whether the calendar should only include first episodes (premieres).
     */
    fun isOnlyPremieres(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            KEY_ONLY_PREMIERES,
            false
        )
    }
}
