
package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Access settings specifically related to the activity stream, e.g. upcoming and recent episodes.
 */
public class ActivitySettings {

    // Only applies to activity stream
    public static final String KEY_ACTIVITYTAB = "com.battlelancer.seriesguide.activitytab";

    public static final String KEY_INFINITE_ACTIVITY
            = "com.battlelancer.seriesguide.activity.infinite";

    public static final String KEY_ONLY_FAVORITE_SHOWS
            = "com.battlelancer.seriesguide.onlyfavorites";

    public static int getDefaultActivityTabPosition(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_ACTIVITYTAB, 0);
    }

    /**
     * Whether the activity stream should be infinite or limited to a number of days.
     */
    public static boolean isInfiniteActivity(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_INFINITE_ACTIVITY,
                false);
    }

    /**
     * Whether the activity stream should only include episodes from favorited shows.
     */
    public static boolean isOnlyFavorites(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_ONLY_FAVORITE_SHOWS,
                false);
    }

}
