package com.battlelancer.seriesguide.settings

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Access advanced settings for auto backup and auto update.
 */
object AdvancedSettings {

    private const val KEY_LAST_SUPPORTER_STATE = "com.battlelancer.seriesguide.lastupgradestate"
    const val KEY_UPCOMING_LIMIT = "com.battlelancer.seriesguide.upcominglimit"

    /**
     * (Only Amazon version) Returns if the user was a supporter through an in-app purchase
     * the last time we checked.
     */
    @JvmStatic
    fun getLastSupporterState(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
            KEY_LAST_SUPPORTER_STATE, false
        )
    }

    /**
     * (Only Amazon version) Set if the user currently has an active purchase to support the app.
     */
    @JvmStatic
    fun setSupporterState(context: Context, isSubscribedToX: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(
            KEY_LAST_SUPPORTER_STATE, isSubscribedToX
        ).apply()
    }

    /**
     * Returns the maximum number of days from today on an episode can air for its show to be
     * considered as upcoming. Returns -1 if any future release date is considered upcoming.
     */
    fun getUpcomingLimitInDays(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
            KEY_UPCOMING_LIMIT, null
        )?.toIntOrNull() ?: 3
    }
}