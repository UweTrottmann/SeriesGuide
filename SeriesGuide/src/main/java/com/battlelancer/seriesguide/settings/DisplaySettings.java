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

import com.battlelancer.seriesguide.Constants;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

/**
 * Settings related to appearance, display formats and sort orders.
 */
public class DisplaySettings {

    public static final String KEY_THEME = "com.battlelancer.seriesguide.theme";

    public static final String KEY_LANGUAGE = "language";

    public static final String KEY_NUMBERFORMAT = "numberformat";

    public static final String NUMBERFORMAT_DEFAULT = "default";

    public static final String NUMBERFORMAT_ENGLISHLOWER = "englishlower";

    public static final String KEY_NO_RELEASED_EPISODES = "onlyFutureEpisodes";

    public static final String KEY_NO_WATCHED_EPISODES
            = "com.battlelancer.seriesguide.activity.nowatched";

    public static final String KEY_SEASON_SORT_ORDER = "seasonSorting";

    public static final String KEY_EPISODE_SORT_ORDER = "episodeSorting";

    public static final String KEY_HIDE_SPECIALS = "onlySeasonEpisodes";

    /**
     * Returns true for xlarge, xlarge-land or sw720dp screens.
     */
    public static boolean isVeryLargeScreen(Context context) {
        return context.getResources().getBoolean(R.bool.isLargeTablet);
    }

    /**
     * Returns true for all screens with dpi higher than {@link DisplayMetrics#DENSITY_HIGH}.
     */
    public static boolean isVeryHighDensityScreen(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi > DisplayMetrics.DENSITY_HIGH;
    }

    /**
     * Returns true for xlarge, xlarge-land or sw720dp screens with xhdpi or higher dpi screens.
     */
    public static boolean isVeryLargeAndHighResScreen(Context context) {
        return isVeryLargeScreen(context)
                && context.getResources().getDisplayMetrics().densityDpi
                > DisplayMetrics.DENSITY_HIGH;
    }

    public static String getThemeIndex(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).
                getString(KEY_THEME, "0");
    }

    public static String getContentLanguage(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LANGUAGE, "en");
    }

    public static String getNumberFormat(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_NUMBERFORMAT, NUMBERFORMAT_DEFAULT);
    }

    public static Constants.EpisodeSorting getEpisodeSortOrder(Context context) {
        String orderId = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_EPISODE_SORT_ORDER, Constants.EpisodeSorting.OLDEST_FIRST.value());
        return Constants.EpisodeSorting.fromValue(orderId);
    }

    public static Constants.SeasonSorting getSeasonSortOrder(Context context) {
        String orderId = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SEASON_SORT_ORDER, Constants.SeasonSorting.LATEST_FIRST.value());
        return Constants.SeasonSorting.fromValue(orderId);
    }

    public static boolean isNoReleasedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_NO_RELEASED_EPISODES, false);
    }

    public static boolean isNoWatchedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_NO_WATCHED_EPISODES, false);
    }

    /**
     * Whether to exclude special episodes wherever possible (except in the actual seasons and
     * episode lists of a show).
     */
    public static boolean isHidingSpecials(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                KEY_HIDE_SPECIALS, false);
    }

}
