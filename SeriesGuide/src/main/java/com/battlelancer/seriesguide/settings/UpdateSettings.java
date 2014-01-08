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
