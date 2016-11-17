
package com.battlelancer.seriesguide.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
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

    public static final String KEY_LAST_STATS_REPORT = "timeLastStatsReport";

    public static final String KEY_ASKED_FOR_FEEDBACK = "askedForFeedback";

    /**
     * Returns the version code of the previously installed version. Is the current version on fresh
     * installs.
     */
    @SuppressLint("CommitPrefEdits")
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

    /**
     * Whether to report stats, or if this was done today already.
     */
    public static boolean shouldReportStats(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long currentTime = System.currentTimeMillis();
        long lastReportTime = prefs.getLong(KEY_LAST_STATS_REPORT, 0);
        boolean shouldReport = lastReportTime + 30 * DateUtils.DAY_IN_MILLIS < currentTime;

        if (shouldReport) {
            // reset report time
            prefs.edit().putLong(KEY_LAST_STATS_REPORT, currentTime).apply();
        }

        return shouldReport;
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
