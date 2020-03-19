package com.battlelancer.seriesguide.dataliberation;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

/**
 * Settings related to creating and restoring backups of the database.
 */
public class BackupSettings {

    // manual backup
    private static final String KEY_SHOWS_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.showsExport";
    private static final String KEY_SHOWS_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.showsImport";
    private static final String KEY_LISTS_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.listsExport";
    private static final String KEY_LISTS_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.listsImport";
    private static final String KEY_MOVIES_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.moviesExport";
    private static final String KEY_MOVIES_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.moviesImport";

    // auto backup
    // Previous auto backup preference key.
    // Renamed (= reset) so on upgrade the new auto backup is turned on,
    // as users might have dismissed the setup notification which
    // turned off the old auto backup.
//    private static final String KEY_AUTOBACKUP
//            = "com.battlelancer.seriesguide.autobackup";
    private static final String KEY_AUTOBACKUP
            = "autobackup";
    private static final String KEY_AUTO_BACKUP_USE_DEFAULT_FILES
            = "com.battlelancer.seriesguide.autobackup.defaultFiles";
    private static final String KEY_AUTOBACKUP_SHOWS_EXPORT_URI
            = "com.battlelancer.seriesguide.autobackup.showsExport";
    private static final String KEY_AUTOBACKUP_LISTS_EXPORT_URI
            = "com.battlelancer.seriesguide.autobackup.listsExport";
    private static final String KEY_AUTOBACKUP_MOVIES_EXPORT_URI
            = "com.battlelancer.seriesguide.autobackup.moviesExport";

    // Store some auto backup prefs in separate preference file
    // that is not backed up by Android app data backup.
    private static final String KEY_AUTOBACKUP_LAST_TIME = "last_backup";
    private static final String KEY_AUTOBACKUP_LAST_ERROR = "last_error";
    private static final String KEY_AUTOBACKUP_LAST_ERROR_WARN = "last_error.warn";

