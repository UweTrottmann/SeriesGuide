// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.settings

import android.content.Context
import android.text.format.DateUtils
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object TmdbSettings {

    private const val KEY_LAST_UPDATED = "com.uwetrottmann.seriesguide.tmdb.lastupdated"

    /* If the image URL changes the old one should be working for a while. If watch providers
    change, typically only affects new shows or new seasons. As users likely check a show once a
     week, updating weekly should be fine. */
    private const val UPDATE_INTERVAL_MS = 7 * DateUtils.DAY_IN_MILLIS

    private const val KEY_TMDB_BASE_URL = "com.battlelancer.seriesguide.tmdb.baseurl"
    const val DEFAULT_BASE_URL = "https://image.tmdb.org/t/p/"

    fun isConfigurationUpToDate(context: Context): Boolean {
        val lastUpdatedMs =
            PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY_LAST_UPDATED, 0)
        return lastUpdatedMs + UPDATE_INTERVAL_MS >= System.currentTimeMillis()
    }

    fun setConfigurationLastUpdatedNow(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
        }
    }

    /**
     * Saves the base URL, unless it's empty or blank.
     */
    fun setImageBaseUrl(context: Context, url: String) {
        if (url.isEmpty() || url.isBlank()) return
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_TMDB_BASE_URL, url)
        }
    }

    /**
     * The base URL to use for loading images from TMDB.
     *
     * See [com.battlelancer.seriesguide.tmdbapi.TmdbTools] for builder methods.
     */
    fun getImageBaseUrl(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_TMDB_BASE_URL, null) ?: DEFAULT_BASE_URL
    }

}
