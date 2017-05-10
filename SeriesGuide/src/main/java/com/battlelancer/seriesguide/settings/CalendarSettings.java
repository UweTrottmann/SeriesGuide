
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Access settings specifically related to the calendar views, e.g. upcoming and recent episodes.
 */
public class CalendarSettings {

    public static final String KEY_HIDE_WATCHED_EPISODES
            = "com.battlelancer.seriesguide.activity.nowatched";

    public static final String KEY_INFINITE_SCROLLING
            = "com.battlelancer.seriesguide.activity.infinite";

    public static final String KEY_ONLY_COLLECTED
            = "com.battlelancer.seriesguide.activity.onlycollected";

    public static final String KEY_ONLY_FAVORITE_SHOWS
            = "com.battlelancer.seriesguide.onlyfavorites";

    /**
     * Whether the calendar should not include watched episodes.
     */
    public static boolean isHidingWatchedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_HIDE_WATCHED_EPISODES, false);
    }

    /**
     * Whether the calendar should be infinite or limited to a number of days.
     */
    public static boolean isInfiniteScrolling(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_INFINITE_SCROLLING,
                false);
    }

    /**
     * Whether the calendar should only include collected episodes.
     */
    public static boolean isOnlyCollected(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_ONLY_COLLECTED,
                false);
    }

    /**
     * Whether the calendar should only include episodes from favorite shows.
     */
    public static boolean isOnlyFavorites(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_ONLY_FAVORITE_SHOWS,
                false);
    }
}
