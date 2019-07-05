
package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Access settings specifically related to the calendar views, e.g. upcoming and recent episodes.
 */
public class CalendarSettings {

    static final String KEY_HIDE_WATCHED_EPISODES
            = "com.battlelancer.seriesguide.activity.nowatched";

    static final String KEY_INFINITE_SCROLLING_2
            = "com.battlelancer.seriesguide.calendar.infinite";

    static final String KEY_ONLY_COLLECTED
            = "com.battlelancer.seriesguide.activity.onlycollected";

    static final String KEY_ONLY_FAVORITE_SHOWS
            = "com.battlelancer.seriesguide.onlyfavorites";

    /**
     * Whether the calendar should not include watched episodes.
     */
    public static boolean isHidingWatchedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_HIDE_WATCHED_EPISODES, false);
    }

    /**
     * Whether the calendar should be infinite (default) or limited to a number of days.
     */
    static boolean isInfiniteScrolling(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_INFINITE_SCROLLING_2,
                true);
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
