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

import com.battlelancer.seriesguide.Constants;

import static com.battlelancer.seriesguide.provider.SeriesContract.Shows;

/**
 * Provides settings used to filter and sort displayed shows in
 * {@link com.battlelancer.seriesguide.ui.ShowsFragment}.
 */
public class ShowsDistillationSettings {

    public static String KEY_SORT_ORDER = "com.battlelancer.seriesguide.sort.order";
    public static String KEY_SORT_FAVORITES_FIRST = "com.battlelancer.seriesguide.sort.favoritesfirst";
    public static String KEY_FILTER_FAVORITES = "com.battlelancer.seriesguide.filter.favorites";
    public static String KEY_FILTER_UNWATCHED = "com.battlelancer.seriesguide.filter.unwatched";
    public static String KEY_FILTER_UPCOMING = "com.battlelancer.seriesguide.filter.upcoming";
    public static String KEY_FILTER_HIDDEN = "com.battlelancer.seriesguide.filter.hidden";

    /**
     * Builds an appropriate SQL sort statement for sorting shows.
     */
    public static String getSortQuery(int sortOrderId, boolean isSortFavoritesFirst) {
        StringBuilder query = new StringBuilder();

        if (isSortFavoritesFirst) {
            query.append(ShowsSortOrder.FAVORITES_FIRST_PREFIX);
        }

        if (sortOrderId == ShowsSortOrder.TITLE_REVERSE_ID) {
            query.append(ShowsSortOrder.TITLE_REVERSE);
        } else if (sortOrderId == ShowsSortOrder.EPISODE_ID) {
            query.append(ShowsSortOrder.EPISODE);
        } else if (sortOrderId == ShowsSortOrder.EPISODE_REVERSE_ID) {
            query.append(ShowsSortOrder.EPISODE_REVERSE);
        } else {
            query.append(ShowsSortOrder.TITLE);
        }

        return query.toString();
    }

    /**
     * Returns the id as of
     * {@link com.battlelancer.seriesguide.settings.ShowsDistillationSettings.ShowsSortOrder}
     * of the current show sort order.
     */
    public static int getSortOrderId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_SORT_ORDER, 0);
    }

    public static boolean isSortFavoritesFirst(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SORT_FAVORITES_FIRST, true);
    }

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

    /**
     * Used by {@link com.battlelancer.seriesguide.ui.ShowsFragment} loader to sort the list of
     * shows.
     */
    public interface ShowsSortOrder {
        // alphabetical by title
        String TITLE = Shows.TITLE + " COLLATE NOCASE ASC";
        // reverse alphabetical by title
        String TITLE_REVERSE = Shows.TITLE + " COLLATE NOCASE DESC";
        // by next episode air time, oldest first
        String EPISODE = Shows.NEXTAIRDATEMS + " ASC," + Shows.AIRSTIME + " ASC,"
                + Shows.TITLE + " COLLATE NOCASE ASC";
        // by next episode air time, newest first
        String EPISODE_REVERSE = Shows.NEXTAIRDATEMS + " DESC," + Shows.AIRSTIME + " ASC,"
                + Shows.TITLE + " COLLATE NOCASE ASC";
        // add as prefix to sort favorites first
        String FAVORITES_FIRST_PREFIX = Shows.FAVORITE + " DESC,";
        // ids used for storing in preferences
        int TITLE_ID = 0;
        int TITLE_REVERSE_ID = 1;
        int EPISODE_ID = 2;
        int EPISODE_REVERSE_ID = 3;
    }
}
