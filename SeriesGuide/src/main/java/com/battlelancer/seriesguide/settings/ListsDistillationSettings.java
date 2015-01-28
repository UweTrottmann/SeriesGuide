/*
 * Copyright 2015 Uwe Trottmann
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
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

/**
 * Provides settings used to sort displayed list items in {@link com.battlelancer.seriesguide.ui.ListsFragment}.
 */
public class ListsDistillationSettings {

    public static class ListsSortOrderChangedEvent {
    }

    public static String KEY_SORT_ORDER = "com.battlelancer.seriesguide.lists.sortorder";

    /**
     * Builds an appropriate SQL sort statement for sorting lists.
     */
    public static String getSortQuery(Context context) {
        int sortOrderId = getSortOrderId(context);

        if (sortOrderId == ListsSortOrder.TITLE_REVERSE_ALHPABETICAL_ID) {
            if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
                return SeriesGuideContract.ListItems.SORT_TITLE_NOARTICLE_REVERSE;
            } else {
                return SeriesGuideContract.ListItems.SORT_TITLE_REVERSE;
            }
        }
        if (sortOrderId == ListsSortOrder.NEWEST_EPISODE_FIRST_ID) {
            return SeriesGuideContract.ListItems.SORT_NEWEST_EPISODE_FIRST;
        }
        if (sortOrderId == ListsSortOrder.OLDEST_EPISODE_FIRST_ID) {
            return SeriesGuideContract.ListItems.SORT_OLDEST_EPISODE_FIRST;
        }

        if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
            return SeriesGuideContract.ListItems.SORT_TITLE_NOARTICLE;
        } else {
            return SeriesGuideContract.ListItems.SORT_TITLE;
        }
    }

    /**
     * Returns the id as of {@link com.battlelancer.seriesguide.settings.ListsDistillationSettings.ListsSortOrder}
     * of the current list sort order.
     */
    public static int getSortOrderId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_SORT_ORDER, ListsSortOrder.TITLE_ALPHABETICAL_ID);
    }

    public interface ListsSortOrder {
        int TITLE_ALPHABETICAL_ID = 0;
        int TITLE_REVERSE_ALHPABETICAL_ID = 1;
        int NEWEST_EPISODE_FIRST_ID = 2;
        int OLDEST_EPISODE_FIRST_ID = 3;
    }
}
