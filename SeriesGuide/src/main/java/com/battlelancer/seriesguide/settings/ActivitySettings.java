
/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
