/*
 * Copyright 2013 Uwe Trottmann
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
 *
 */

package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Provides settings used to filter displayed shows in
 * {@link com.battlelancer.seriesguide.ui.ShowsFragment}.
 */
public class ShowFilterSettings {

    public static String KEY_FILTER_FAVORITES = "com.battlelancer.seriesguide.filter.favorites";
    public static String KEY_FILTER_UNWATCHED = "com.battlelancer.seriesguide.filter.unwatched";
    public static String KEY_FILTER_UPCOMING = "com.battlelancer.seriesguide.filter.upcoming";
    public static String KEY_FILTER_HIDDEN = "com.battlelancer.seriesguide.filter.hidden";

    public static boolean isFilteringFavorites(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILTER_FAVORITES, false);
    }

    public static boolean isFilteringUnwatched(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILTER_UNWATCHED, false);
    }

    public static boolean isFilteringUpcoming(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILTER_UPCOMING, false);
    }

    public static boolean isFilteringHidden(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_FILTER_HIDDEN, false);
    }
}
