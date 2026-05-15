// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

@file:Suppress("DEPRECATION") // Ignore warning that AsyncTask should not be used for new code

package com.battlelancer.seriesguide.lists

import android.content.Context
import android.os.AsyncTask
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.tasks.AddListTask
import com.battlelancer.seriesguide.util.tasks.ChangeListItemListsTask
import com.battlelancer.seriesguide.util.tasks.DeleteListTask
import com.battlelancer.seriesguide.util.tasks.RemoveListItemTask
import com.battlelancer.seriesguide.util.tasks.RenameListTask
import com.battlelancer.seriesguide.util.tasks.ReorderListsTask
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem

/**
 * Helper tools for SeriesGuide lists.
 */
object ListsTools {

    interface Query {
        companion object {
            val PROJECTION_LIST_ID = arrayOf(
                SeriesGuideContract.Lists.LIST_ID
            )
            val PROJECTION_LIST = arrayOf(
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME,
                SeriesGuideContract.Lists.ORDER
            )
            const val LIST_ID = 0
            const val NAME = 1
            const val ORDER = 2

            val PROJECTION_LIST_ITEMS = arrayOf(
                SeriesGuideContract.ListItems.LIST_ITEM_ID
            )
            const val LIST_ITEM_ID = 0
        }
    }

    fun addList(context: Context, listName: String) {
        AddListTask(context, listName).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun renameList(context: Context, listId: String, listName: String) {
        RenameListTask(context, listId, listName).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun deleteList(context: Context, listId: String) {
        DeleteListTask(context, listId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun reorderLists(context: Context, listIdsInOrder: List<String>) {
        ReorderListsTask(context, listIdsInOrder).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun changeListsOfItem(
        context: Context,
        itemStableId: Int,
        itemType: Int,
        addToTheseLists: List<String>,
        removeFromTheseLists: List<String>
    ) {
        ChangeListItemListsTask(
            context,
            itemStableId,
            itemType,
            addToTheseLists,
            removeFromTheseLists
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun removeListItem(context: Context, listItemId: String) {
        RemoveListItemTask(context, listItemId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    /**
     * Returns all list ids in the local database.
     *
     * @return null if there was an error, empty list if there are no lists.
     */
    fun getListIds(context: Context): HashSet<String>? {
        val query = context.contentResolver.query(
            SeriesGuideContract.Lists.CONTENT_URI,
            Query.PROJECTION_LIST_ID, null, null, null
        ) ?: return null

        val listIds = HashSet<String>()
        query.use {
            while (it.moveToNext()) {
                listIds.add(it.getString(Query.LIST_ID))
            }
        }
        return listIds
    }

    /**
     * Returns all list item ids of the given list in the local database.
     *
     * @return null if there was an error, empty list if there are no list items in this list.
     */
    fun getListItemIds(context: Context, listId: String): HashSet<String>? {
        val query = context.contentResolver.query(
            SeriesGuideContract.ListItems.CONTENT_URI,
            Query.PROJECTION_LIST_ITEMS,
            SeriesGuideContract.ListItems.SELECTION_LIST,
            arrayOf(listId), null
        ) ?: return null

        val listItemIds = HashSet<String>()
        query.use {
            while (it.moveToNext()) {
                listItemIds.add(it.getString(Query.LIST_ITEM_ID))
            }
        }
        return listItemIds
    }

    fun getListItems(context: Context, listId: String): List<SgListItem>? {
        val query = context.contentResolver.query(
            SeriesGuideContract.ListItems.CONTENT_URI,
            Query.PROJECTION_LIST_ITEMS,
            SeriesGuideContract.ListItems.SELECTION_LIST,
            arrayOf(listId), null
        ) ?: return null // query failed

        if (query.count == 0) {
            query.close()
            return null // no items in this list
        }

        val items = ArrayList<SgListItem>(query.count)
        query.use {
            while (it.moveToNext()) {
                val itemId = it.getString(Query.LIST_ITEM_ID)
                if (itemId.isNullOrEmpty()) continue // skip, no item id
                val item = SgListItem()
                item.listItemId = itemId
                items.add(item)
            }
        }
        return items
    }
}
