package com.battlelancer.seriesguide.backend.settings;

import android.content.Context;
import android.preference.PreferenceManager;

public class HexagonSettings {

    public static final String AUDIENCE = "server:client_id:137959300653-9pg0ulu5d3d6jhm4fotn2onk789vsob7.apps.googleusercontent.com";

    public static final String KEY_ACCOUNT_NAME = "com.battlelancer.seriesguide.accountname";

    /**
     * Returns the account name used for authenticating against Hexagon.
     */
    public static String getAccountName(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_ACCOUNT_NAME, null);
    }

}
