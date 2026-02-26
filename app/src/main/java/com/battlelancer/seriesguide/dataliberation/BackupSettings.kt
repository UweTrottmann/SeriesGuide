// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.text.format.DateUtils
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.dataliberation.BackupSettings.isWarnLastAutoBackupFailed

/**
 * Settings related to creating and restoring backups of the database.
 */
object BackupSettings {

    // manual backup
    private const val KEY_SHOWS_EXPORT_URI = "com.battlelancer.seriesguide.backup.showsExport"
    private const val KEY_SHOWS_IMPORT_URI = "com.battlelancer.seriesguide.backup.showsImport"
    private const val KEY_LISTS_EXPORT_URI = "com.battlelancer.seriesguide.backup.listsExport"
    private const val KEY_LISTS_IMPORT_URI = "com.battlelancer.seriesguide.backup.listsImport"
    private const val KEY_MOVIES_EXPORT_URI = "com.battlelancer.seriesguide.backup.moviesExport"
    private const val KEY_MOVIES_IMPORT_URI = "com.battlelancer.seriesguide.backup.moviesImport"

    // auto backup
    // Previous auto backup preference key.
    // Renamed (= reset) so on upgrade the new auto backup is turned on,
    // as users might have dismissed the setup notification which
    // turned off the old auto backup.
//    private const val KEY_AUTOBACKUP = "com.battlelancer.seriesguide.autobackup"
    private const val KEY_AUTOBACKUP = "autobackup"
    private const val KEY_AUTO_BACKUP_USE_DEFAULT_FILES =
        "com.battlelancer.seriesguide.autobackup.defaultFiles"
    private const val KEY_AUTOBACKUP_SHOWS_EXPORT_URI =
        "com.battlelancer.seriesguide.autobackup.showsExport"
    private const val KEY_AUTOBACKUP_LISTS_EXPORT_URI =
        "com.battlelancer.seriesguide.autobackup.listsExport"
    private const val KEY_AUTOBACKUP_MOVIES_EXPORT_URI =
        "com.battlelancer.seriesguide.autobackup.moviesExport"

    // Store some auto backup prefs in separate preference file
    // that is not backed up by Android app data backup.
    private const val KEY_AUTOBACKUP_LAST_TIME = "last_backup"
    private const val KEY_AUTOBACKUP_LAST_ERROR = "last_error"
    private const val KEY_AUTOBACKUP_LAST_ERROR_WARN = "last_error.warn"

