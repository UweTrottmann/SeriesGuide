// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.extensions.ExtensionManager
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.traktapi.TraktSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * If necessary, runs upgrade code after an update with a higher version code is installed.
 */
class AppUpgrade(
    private val context: Context,
    private val lastVersion: Int = AppSettings.getLastVersionCode(context),
    private val currentVersion: Int = BuildConfig.VERSION_CODE
) {

    /**
     * Returns true if the app was updated from a previous version.
     */
    fun upgradeIfNewVersion(): Boolean {
        return if (lastVersion < currentVersion) {
            doUpgrades()
            // Update last version to current version
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(AppSettings.KEY_VERSION, currentVersion)
                .apply()
            true
        } else {
            false
        }
    }

    private fun doUpgrades() {
        // Run some required tasks after updating to certain versions.
        // NOTE: see version codes for upgrade description.
        if (lastVersion < SgApp.RELEASE_VERSION_16_BETA1) {
            clearLegacyExternalFileCache(context)
        }

        if (lastVersion < SgApp.RELEASE_VERSION_23_BETA4) {
            // make next trakt sync download watched movies
            TraktSettings.resetMoviesLastWatchedAt(context)
        }

        if (lastVersion < SgApp.RELEASE_VERSION_36_BETA2) {
            // used account name to determine sign-in state before switch to Google Sign-In
            if (!HexagonSettings.getAccountName(context).isNullOrEmpty()) {
                // tell users to sign in again
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, true)
                    .apply()
            }
        }

        if (lastVersion < SgApp.RELEASE_VERSION_40_BETA4) {
            ExtensionManager.get(context).setDefaultEnabledExtensions(context)
        }

        if (lastVersion < SgApp.RELEASE_VERSION_40_BETA6) {
            // cancel old widget alarm using implicit intent
            val am = context.getSystemService<AlarmManager>()
            if (am != null) {
                val pi = PendingIntent.getBroadcast(
                    context,
                    ListWidgetProvider.REQUEST_CODE,
                    Intent(ListWidgetProvider.ACTION_DATA_CHANGED),
                    // Mutable because it was created without flags which defaulted to mutable.
                    PendingIntentCompat.flagMutable
                )
                am.cancel(pi)
            }
            // new alarm is set automatically as upgrading causes app widgets to update
        }

        if (lastVersion != SgApp.RELEASE_VERSION_50_1
            && lastVersion < SgApp.RELEASE_VERSION_51_BETA4) {
            // Movies were not added in all cases when syncing, so ensure they are now.
            TraktSettings.resetMoviesLastWatchedAt(context)
            HexagonSettings.resetSyncState(context)
        }

        if (lastVersion < SgApp.RELEASE_VERSION_59_BETA1) {
            // Changed ID of Canceled show status for better sorting.
            SgApp.coroutineScope.launch(Dispatchers.IO) {
                SgRoomDatabase.getInstance(context).sgShow2Helper().migrateCanceledShowStatus()
            }
        }
    }

    /**
     * Clear all files in files directory on external storage.
     */
    private fun clearLegacyExternalFileCache(context: Context) {
        val path = context.applicationContext.getExternalFilesDir(null)
        if (path == null) {
            Timber.w("Could not clear cache, external storage not available")
            return
        }
        val files = path.listFiles()
        if (files != null) {
            for (file in files) {
                file.delete()
            }
        }
    }

}