
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Access advanced settings for auto backup and auto update.
 */
public class AdvancedSettings {

    private static final String KEY_LAST_SUPPORTER_STATE
            = "com.battlelancer.seriesguide.lastupgradestate";

    public static final String KEY_UPCOMING_LIMIT = "com.battlelancer.seriesguide.upcominglimit";

    /**
     * (Only Amazon version) Returns if the user was a supporter through an in-app purchase
     * the last time we checked.
     */
    public static boolean getLastSupporterState(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_LAST_SUPPORTER_STATE, false);
    }

    /**
     * (Only Amazon version) Set if the user currently has an active purchase to support the app.
     */
    public static void setSupporterState(Context context, boolean isSubscribedToX) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(
                KEY_LAST_SUPPORTER_STATE, isSubscribedToX).apply();
    }

    /**
     * Returns the maximum number of days from today on an episode can air for its show to be
     * considered as upcoming.
     */
    public static int getUpcomingLimitInDays(Context context) {
        int upcomingLimit = 3;
        try {
            upcomingLimit = Integer.parseInt(PreferenceManager
                    .getDefaultSharedPreferences(context).getString(
                            KEY_UPCOMING_LIMIT, "3"));
        } catch (NumberFormatException ignored) {
        }

        return upcomingLimit;
    }
}
