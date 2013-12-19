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
