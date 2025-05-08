// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2025 Uwe Trottmann

package com.battlelancer.seriesguide.lists;

import android.content.Context;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings;
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowSortOrder;

/**
 * Provides settings used to sort displayed list items.
 */
public class ListsDistillationSettings {

    public static class ListsSortOrderChangedEvent {
    }

    public static String KEY_SORT_ORDER = "com.battlelancer.seriesguide.lists.sortorder";

    /**
     * Builds an appropriate SQL sort statement for sorting lists.
     */
    static String getSortQuery(Context context) {
        int sortOrderId = getSortOrderId(context);

        // convert to show sort order id
        switch (sortOrderId) {
            case ListsSortOrder.LATEST_EPISODE_ID:
                sortOrderId = ShowSortOrder.LATEST_EPISODE_ID;
                break;
            case ListsSortOrder.OLDEST_EPISODE_ID:
                sortOrderId = ShowSortOrder.OLDEST_EPISODE_ID;
                break;
            case ListsSortOrder.LAST_WATCHED_ID:
                sortOrderId = ShowSortOrder.LAST_WATCHED_ID;
                break;
            case ListsSortOrder.LEAST_REMAINING_EPISODES_ID:
                sortOrderId = ShowSortOrder.LEAST_REMAINING_EPISODES_ID;
                break;
            default:
                sortOrderId = ShowSortOrder.TITLE_ID;
                break;
        }

        String baseQuery = ShowsDistillationSettings.getSortQuery2(sortOrderId, false,
                DisplaySettings.isSortOrderIgnoringArticles(context));

        // append sorting by list type
        return baseQuery + "," + ListItems.SORT_TYPE;
    }

    /**
     * Returns the id as of {@link ListsDistillationSettings.ListsSortOrder}
     * of the current list sort order.
     */
    public static int getSortOrderId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_SORT_ORDER, ListsSortOrder.TITLE_ALPHABETICAL_ID);
    }

    public interface ListsSortOrder {
        int TITLE_ALPHABETICAL_ID = 0;
        // @deprecated Only supporting alphabetical sort order going forward.
        // int TITLE_REVERSE_ALHPABETICAL_ID = 1;
        int LATEST_EPISODE_ID = 2;
        int OLDEST_EPISODE_ID = 3;
        int LAST_WATCHED_ID = 4;
        int LEAST_REMAINING_EPISODES_ID = 5;
    }
}
