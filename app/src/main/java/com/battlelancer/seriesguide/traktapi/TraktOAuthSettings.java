package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.format.DateUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TraktOAuthSettings {

    private static final String KEY_REFRESH_TOKEN = "trakt.refresh_token";
    private static final String KEY_ACCESS_TOKEN_EXPIRY_DATE = "trakt.access_token_expiry";
    private static final String SETTINGS_FILE = "trakt-oauth-settings";

    /**
     * Returns the refresh token or {@code null} if there is none.
     */
    @Nullable
    static String getRefreshToken(Context context) {
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
