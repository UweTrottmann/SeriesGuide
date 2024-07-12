// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.ui.dialogs.YearPickerDialogFragment.Companion.MINIMUM_YEAR
import com.battlelancer.seriesguide.ui.dialogs.YearPickerDialogFragment.Companion.YEAR_CURRENT
import com.battlelancer.seriesguide.ui.dialogs.toActualYear

object ShowsDiscoverSettings {

    private const val KEY_FIRST_RELEASE_YEAR = "seriesguide.shows.discover.firstreleaseyear"
    private const val KEY_ORIGINAL_LANGUAGE = "seriesguide.shows.discover.originallanguage"

    /**
     * Can save [YEAR_CURRENT] as [value] so [getFirstReleaseYear] always returns the current year.
     */
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

    /**
     * Like [getFirstReleaseYearRaw], but if the stored value is [YEAR_CURRENT],
     * returns the current year.
     */
    fun getFirstReleaseYear(context: Context): Int? {
        return getFirstReleaseYearRaw(context)
            .let { it.toActualYear() }
    }

    /**
     * If no value is stored, returns `null`.
     *
     * If a value is stored, returns [YEAR_CURRENT] or at least [MINIMUM_YEAR].
     */
    fun getFirstReleaseYearRaw(context: Context): Int? {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_FIRST_RELEASE_YEAR, 0)
            .let {
                when (it) {
                    0 -> null
                    YEAR_CURRENT -> YEAR_CURRENT
                    else -> it.coerceAtLeast(MINIMUM_YEAR)
                }
            }
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