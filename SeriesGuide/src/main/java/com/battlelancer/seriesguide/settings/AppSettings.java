
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.BuildConfig;

public class AppSettings {

    public static final String KEY_VERSION = "oldversioncode";

    public static final String KEY_GOOGLEANALYTICS = "enableGAnalytics";

    public static final String KEY_HAS_SEEN_NAV_DRAWER = "hasSeenNavDrawer";

    public static final String KEY_LAST_STATS_REPORT = "timeLastStatsReport";

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
     * Whether the user was shown the nav drawer for this app.
     */
    public static boolean hasSeenNavDrawer(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_HAS_SEEN_NAV_DRAWER, false);
    }

    /**
     * Whether to report stats, or if this was done today already.
     */
    @SuppressLint("CommitPrefEdits")
    public static boolean shouldReportStats(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        long currentTime = System.currentTimeMillis();
        long lastReportTime = prefs.getLong(KEY_LAST_STATS_REPORT, 0);
        boolean shouldReport = lastReportTime + DateUtils.DAY_IN_MILLIS < currentTime;

        if (shouldReport) {
            // reset report time
            prefs.edit().putLong(KEY_LAST_STATS_REPORT, currentTime).commit();
        }

        return shouldReport;
    }
}
