package com.battlelancer.seriesguide.movies

import android.content.Context
import androidx.preference.PreferenceManager

object MoviesSettings {

    const val KEY_LAST_ACTIVE_MOVIES_TAB = "com.battlelancer.seriesguide.moviesActiveTab"

    @JvmStatic
    fun saveLastMoviesTabPosition(context: Context, position: Int) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(KEY_LAST_ACTIVE_MOVIES_TAB, position)
            .apply()
    }

    /**
     * Return the position of the last selected movies tab.
     */
    @JvmStatic
    fun getLastMoviesTabPosition(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_LAST_ACTIVE_MOVIES_TAB, 0)
    }

}