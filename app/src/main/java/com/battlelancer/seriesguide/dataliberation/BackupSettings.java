package com.battlelancer.seriesguide.dataliberation;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Settings related to creating and restoring backups of the database.
 */
public class BackupSettings {

    // manual backup
    public static final String KEY_SHOWS_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.showsExport";
    public static final String KEY_SHOWS_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.showsImport";
    public static final String KEY_LISTS_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.listsExport";
    public static final String KEY_LISTS_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.listsImport";
    public static final String KEY_MOVIES_EXPORT_URI
            = "com.battlelancer.seriesguide.backup.moviesExport";
    public static final String KEY_MOVIES_IMPORT_URI
            = "com.battlelancer.seriesguide.backup.moviesImport";

    // auto backup
    public static final String KEY_AUTOBACKUP
            = "com.battlelancer.seriesguide.autobackup";
    public static final String KEY_AUTO_BACKUP_USE_DEFAULT_FILES
            = "com.battlelancer.seriesguide.autobackup.defaultFiles";
    public static final String KEY_AUTO_BACKUP_SHOWS_EXPORT_URI
            = "com.battlelancer.seriesguide.autobackup.showsExport";
    public static final String KEY_AUTO_BACKUP_LISTS_EXPORT_URI
            = "com.battlelancer.seriesguide.autobackup.listsExport";
    public static final String KEY_AUTO_BACKUP_MOVIES_EXPORT_URI
            = "com.battlelancer.seriesguide.autobackup.moviesExport";

    /**
     * Store some auto backup prefs in separate preference file
     * that is not backed up by Android auto backup.
     */
    private static final String PREFS_AUTOBACKUP = "autobackup";
    private static final String KEY_AUTOBACKUP_LAST_TIME
            = "com.battlelancer.seriesguide.lastbackup";
    private static final String KEY_AUTOBACKUP_LAST_ERROR
            = "com.uwetrottmann.seriesguide.autobackup.lasterror";

    @Retention(RetentionPolicy.SOURCE)
    @StringDef({
            KEY_SHOWS_EXPORT_URI,
            KEY_SHOWS_IMPORT_URI,
            KEY_LISTS_EXPORT_URI,
            KEY_LISTS_IMPORT_URI,
            KEY_MOVIES_EXPORT_URI,
            KEY_MOVIES_IMPORT_URI,
            KEY_AUTO_BACKUP_SHOWS_EXPORT_URI,
            KEY_AUTO_BACKUP_LISTS_EXPORT_URI,
            KEY_AUTO_BACKUP_MOVIES_EXPORT_URI
    })
    public @interface FileUriSettingsKey {
    }

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

    public static boolean isTimeForAutoBackup(Context context) {
        long now = System.currentTimeMillis();
        long previousBackupTime = getLastAutoBackupTime(context);
        return (now - previousBackupTime) > 7 * DateUtils.DAY_IN_MILLIS;
    }

    private static long getLastAutoBackupTime(Context context) {
        SharedPreferences prefs = context
                .getSharedPreferences(PREFS_AUTOBACKUP, Context.MODE_PRIVATE);

        long time = prefs.getLong(KEY_AUTOBACKUP_LAST_TIME, 0);
        if (time == 0) {
            // For new installs set last time to now so backup will not run right away.
            time = System.currentTimeMillis();
            prefs.edit().putLong(KEY_AUTOBACKUP_LAST_TIME, time).apply();
        }

        return time;
    }

    public static void setLastAutoBackupTimeToNow(Context context) {
        context.getSharedPreferences(PREFS_AUTOBACKUP, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_AUTOBACKUP_LAST_TIME, System.currentTimeMillis())
                .apply();
    }

    public static boolean isCreateCopyOfAutoBackup(Context context) {
        return !PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_AUTO_BACKUP_USE_DEFAULT_FILES, true);
    }

    @Nullable
    public static String getAutoBackupErrorOrNull(Context context) {
        return context.getSharedPreferences(PREFS_AUTOBACKUP, Context.MODE_PRIVATE)
                .getString(KEY_AUTOBACKUP_LAST_ERROR, null);
    }

    public static void setAutoBackupErrorOrNull(Context context, String errorOrNull) {
        context.getSharedPreferences(PREFS_AUTOBACKUP, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_AUTOBACKUP_LAST_ERROR, errorOrNull)
                .apply();
    }

    /**
     * Store or remove (by setting it {@code null}) the URI to a backup file.
     */
    static void storeFileUri(Context context, @FileUriSettingsKey String key,
            @Nullable Uri uri) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, uri == null ? null : uri.toString())
                .apply();
    }

    /**
     * Retrieve a backup file URI. If looking for an import URI and there is none defined, tries to
     * get the export URI.
     */
    @Nullable
    static Uri getFileUri(Context context, @FileUriSettingsKey String key) {
        String uriString = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(key, null);
        if (uriString == null) {
            // if there is no import uri, try to fall back to the export file uri
            if (KEY_SHOWS_IMPORT_URI.equals(key)) {
                return getFileUri(context, KEY_SHOWS_EXPORT_URI);
            } else if (KEY_LISTS_IMPORT_URI.equals(key)) {
                return getFileUri(context, KEY_LISTS_EXPORT_URI);
            } else if (KEY_MOVIES_IMPORT_URI.equals(key)) {
                return getFileUri(context, KEY_MOVIES_EXPORT_URI);
            }
            return null;
        }
        return Uri.parse(uriString);
    }

    /**
     * Returns whether an auto backup file is not configured (either because the user did not or the
     * backup task removed a file that had issues).
     */
    public static boolean isMissingAutoBackupFile(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        boolean isMissingBackupFile = false;
        if (prefs.getString(KEY_AUTO_BACKUP_SHOWS_EXPORT_URI, null) == null) {
            isMissingBackupFile = true;
        } else if (prefs.getString(KEY_AUTO_BACKUP_LISTS_EXPORT_URI, null) == null) {
            isMissingBackupFile = true;
        } else if (prefs.getString(KEY_AUTO_BACKUP_MOVIES_EXPORT_URI, null) == null) {
            isMissingBackupFile = true;
        }
        return isMissingBackupFile;
    }
}