    public static boolean isAutoBackupEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_AUTOBACKUP,
                true);
    }

    static void setAutoBackupEnabled(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_AUTOBACKUP, true)
                .apply();
    }

    static void setAutoBackupDisabled(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_AUTOBACKUP, false)
                .apply();
    }

    public static boolean isCreateCopyOfAutoBackup(Context context) {
        // Re-purposes the old default files pref,
        // which if true meant to backup to the old default location,
        // which if false meant users specified files to backup to.
        // Now if true means to not create any copies,
        // if false means to create a copy to the user specified files.
        return !PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_AUTO_BACKUP_USE_DEFAULT_FILES, true);
    }

    static void setCreateCopyOfAutoBackup(Context context, boolean createCopy) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_AUTO_BACKUP_USE_DEFAULT_FILES, !createCopy)
                .apply();
    }

    private static SharedPreferences getAutoBackupPrefs(Context context) {
        return context.getSharedPreferences(
                context.getPackageName() + "_autobackup",
                Context.MODE_PRIVATE
        );
    }

    public static boolean isTimeForAutoBackup(Context context) {
        long now = System.currentTimeMillis();
        long previousBackupTime = getLastAutoBackupTime(context);
        return (now - previousBackupTime) > 7 * DateUtils.DAY_IN_MILLIS;
    }

    private static long getLastAutoBackupTime(Context context) {
        SharedPreferences prefs = getAutoBackupPrefs(context);

        long time = prefs.getLong(KEY_AUTOBACKUP_LAST_TIME, 0);
        if (time == 0) {
            // For new installs set last time to now so backup will not run right away.
            time = System.currentTimeMillis();
            prefs.edit().putLong(KEY_AUTOBACKUP_LAST_TIME, time).apply();
        }

        return time;
    }

    public static void setLastAutoBackupTimeToNow(Context context) {
        getAutoBackupPrefs(context)
                .edit()
                .putLong(KEY_AUTOBACKUP_LAST_TIME, System.currentTimeMillis())
                .apply();
    }

    @Nullable
    static String getAutoBackupErrorOrNull(Context context) {
        return getAutoBackupPrefs(context)
                .getString(KEY_AUTOBACKUP_LAST_ERROR, null);
    }

    /**
     * If error is not null, {@link #isWarnLastAutoBackupFailed(Context)}
     * will return true afterwards. If it is null, it will return false.
     */
    static void setAutoBackupErrorOrNull(Context context, String errorOrNull) {
        getAutoBackupPrefs(context)
                .edit()
                .putString(KEY_AUTOBACKUP_LAST_ERROR, errorOrNull)
                .putBoolean(KEY_AUTOBACKUP_LAST_ERROR_WARN, errorOrNull != null)
                .apply();
    }

    public static boolean isWarnLastAutoBackupFailed(Context context) {
        return getAutoBackupPrefs(context)
                .getBoolean(KEY_AUTOBACKUP_LAST_ERROR_WARN, false);
    }

    /**
     * Afterwards {@link #isWarnLastAutoBackupFailed(Context)} returns false.
     */
    public static void setHasSeenLastAutoBackupFailed(Context context) {
        getAutoBackupPrefs(context)
                .edit()
                .putBoolean(KEY_AUTOBACKUP_LAST_ERROR_WARN, false)
                .apply();
    }

    /**
     * Store or remove (by setting it {@code null}) the URI to a backup file.
     */
    static void storeExportFileUri(
            Context context,
            @JsonExportTask.BackupType int type,
            @Nullable Uri uri,
            boolean isAutoBackup
    ) {
        String key = isAutoBackup
                ? getAutoBackupFileKey(type)
                : getExportFileKey(type);

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, uri == null ? null : uri.toString())
                .apply();
    }

    /**
     * Store the URI to a backup file to import.
     */
    static void storeImportFileUri(
            Context context,
            @JsonExportTask.BackupType int type,
            @NonNull Uri uri
    ) {
        String key = getImportFileKey(type);

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, uri.toString())
                .apply();
    }

    @Nullable
    static Uri getExportFileUri(
            Context context,
            @JsonExportTask.BackupType int type,
            boolean isAutoBackup
    ) {
        String key = isAutoBackup
                ? getAutoBackupFileKey(type)
                : getExportFileKey(type);

        String uriString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, null);
        return uriString != null
                ? Uri.parse(uriString)
                : null;
    }

    /**
     * Retrieve a backup file URI. If looking for an import URI and there is none defined, tries to
     * get the export URI.
     */
    @Nullable
    static Uri getImportFileUriOrExportFileUri(
            Context context,
            @JsonExportTask.BackupType int type
    ) {
        String key = getImportFileKey(type);

        String uriString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, null);
        if (uriString == null) {
            // If there is no import uri, try to fall back to the export file uri.
            return getExportFileUri(context, type, false);
        }
        return Uri.parse(uriString);
    }

    private static String getAutoBackupFileKey(@JsonExportTask.BackupType int type) {
        switch (type) {
            case JsonExportTask.BACKUP_SHOWS:
                return KEY_AUTOBACKUP_SHOWS_EXPORT_URI;
            case JsonExportTask.BACKUP_LISTS:
                return KEY_AUTOBACKUP_LISTS_EXPORT_URI;
            case JsonExportTask.BACKUP_MOVIES:
                return KEY_AUTOBACKUP_MOVIES_EXPORT_URI;
            default:
                throw new IllegalArgumentException("Unknown backup type " + type);
        }
    }

    private static String getExportFileKey(@JsonExportTask.BackupType int type) {
        switch (type) {
            case JsonExportTask.BACKUP_SHOWS:
                return KEY_SHOWS_EXPORT_URI;
            case JsonExportTask.BACKUP_LISTS:
                return KEY_LISTS_EXPORT_URI;
            case JsonExportTask.BACKUP_MOVIES:
                return KEY_MOVIES_EXPORT_URI;
            default:
                throw new IllegalArgumentException("Unknown backup type " + type);
        }
    }

    private static String getImportFileKey(@JsonExportTask.BackupType int type) {
        switch (type) {
            case JsonExportTask.BACKUP_SHOWS:
                return KEY_SHOWS_IMPORT_URI;
            case JsonExportTask.BACKUP_LISTS:
                return KEY_LISTS_IMPORT_URI;
            case JsonExportTask.BACKUP_MOVIES:
                return KEY_MOVIES_IMPORT_URI;
            default:
                throw new IllegalArgumentException("Unknown backup type " + type);
        }
    }

    /**
     * Returns whether an auto backup file is not configured (either because the user did not or the
     * backup task removed a file that had issues).
     */
    public static boolean isMissingAutoBackupFile(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean isMissingBackupFile = false;
        if (prefs.getString(KEY_AUTOBACKUP_SHOWS_EXPORT_URI, null) == null) {
            isMissingBackupFile = true;
        } else if (prefs.getString(KEY_AUTOBACKUP_LISTS_EXPORT_URI, null) == null) {
            isMissingBackupFile = true;
        } else if (prefs.getString(KEY_AUTOBACKUP_MOVIES_EXPORT_URI, null) == null) {
            isMissingBackupFile = true;
        }
        return isMissingBackupFile;
    }
}
