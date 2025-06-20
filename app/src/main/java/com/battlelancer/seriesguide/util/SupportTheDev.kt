// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import androidx.core.content.edit
import androidx.preference.PreferenceManager

/**
 * Tools to periodically ask users to support the developer of this app.
 * Some might not even be aware this is possible.
 */
object SupportTheDev {

    const val SUPPORT_MESSAGE_DURATION_MILLISECONDS = 10000

    private const val PREF_SUPPORT_DEV_LAST_DISMISSED =
        "com.uwetrottmann.seriesguide.support_dev_last_dismissed"

    private const val DURATION_ASK_AGAIN = 6 * DateUtils.WEEK_IN_MILLIS

    fun shouldAsk(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val lastDismissed = prefs.getLong(PREF_SUPPORT_DEV_LAST_DISMISSED, 0)

        if (Utils.hasAccessToX(context)) {
            // Reset to ask again after access expired
            if (lastDismissed != 0L) {
                prefs.saveLastDismissed(0)
            }
            return false
        } else {
            return when {
                lastDismissed == 0L -> {
                    // Do not ask the first time.
                    prefs.saveLastDismissed(System.currentTimeMillis())
                    false
                }
                lastDismissed < System.currentTimeMillis() - DURATION_ASK_AGAIN -> {
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    fun saveDismissedRightNow(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .saveLastDismissed(System.currentTimeMillis())
    }

    private fun SharedPreferences.saveLastDismissed(timeInMillis: Long) {
        edit {
            putLong(PREF_SUPPORT_DEV_LAST_DISMISSED, timeInMillis)
        }
    }

}