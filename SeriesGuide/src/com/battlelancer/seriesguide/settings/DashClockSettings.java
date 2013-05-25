
package com.battlelancer.seriesguide.settings;

import android.content.Context;

/**
 * Access DashClock related settings.
 */
public class DashClockSettings {

    public static final String SETTINGS_FILE = "DashClockPreferences";

    public static final String KEY_DASHCLOCK_THRESHOLD = "upcoming_threshold";

    /**
     * How far into the future to include upcoming episodes in hours.
     */
    public static int getUpcomingTreshold(Context context) {
        int threshold = 0;
        try {
            threshold = Integer.parseInt(context.getSharedPreferences(SETTINGS_FILE, 0)
                    .getString(KEY_DASHCLOCK_THRESHOLD, "12"));
        } catch (NumberFormatException ignored) {
        }

        return threshold;
    }

}
