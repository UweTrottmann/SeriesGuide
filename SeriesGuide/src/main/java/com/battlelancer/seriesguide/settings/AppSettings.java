
package com.battlelancer.seriesguide.settings;

import com.uwetrottmann.seriesguide.BuildConfig;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class AppSettings {

    public static final String KEY_VERSION = "oldversioncode";

    public static final String KEY_GOOGLEANALYTICS = "enableGAnalytics";

    /**
     * Returns the version code of the previously installed version. Is the current version on fresh
     * installs.
     */
    public static int getLastVersionCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int lastVersionCode = prefs.getInt(KEY_VERSION, -1);
        if (lastVersionCode == -1) {
            // set current version as default value
            lastVersionCode = BuildConfig.VERSION_CODE;
            prefs.edit().putInt(KEY_VERSION, lastVersionCode).commit();
        }

        return lastVersionCode;
    }

    public static boolean isGaAppOptOut(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_GOOGLEANALYTICS, false);
    }
}
