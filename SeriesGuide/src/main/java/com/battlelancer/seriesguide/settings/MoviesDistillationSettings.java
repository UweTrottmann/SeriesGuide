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

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

/**
 * Provides settings used to sort displayed movies in
 * {@link com.battlelancer.seriesguide.ui.MoviesBaseFragment} subclasses.
 */
public class MoviesDistillationSettings {

    public static class MoviesSortOrderChangedEvent {
    }

    public static String KEY_SORT_ORDER = "com.battlelancer.seriesguide.movies.sort.order";

    /**
     * Builds an appropriate SQL sort statement for sorting movies.
     */
    public static String getSortQuery(Context context) {
        int sortOrderId = getSortOrderId(context);

        if (sortOrderId == MoviesSortOrder.TITLE_REVERSE_ALHPABETICAL_ID) {
            if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
                return Movies.SORT_TITLE_REVERSE_ALPHACETICAL_NO_ARTICLE;
            } else {
                return Movies.SORT_TITLE_REVERSE_ALPHACETICAL;
            }
        } else if (sortOrderId == MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID) {
            return Movies.SORT_RELEASE_DATE_NEWEST_FIRST;
        } else if (sortOrderId == MoviesSortOrder.RELEASE_DATE_OLDEST_FIRST_ID) {
            return Movies.SORT_RELEASE_DATE_OLDEST_FIRST;
        }

        if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
            return Movies.SORT_TITLE_ALPHABETICAL_NO_ARTICLE;
        } else {
            return Movies.SORT_TITLE_ALPHABETICAL;
        }
    }

    /**
     * Returns the id as of
     * {@link com.battlelancer.seriesguide.settings.MoviesDistillationSettings.MoviesSortOrder}
     * of the current movie sort order.
     */
    public static int getSortOrderId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_SORT_ORDER, MoviesSortOrder.TITLE_ALPHABETICAL_ID);
    }

    public interface MoviesSortOrder {
        int TITLE_ALPHABETICAL_ID = 0;
        int TITLE_REVERSE_ALHPABETICAL_ID = 1;
        int RELEASE_DATE_NEWEST_FIRST_ID = 2;
        int RELEASE_DATE_OLDEST_FIRST_ID = 3;
    }
}
