package com.battlelancer.seriesguide

import android.app.backup.BackupAgent
import android.app.backup.BackupAgentHelper
import android.app.backup.SharedPreferencesBackupHelper
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.ui.shows.FirstRunView

/**
 * On API 23+ (M+) uses full backup. On older versions key-value based backup, which requires
 * a com.google.android.backup.api_key meta-data tag in AndroidManifest.xml.
 *
 * A [BackupAgent] that resets Hexagon and first run settings in [BackupAgent.onRestoreFinished].
 */
class SgBackupAgent : BackupAgentHelper() {

    // A key to uniquely identify the set of backup data for legacy key-value backup.
    private val keyValueBackupKey = "prefs"

    override fun onCreate() {
        // Allocate a helper for legacy key-value backup and add it to the backup agent.
        // Note: packageName requires context, so do not access before here.
        SharedPreferencesBackupHelper(this, "${packageName}_preferences").also {
            addHelper(keyValueBackupKey, it)
        }
    }

    override fun onRestoreFinished() {
        PreferenceManager.getDefaultSharedPreferences(applicationContext).edit {
            // First run view is useful on re-installing: has important settings, first actions.
            putBoolean(FirstRunView.PREF_KEY_FIRSTRUN, false)

            // Disable Hexagon. Re-enabling will reset any previous sync state.
            // Note: silent sign-in might work on re-installs. But explicitly requiring to turn on
            // Hexagon again, e.g. to allow restoring backup before.
            putBoolean(HexagonSettings.KEY_ENABLED, false)
            putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, false)

            // Note: Trakt stores access token using account system.
            // That should not get backed up and restored. On re-connecting will reset sync state.
            // So nothing to do here.
        }

    }

}