package com.battlelancer.seriesguide.util

import android.content.Context
import android.content.SharedPreferences
import android.text.format.DateUtils
import android.view.View
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.google.android.material.snackbar.Snackbar

/**
 * Tools to periodically ask users to support the developer of this app.
 * Some might not even be aware this is possible.
 */
object SupportTheDev {

    private const val PREF_SUPPORT_DEV_LAST_DISMISSED =
        "com.uwetrottmann.seriesguide.support_dev_last_dismissed"

    private const val DURATION_ASK_AGAIN = 6 * DateUtils.WEEK_IN_MILLIS

    @JvmStatic
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

    private fun SharedPreferences.saveLastDismissed(timeInMillis: Long) {
        edit {
            putLong(PREF_SUPPORT_DEV_LAST_DISMISSED, timeInMillis)
        }
    }

    @JvmStatic
    fun buildSnackbar(context: Context, parentView: View): Snackbar {
        return Snackbar.make(parentView, R.string.support_the_dev, 10000)
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(snackbar: Snackbar, event: Int) {
                    // Always do not show again after user has seen it once.
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .saveLastDismissed(System.currentTimeMillis())
                }
            }).setAction(R.string.billing_action_subscribe) {
                context.startActivity(Utils.getBillingActivityIntent(context))
            }
    }

}