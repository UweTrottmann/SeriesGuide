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

package com.battlelancer.seriesguide.backend.settings;

import android.content.Context;
import android.preference.PreferenceManager;

public class HexagonSettings {

    public static final String AUDIENCE
            = "server:client_id:137959300653-9pg0ulu5d3d6jhm4fotn2onk789vsob7.apps.googleusercontent.com";

    public static final String KEY_ACCOUNT_NAME
            = "com.battlelancer.seriesguide.hexagon.accountname";

    public static final String KEY_SETUP_COMPLETED
            = "com.battlelancer.seriesguide.hexagon.setup_complete";

    /**
     * Returns the account name used for authenticating against Hexagon.
     */
    public static String getAccountName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_ACCOUNT_NAME, null);
    }

    /**
     * Whether the Hexagon setup has been completed after the last sign in.
     */
    public static boolean hasCompletedSetup(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SETUP_COMPLETED, true);
    }

    public static void setSetupCompleted(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(HexagonSettings.KEY_SETUP_COMPLETED, true).commit();
    }

    public static void setSetupIncomplete(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(HexagonSettings.KEY_SETUP_COMPLETED, false).commit();
    }
}
