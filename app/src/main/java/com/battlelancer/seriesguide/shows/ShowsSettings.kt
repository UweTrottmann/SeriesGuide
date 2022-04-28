package com.battlelancer.seriesguide.shows

import android.content.Context
import androidx.preference.PreferenceManager

object ShowsSettings {

    const val KEY_LAST_ACTIVE_SHOWS_TAB = "com.battlelancer.seriesguide.activitytab"

    fun saveLastShowsTabPosition(context: Context, position: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(ShowsSettings.KEY_LAST_ACTIVE_SHOWS_TAB, position)
            .apply()
    }

    /**
     * Return the position of the last selected shows tab.
     */
    fun getLastShowsTabPosition(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(
                KEY_LAST_ACTIVE_SHOWS_TAB,
                ShowsActivityImpl.Tab.SHOWS.index
            )
    }

}