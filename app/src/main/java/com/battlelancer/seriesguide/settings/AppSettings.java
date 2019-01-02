
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import timber.log.Timber;

public class AppSettings {

    public static final String KEY_VERSION = "oldversioncode";

    public static final String KEY_GOOGLEANALYTICS = "enableGAnalytics";

    @SuppressWarnings("unused") @Deprecated
    public static final String KEY_HAS_SEEN_NAV_DRAWER = "hasSeenNavDrawer";

    public static final String KEY_ASKED_FOR_FEEDBACK = "askedForFeedback";

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
            prefs.edit().putInt(KEY_VERSION, lastVersionCode).apply();
        }

        return lastVersionCode;
    }

    public static boolean isGaEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_GOOGLEANALYTICS, true);
    }

    public static boolean shouldAskForFeedback(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean(KEY_ASKED_FOR_FEEDBACK, false)) {
            return false; // already asked for feedback
        }

        try {
            PackageInfo ourPackageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            boolean installedRecently = System.currentTimeMillis()
                    < ourPackageInfo.firstInstallTime + 30 * DateUtils.DAY_IN_MILLIS;
            if (installedRecently) {
                return false; // was only installed recently
            }
        } catch (PackageManager.NameNotFoundException e) {
            Timber.e(e, "Failed to find our package info.");
            return false; // failed to find our package
        }

        int showsCount = DBUtils.getCountOf(context.getContentResolver(),
                SeriesGuideContract.Shows.CONTENT_URI, null, null, -1);

        return showsCount >= 5; // only if 5+ shows are added
    }

    public static void setAskedForFeedback(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_ASKED_FOR_FEEDBACK, true)
                .apply();
    }
}
