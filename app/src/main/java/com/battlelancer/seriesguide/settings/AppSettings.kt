package com.battlelancer.seriesguide.settings

import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateUtils
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.util.Errors
import timber.log.Timber

object AppSettings {

    const val KEY_VERSION = "oldversioncode"

    @Deprecated("")
    const val KEY_GOOGLEANALYTICS = "enableGAnalytics"

    @Deprecated("")
    const val KEY_HAS_SEEN_NAV_DRAWER = "hasSeenNavDrawer"
    const val KEY_ASKED_FOR_FEEDBACK = "askedForFeedback"
    const val KEY_SEND_ERROR_REPORTS = "com.battlelancer.seriesguide.sendErrorReports"
    const val KEY_USER_DEBUG_MODE_ENBALED = "com.battlelancer.seriesguide.userDebugModeEnabled"

    /**
     * Returns the version code of the previously installed version. Is the current version on fresh
     * installs.
     */
    @JvmStatic
    fun getLastVersionCode(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        var lastVersionCode = prefs.getInt(KEY_VERSION, -1)
        if (lastVersionCode == -1) {
            // set current version as default value
            lastVersionCode = BuildConfig.VERSION_CODE
            prefs.edit().putInt(KEY_VERSION, lastVersionCode).apply()
        }
        return lastVersionCode
    }

    @JvmStatic
    fun shouldAskForFeedback(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        if (prefs.getBoolean(KEY_ASKED_FOR_FEEDBACK, false)) {
            return false // already asked for feedback
        }
        try {
            val ourPackageInfo = context.packageManager
                .getPackageInfo(context.packageName, 0)
            val installedRecently = System.currentTimeMillis() <
                    ourPackageInfo.firstInstallTime + 30 * DateUtils.DAY_IN_MILLIS
            if (installedRecently) {
                return false // was only installed recently
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e, "Failed to find our package info.")
            return false // failed to find our package
        }
        return true
    }

    @JvmStatic
    fun setAskedForFeedback(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_ASKED_FOR_FEEDBACK, true)
            .apply()
    }

    fun isSendErrorReports(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_SEND_ERROR_REPORTS, true)
    }

    fun setSendErrorReports(context: Context, isEnabled: Boolean, save: Boolean) {
        if (save) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_SEND_ERROR_REPORTS, isEnabled)
                .apply()
        }
        Timber.d("Turning error reporting %s", if (isEnabled) "ON" else "OFF")
        Errors.getReporter()?.setCrashlyticsCollectionEnabled(isEnabled)
    }

    /**
     * Returns if user-visible debug components should be enabled
     * (e.g. logging to logcat, debug views). Always true for debug builds.
     */
    fun isUserDebugModeEnabled(context: Context): Boolean {
        return BuildConfig.DEBUG || PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_USER_DEBUG_MODE_ENBALED, false)
    }
}