// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.seriesguide.backend.lists.model.SgList
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList
import java.io.IOException

/**
 * Task to remove an item from a single list (basically delete the list item).
 *
 * If a [movieTmdbId] is given, will also delete that movie from the database if it isn't on any
 * other custom list or built-in list.
 */
class RemoveListItemTask(
    context: Context,
    private val listItemId: String,
    private val movieTmdbId: Int?
) : BaseActionTask(context) {

    override val isSendingToTrakt: Boolean = false

    override fun doBackgroundAction(vararg params: Void?): Int {
        if (isSendingToHexagon) {
            val hexagonTools = SgApp.getServicesComponent(context).hexagonTools()
            val listsService = hexagonTools.listsService
                ?: return ERROR_HEXAGON_API // no longer signed in

            // extract the list id of this list item
            val splitListItemId = ListItems.splitListItemId(listItemId)
                ?: return ERROR_DATABASE
            val removeFromListId = splitListItemId[2]

            // send the item to be removed from hexagon
            val wrapper = SgListList()
            val lists = buildListItemLists(removeFromListId, listItemId)
            wrapper.setLists(lists)
            try {
                listsService.removeItems(wrapper).execute()
            } catch (e: IOException) {
                Errors.logAndReportHexagon("remove list item", e)
                return ERROR_HEXAGON_API
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE
        }

        return SUCCESS
    }

    private fun buildListItemLists(listId: String, listItemId: String): List<SgList> {
        val list = SgList().also { list ->
            list.setListId(listId)

            val item = SgListItem()
                .also { it.setListItemId(listItemId) }
            list.setListItems(listOf(item))
        }
        return listOf(list)
    }

    private fun doDatabaseUpdate(): Boolean {
        val deleted = context.contentResolver
            .delete(ListItems.buildListItemUri(listItemId), null, null)
        if (deleted == 0) {
            return false // if 0 nothing got deleted
        }

        // For a movie, also delete it from the database if it is no longer on any custom or
        // built-in list.
        if (movieTmdbId != null) {
            return SgApp.getServicesComponent(context).movieTools()
                .deleteFromDatabaseIfNotOnBuiltInList(movieTmdbId)
        }

        return true
    }

    override val successTextResId: Int
        get() = if (isSendingToHexagon) R.string.ack_list_item_removed else 0

}
