package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class UpdateSettings {

    public static final String KEY_AUTOUPDATE = "com.battlelancer.seriesguide.autoupdate";

    public static final String KEY_ONLYWIFI = "com.battlelancer.seriesguide.autoupdatewlanonly";

    public static final String KEY_LASTUPDATE = "com.battlelancer.seriesguide.lastupdate";

    public static final String KEY_FAILED_COUNTER = "com.battlelancer.seriesguide.failedcounter";

    /**
     * Whether the user wants us to download larger chunks of data (e.g. images) only over a Wi-Fi
     * connection.
     */
    public static boolean isLargeDataOverWifiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_ONLYWIFI, false
        );
    }

    public static long getLastAutoUpdateTime(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long time = prefs.getLong(KEY_LASTUPDATE, 0);
        if (time == 0) {
            // use now as default value, so auto-update will not run on first
            // launch
            time = System.currentTimeMillis();
            prefs.edit().putLong(KEY_LASTUPDATE, time).commit();
        }

        return time;
    }

    public static int getFailedNumberOfUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_FAILED_COUNTER, 0);
    }

}
