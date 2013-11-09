package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;
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
        return !(getAuthToken(context) == "" || getRefreshToken(context) == "");
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
