package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

public class GetGlueSettings {

    public static final String KEY_AUTH_TOKEN = "com.battlelancer.seriesguide.getglue.authtoken";

    public static final String KEY_AUTH_EXPIRATION = "com.battlelancer.seriesguide.getglue.authexpiration";

    public static final String KEY_REFRESH_TOKEN = "com.battlelancer.seriesguide.getglue.refreshtoken";

    public static String getAuthToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_AUTH_TOKEN, "");
    }

    public static String getRefreshToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_REFRESH_TOKEN, "");
    }

    public static boolean isAuthenticated(Context context) {
        return !(getAuthToken(context) == "" || getRefreshToken(context) == "");
    }

    public static boolean isAuthTokenExpired(Context context) {
        long now = System.currentTimeMillis();
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_AUTH_EXPIRATION, now) <= now;
    }

    public static void clearTokens(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_AUTH_TOKEN, "")
                .putLong(KEY_AUTH_EXPIRATION, 0)
                .putString(KEY_REFRESH_TOKEN, "")
                .commit();
    }
}
