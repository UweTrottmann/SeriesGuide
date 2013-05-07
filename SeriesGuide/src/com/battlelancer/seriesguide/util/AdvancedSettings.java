
package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Access advanced settings for auto backup.
 */
public class AdvancedSettings {

    public static final String KEY_AUTOBACKUP = "com.battlelancer.seriesguide.autobackup";

    public static final String KEY_LASTBACKUP = "com.battlelancer.seriesguide.lastbackup";

    public static boolean isAutoBackupEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(KEY_AUTOBACKUP,
                true);
    }

    public static long getLastAutoBackupTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long time = prefs.getLong(KEY_LASTBACKUP, 0);
        if (time == 0) {
            // use now as default value, so a re-install won't overwrite the old
            // auto-backup right away
            time = System.currentTimeMillis();
            prefs.edit().putLong(KEY_LASTBACKUP, time).commit();
        }

        return time;
    }
}
