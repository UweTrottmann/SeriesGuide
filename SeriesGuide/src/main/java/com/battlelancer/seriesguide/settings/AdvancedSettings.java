
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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Access advanced settings for auto backup and auto update.
 */
public class AdvancedSettings {

    public static final String KEY_AUTOBACKUP = "com.battlelancer.seriesguide.autobackup";

    public static final String KEY_LASTBACKUP = "com.battlelancer.seriesguide.lastbackup";

    public static final String KEY_LAST_UPGRADE_STATE = "com.battlelancer.seriesguide.lastupgradestate";

    public static final String KEY_UPCOMING_LIMIT = "com.battlelancer.seriesguide.upcominglimit";

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

    /**
     * Wether the user is qualified for the X upgrade since the last time we
     * checked.
     */
    public static boolean isSubscribedToX(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_LAST_UPGRADE_STATE, false);
    }

    public static void setSubscriptionState(Context context, boolean isSubscribedToX) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(
                KEY_LAST_UPGRADE_STATE, isSubscribedToX).commit();
    }

    /**
     * Returns the maximum number of days from today on an episode can air for its show to be
     * considered as upcoming.
     */
    public static int getUpcomingLimitInDays(Context context) {
        int upcomingLimit = 3;
        try {
            upcomingLimit = Integer.parseInt(PreferenceManager
                    .getDefaultSharedPreferences(context).getString(
                            KEY_UPCOMING_LIMIT, "3"));
        } catch (NumberFormatException ignored) {
        }

        return upcomingLimit;
    }
}
