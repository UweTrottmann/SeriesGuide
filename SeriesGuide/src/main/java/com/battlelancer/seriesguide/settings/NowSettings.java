package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Settings related to the Now tab.
 */
public class NowSettings {

    public static final String KEY_DISPLAY_RELEASED_TODAY
            = "com.battlelancer.seriesguide.now.releasedtoday";

    /**
     * Whether the Now tab should display episodes released today.
     */
    public static boolean isDisplayingReleasedToday(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_DISPLAY_RELEASED_TODAY, true);
    }
}
