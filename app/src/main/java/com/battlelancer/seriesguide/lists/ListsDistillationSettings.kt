// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2015-2025 Uwe Trottmann

package com.battlelancer.seriesguide.lists

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowSortOrder

/**
 * Provides settings used to sort displayed list items.
 */
object ListsDistillationSettings {

    class ListsSortOrderChangedEvent

    const val KEY_SORT_ORDER = "com.battlelancer.seriesguide.lists.sortorder"

    /**
     * Builds an appropriate SQL sort statement for sorting lists.
     */
    fun getSortQuery(context: Context): String {
        val sortOrderId = when (getSortOrderId(context)) {
            ListsSortOrder.LATEST_EPISODE_ID -> ShowSortOrder.LATEST_EPISODE_ID
            ListsSortOrder.OLDEST_EPISODE_ID -> ShowSortOrder.OLDEST_EPISODE_ID
            ListsSortOrder.LAST_WATCHED_ID -> ShowSortOrder.LAST_WATCHED_ID
            ListsSortOrder.LEAST_REMAINING_EPISODES_ID -> ShowSortOrder.LEAST_REMAINING_EPISODES_ID
            else -> ShowSortOrder.TITLE_ID
        }

        val baseQuery = ShowsDistillationSettings.getSortQuery2(
            sortOrderId,
            isSortFavoritesFirst = false,
            DisplaySettings.isSortOrderIgnoringArticles(context)
        )

        // append sorting by list type
        return "$baseQuery,${SgListItemWithDetails.SORT_TYPE}"
    }

    /**
     * Returns the id as of [ListsDistillationSettings.ListsSortOrder]
     * of the current list sort order.
     */
    fun getSortOrderId(context: Context): Int {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_SORT_ORDER, ListsSortOrder.TITLE_ALPHABETICAL_ID)
    }

    object ListsSortOrder {
        const val TITLE_ALPHABETICAL_ID = 0
        // @deprecated Only supporting alphabetical sort order going forward.
        // const val TITLE_REVERSE_ALPHABETICAL_ID = 1
        const val LATEST_EPISODE_ID = 2
        const val OLDEST_EPISODE_ID = 3
        const val LAST_WATCHED_ID = 4
        const val LEAST_REMAINING_EPISODES_ID = 5
    }
}
