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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;

public class GetGlueSettings {

    public static final String KEY_AUTH_TOKEN = "com.battlelancer.seriesguide.getglue.authtoken";

    public static final String KEY_AUTH_EXPIRATION
            = "com.battlelancer.seriesguide.getglue.authexpiration";

    public static final String KEY_REFRESH_TOKEN
            = "com.battlelancer.seriesguide.getglue.refreshtoken";

    public static final String KEY_SHARE_WITH_GETGLUE
            = "com.battlelancer.seriesguide.sharewithgetglue";

    public static String getAuthToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_AUTH_TOKEN, "");
    }

    public static String getRefreshToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_REFRESH_TOKEN, "");
    }

    public static boolean isAuthenticated(Context context) {
        return !(TextUtils.isEmpty(getAuthToken(context))
                || TextUtils.isEmpty(getRefreshToken(context)));
    }

    /**
     * Returns true if the expired date of the current access token is within a day or later.
     */
    public static boolean isAuthTokenExpired(Context context) {
        long now = System.currentTimeMillis();
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_AUTH_EXPIRATION, now) <= now - DateUtils.DAY_IN_MILLIS;
    }

    public static void clearTokens(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_AUTH_TOKEN, "")
                .putLong(KEY_AUTH_EXPIRATION, 0)
                .putString(KEY_REFRESH_TOKEN, "")
                .commit();
    }

    public static boolean isSharingWithGetGlue(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SHARE_WITH_GETGLUE, false);
    }
}
