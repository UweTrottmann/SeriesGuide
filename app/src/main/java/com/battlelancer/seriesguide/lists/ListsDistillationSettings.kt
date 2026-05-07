// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright 2015-2025 Uwe Trottmann

package com.battlelancer.seriesguide.lists

import android.content.Context
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails
import com.battlelancer.seriesguide.settings.DisplaySettings

/**
 * Provides settings used to sort displayed list items.
 */
object ListsDistillationSettings {

    class ListsSortOrderChangedEvent

    const val KEY_SORT_ORDER = "com.battlelancer.seriesguide.lists.sortorder"

    /**
     * Builds an appropriate SQL sort statement for sorting [SgListItemWithDetails] rows.
     */
    fun getSortQuery(context: Context): String {
        val query = StringBuilder()

        // TODO Support new sort order options for lists
        val sortOrderId = getSortOrderId(context)

        // Sort by title
        query.append(
            if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
                SgListItemWithDetails.SORT_TITLE_NO_ARTICLE
            } else {
                SgListItemWithDetails.SORT_TITLE
            }
        )

        // Then by type
        query.append(",").append(SgListItemWithDetails.SORT_TYPE)

        return query.toString()
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
