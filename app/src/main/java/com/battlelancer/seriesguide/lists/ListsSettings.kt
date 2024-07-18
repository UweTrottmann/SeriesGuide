// SPDX-License-Identifier: Apache-2.0
// Copyright 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.lists

import android.content.Context
import androidx.preference.PreferenceManager

object ListsSettings {

    const val KEY_LAST_ACTIVE_LISTS_TAB = "seriesguide.lists.selectedtab"

    /**
     * Return the position of the last selected lists tab.
     */
    fun getLastListsTabPosition(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_LAST_ACTIVE_LISTS_TAB, 0)
    }

}