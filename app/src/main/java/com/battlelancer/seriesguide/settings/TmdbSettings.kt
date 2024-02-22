// SPDX-License-Identifier: Apache-2.0
// Copyright 2014, 2016-2018, 2020-2021, 2023-2024 Uwe Trottmann

package com.battlelancer.seriesguide.settings

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.settings.DisplaySettings.isVeryHighDensityScreen

object TmdbSettings {

    const val KEY_TMDB_BASE_URL = "com.battlelancer.seriesguide.tmdb.baseurl"
    const val POSTER_SIZE_SPEC_W154 = "w154"
    const val POSTER_SIZE_SPEC_W342 = "w342"
    private const val STILL_SIZE_SPEC_W300 = "w300"
    const val IMAGE_SIZE_SPEC_ORIGINAL = "original"
    private const val DEFAULT_BASE_URL = "https://image.tmdb.org/t/p/"

    @JvmStatic
    fun getImageBaseUrl(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_TMDB_BASE_URL, null) ?: DEFAULT_BASE_URL
    }

    fun getImageOriginalUrl(context: Context, path: String): String {
        return getImageBaseUrl(context) + IMAGE_SIZE_SPEC_ORIGINAL + path
    }

    /**
     * Returns base image URL based on screen density.
     */
    @JvmStatic
    fun getPosterBaseUrl(context: Context): String {
        return if (isVeryHighDensityScreen(context)) {
            getImageBaseUrl(context) + POSTER_SIZE_SPEC_W342
        } else {
            getImageBaseUrl(context) + POSTER_SIZE_SPEC_W154
        }
    }

    fun getStillUrl(context: Context, path: String): String {
        return getImageBaseUrl(context) + STILL_SIZE_SPEC_W300 + path
    }
}
