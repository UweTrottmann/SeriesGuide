
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;

public class AppSettings {

    public static final String KEY_VERSION = "oldversioncode";

    public static final String KEY_GOOGLEANALYTICS = "enableGAnalytics";

    /**
     * Returns the version code of the previously installed version. Is the
     * current version on fresh installs.
     */
    public static int getLastVersionCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int lastVersionCode = prefs.getInt(KEY_VERSION, -1);
        if (lastVersionCode == -1) {
            try {
                // set current version as default value
                lastVersionCode = context.getPackageManager().getPackageInfo(
                        context.getPackageName(),
                        PackageManager.GET_META_DATA).versionCode;
                prefs.edit().putInt(KEY_VERSION, lastVersionCode).commit();
            } catch (NameNotFoundException e) {
                // should not happen, it's this package we are looking for
            }
        }

        return lastVersionCode;
    }

    public static boolean isGaAppOptOut(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_GOOGLEANALYTICS, false);
    }
}