    fun isAutoBackupEnabled(context: Context): Boolean =
        PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_AUTOBACKUP, true)

    fun setAutoBackupEnabled(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(KEY_AUTOBACKUP, true)
            .apply()
    }

    fun setAutoBackupDisabled(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putBoolean(KEY_AUTOBACKUP, false)
            .apply()
    }

    fun isCreateCopyOfAutoBackup(context: Context): Boolean {
        // Re-purposes the old default files pref,
        // which if true meant to backup to the old default location,
        // which if false meant users specified files to backup to.
        // Now if true means to not create any copies,
        // if false means to create a copy to the user specified files.
        return !PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(KEY_AUTO_BACKUP_USE_DEFAULT_FILES, true)
    }

    fun setCreateCopyOfAutoBackup(context: Context, createCopy: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(KEY_AUTO_BACKUP_USE_DEFAULT_FILES, !createCopy)
            .apply()
    }

    private fun getAutoBackupPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(
            context.packageName + "_autobackup",
            Context.MODE_PRIVATE
        )

    fun isTimeForAutoBackup(context: Context): Boolean {
        val now = System.currentTimeMillis()
        val previousBackupTime = getLastAutoBackupTime(context)
        return (now - previousBackupTime) > 7 * DateUtils.DAY_IN_MILLIS
    }

    private fun getLastAutoBackupTime(context: Context): Long {
        val prefs = getAutoBackupPrefs(context)

        var time = prefs.getLong(KEY_AUTOBACKUP_LAST_TIME, 0)
        if (time == 0L) {
            // For new installs set last time to now so backup will not run right away.
            time = System.currentTimeMillis()
            prefs.edit().putLong(KEY_AUTOBACKUP_LAST_TIME, time).apply()
        }

        return time
    }

    fun setLastAutoBackupTimeToNow(context: Context) {
        getAutoBackupPrefs(context)
            .edit()
            .putLong(KEY_AUTOBACKUP_LAST_TIME, System.currentTimeMillis())
            .apply()
    }

    fun getAutoBackupErrorOrNull(context: Context): String? =
        getAutoBackupPrefs(context).getString(KEY_AUTOBACKUP_LAST_ERROR, null)

    /**
     * If error is not null, [isWarnLastAutoBackupFailed]
     * will return true afterwards. If it is null, it will return false.
     */
    fun setAutoBackupErrorOrNull(context: Context, errorOrNull: String?) {
        getAutoBackupPrefs(context)
            .edit()
            .putString(KEY_AUTOBACKUP_LAST_ERROR, errorOrNull)
            .putBoolean(KEY_AUTOBACKUP_LAST_ERROR_WARN, errorOrNull != null)
            .apply()
    }

    fun isWarnLastAutoBackupFailed(context: Context): Boolean =
        getAutoBackupPrefs(context).getBoolean(KEY_AUTOBACKUP_LAST_ERROR_WARN, false)

    /**
     * Afterwards [isWarnLastAutoBackupFailed] returns false.
     */
    fun setHasSeenLastAutoBackupFailed(context: Context) {
        getAutoBackupPrefs(context)
            .edit()
            .putBoolean(KEY_AUTOBACKUP_LAST_ERROR_WARN, false)
            .apply()
    }

    /**
     * Store or remove (by setting it `null`) the URI to an export file.
     */
    fun storeExportFileUri(
        context: Context,
        @JsonExportTask.ExportType type: Int,
        uri: Uri?,
        isAutoBackup: Boolean
    ) {
        val key = if (isAutoBackup) getAutoBackupFileKey(type) else getExportFileKey(type)

        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(key, uri?.toString())
            .apply()
    }

    /**
     * Store the URI to a backup file to import.
     */
    fun storeImportFileUri(
        context: Context,
        @JsonExportTask.ExportType type: Int,
        uri: Uri
    ) {
        val key = getImportFileKey(type)

        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(key, uri.toString())
            .apply()
    }

    fun getExportFileUri(
        context: Context,
        @JsonExportTask.ExportType type: Int,
        isAutoBackup: Boolean
    ): Uri? {
        val key = if (isAutoBackup) getAutoBackupFileKey(type) else getExportFileKey(type)

        val uriString = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(key, null)
        return uriString?.let { Uri.parse(it) }
    }

    /**
     * Retrieve a backup file URI. If looking for an import URI and there is none defined, tries to
     * get the export URI.
     */
    fun getImportFileUriOrExportFileUri(
        context: Context,
        @JsonExportTask.ExportType type: Int
    ): Uri? {
        val key = getImportFileKey(type)

        val uriString = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(key, null)
            ?: return getExportFileUri(
                context,
                type,
                false
            ) // If there is no import uri, try to fall back to the export file uri.
        return Uri.parse(uriString)
    }

    private fun getAutoBackupFileKey(@JsonExportTask.ExportType type: Int): String =
        when (type) {
            JsonExportTask.EXPORT_SHOWS -> KEY_AUTOBACKUP_SHOWS_EXPORT_URI
            JsonExportTask.EXPORT_LISTS -> KEY_AUTOBACKUP_LISTS_EXPORT_URI
            JsonExportTask.EXPORT_MOVIES -> KEY_AUTOBACKUP_MOVIES_EXPORT_URI
            else -> throw IllegalArgumentException("Unknown backup type $type")
        }

    private fun getExportFileKey(@JsonExportTask.ExportType type: Int): String =
        when (type) {
            JsonExportTask.EXPORT_SHOWS -> KEY_SHOWS_EXPORT_URI
            JsonExportTask.EXPORT_LISTS -> KEY_LISTS_EXPORT_URI
            JsonExportTask.EXPORT_MOVIES -> KEY_MOVIES_EXPORT_URI
            else -> throw IllegalArgumentException("Unknown backup type $type")
        }

    private fun getImportFileKey(@JsonExportTask.ExportType type: Int): String =
        when (type) {
            JsonExportTask.EXPORT_SHOWS -> KEY_SHOWS_IMPORT_URI
            JsonExportTask.EXPORT_LISTS -> KEY_LISTS_IMPORT_URI
            JsonExportTask.EXPORT_MOVIES -> KEY_MOVIES_IMPORT_URI
            else -> throw IllegalArgumentException("Unknown backup type $type")
        }

    /**
     * Returns whether an auto backup file is not configured (either because the user did not or the
     * backup task removed a file that had issues).
     */
    fun isMissingAutoBackupFile(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_AUTOBACKUP_SHOWS_EXPORT_URI, null) == null
                || prefs.getString(KEY_AUTOBACKUP_LISTS_EXPORT_URI, null) == null
                || prefs.getString(KEY_AUTOBACKUP_MOVIES_EXPORT_URI, null) == null
    }
}
