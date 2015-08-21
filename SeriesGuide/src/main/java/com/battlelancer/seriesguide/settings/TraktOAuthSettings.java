/*
 * Copyright 2015 Uwe Trottmann
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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

public class TraktOAuthSettings {

    private static final String KEY_REFRESH_TOKEN = "trakt.refresh_token";
    private static final String KEY_ACCESS_TOKEN_EXPIRY_DATE = "trakt.access_token_expiry";
    private static final String SETTINGS_FILE = "trakt-oauth-settings";
    private static final long REFRESH_THRESHOLD = DateUtils.DAY_IN_MILLIS;

    /**
     * Checks if the access token is about to expire. If so returns {@code true}.
     *
     * <p><b>Note:</b> If there is no expiry date, will return {@code false}.
     */
    public static boolean isTimeToRefreshAccessToken(Context context) {
        long expiryDate = getSettings(context).getLong(KEY_ACCESS_TOKEN_EXPIRY_DATE, 0);
        return expiryDate != 0 && expiryDate - REFRESH_THRESHOLD < System.currentTimeMillis();
    }

    /**
     * Returns the refresh token or {@code null} if there is none.
     */
    @Nullable
    public static String getRefreshToken(Context context) {
        return getSettings(context).getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * @param refreshToken The trakt refresh token.
     * @param expiresIn The trakt access token expires duration in seconds.
     * @return Returns true if the new values were successfully written to persistent storage.
     */
    public static boolean storeRefreshData(Context context, @NonNull String refreshToken,
            long expiresIn) {
        return getSettings(context).edit()
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_ACCESS_TOKEN_EXPIRY_DATE,
                        System.currentTimeMillis() + expiresIn * DateUtils.SECOND_IN_MILLIS)
                .commit();
    }

    private static SharedPreferences getSettings(Context context) {
        return context.getSharedPreferences(SETTINGS_FILE, Context.MODE_PRIVATE);
    }
}
