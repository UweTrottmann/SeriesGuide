package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

public class GetGlueSettings {

    public static final String KEY_AUTH_TOKEN = "com.battlelancer.seriesguide.getglue.authtoken";

    public static final String KEY_AUTH_EXPIRATION = "com.battlelancer.seriesguide.getglue.authexpiration";

    public static final String KEY_REFRESH_TOKEN = "com.battlelancer.seriesguide.getglue.refreshtoken";

    public static String getAuthToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_AUTH_TOKEN, null);
    }

    public static long getAuthTokenExpiration(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_AUTH_EXPIRATION, System.currentTimeMillis());
    }

    public static String getRefreshToken(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_REFRESH_TOKEN, null);
    }



}
