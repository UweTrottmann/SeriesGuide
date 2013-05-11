
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Access settings related to the activity stream, e.g. upcoming and recent
 * episodes.
 */
public class ActivitySettings {

    public static final String KEY_INFINITE_SCROLLING = "com.battlelancer.seriesguide.activity.infinite";

    public static final String KEY_HIDE_SPECIALS = "onlySeasonEpisodes";

    /**
     * Whether the activity stream should be infinite or limited to a number of
     * days.
     */
    public static boolean isInfiniteScrolling(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_INFINITE_SCROLLING,
                false);
    }

    /**
     * Whether to exclude special episodes wherever possible (except in the
     * actual seasons and episode lists of a show).
     */
    public static boolean isHidingSpecials(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_HIDE_SPECIALS, false);
    }
}
