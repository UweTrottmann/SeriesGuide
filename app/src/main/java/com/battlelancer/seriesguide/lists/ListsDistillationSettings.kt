// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails
import com.battlelancer.seriesguide.settings.DisplaySettings

/**
 * Provides settings used to sort displayed list items.
 */
object ListsDistillationSettings {

    class ListsSortOrderChangedEvent

    private const val KEY_SORT_ORDER = "com.battlelancer.seriesguide.lists.sortorder"

    /**
     * Builds an appropriate SQL sort statement for sorting [SgListItemWithDetails] rows.
     */
    fun getSortQuery(context: Context): String {
        val query = StringBuilder()

        val sortOrder = getSortOrder(context)
        val orderClause: String = when (sortOrder) {
            ListsSortOrder.TITLE_ALPHABETICAL ->
                if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
                    SgListItemWithDetails.SORT_TITLE_NO_ARTICLE
                } else {
                    SgListItemWithDetails.SORT_TITLE
                }

            ListsSortOrder.LATEST_EPISODE -> SgListItemWithDetails.SORT_LATEST_RELEASE_DATE
            ListsSortOrder.OLDEST_EPISODE -> SgListItemWithDetails.SORT_OLDEST_RELEASE_DATE
            ListsSortOrder.LAST_WATCHED -> SgListItemWithDetails.SORT_LAST_WATCHED
            ListsSortOrder.LEAST_REMAINING_EPISODES -> SgListItemWithDetails.SORT_REMAINING_EPISODES
        }
        query.append(orderClause)

        // Then by type
        query.append(",").append(SgListItemWithDetails.SORT_TYPE)

        return query.toString()
    }

    fun saveSortOrder(context: Context, sortOrder: ListsSortOrder) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putInt(KEY_SORT_ORDER, sortOrder.id)
        }
    }

    /**
     * Returns the current [ListsSortOrder], defaulting to [ListsSortOrder.TITLE_ALPHABETICAL]
     * for unknown stored values.
     */
    fun getSortOrder(context: Context): ListsSortOrder {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_SORT_ORDER, ListsSortOrder.TITLE_ALPHABETICAL.id)
            .let { ListsSortOrder.fromId(it) }
    }

    enum class ListsSortOrder(val id: Int) {
        TITLE_ALPHABETICAL(0),

        // Deprecated: Only supporting alphabetical sort order going forward.
        // TITLE_REVERSE_ALPHABETICAL(1),

        LATEST_EPISODE(2),
        OLDEST_EPISODE(3),
        LAST_WATCHED(4),
        LEAST_REMAINING_EPISODES(5);

        companion object {
            fun fromId(id: Int): ListsSortOrder =
                entries.find { it.id == id } ?: TITLE_ALPHABETICAL
        }
    }
}
