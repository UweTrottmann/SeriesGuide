// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object ShowsDiscoverSettings {

    const val KEY_FIRST_RELEASE_YEAR = "seriesguide.shows.discover.firstreleaseyear"
    const val KEY_ORIGINAL_LANGUAGE = "seriesguide.shows.discover.originallanguage"

    fun setFirstReleaseYear(context: Context, value: Int?) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit {
                if (value != null) {
                    putInt(KEY_FIRST_RELEASE_YEAR, value)
                } else {
                    remove(KEY_FIRST_RELEASE_YEAR)
                }
            }
    }

    fun getFirstReleaseYear(context: Context): Int? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_FIRST_RELEASE_YEAR, 0)
            .let { if (it == 0) return null else it }
    }

    fun setOriginalLanguage(context: Context, value: String?) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit { putString(KEY_ORIGINAL_LANGUAGE, value) }
    }

    fun getOriginalLanguage(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_ORIGINAL_LANGUAGE, null)
    }

}