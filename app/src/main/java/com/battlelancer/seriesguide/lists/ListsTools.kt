// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

@file:Suppress("DEPRECATION") // Ignore warning that AsyncTask should not be used for new code

package com.battlelancer.seriesguide.lists

import android.content.Context
import android.os.AsyncTask
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.lists.database.SgListItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.tasks.AddListTask
import com.battlelancer.seriesguide.util.tasks.ChangeListItemListsTask
import com.battlelancer.seriesguide.util.tasks.DeleteListTask
import com.battlelancer.seriesguide.util.tasks.RemoveListItemTask
import com.battlelancer.seriesguide.util.tasks.RenameListTask
import com.battlelancer.seriesguide.util.tasks.ReorderListsTask
import com.google.api.client.http.HttpResponseException
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList
import timber.log.Timber
import java.io.IOException
import com.uwetrottmann.seriesguide.backend.lists.model.SgList as SgCloudList
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem as SgCloudListItem

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
        itemTmdbId: Int,
        @ListItemTypes itemType: Int,
        addToTheseLists: List<String>,
        removeFromTheseLists: List<String>
    ) {
        ChangeListItemListsTask(
            context,
            itemTmdbId,
            itemType,
            addToTheseLists,
            removeFromTheseLists
        ).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    fun removeListItem(context: Context, listItemId: String, movieTmdbId: Int?) {
        RemoveListItemTask(context, listItemId, movieTmdbId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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

    fun getListItems(context: Context, listId: String): List<SgCloudListItem>? {
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

        val items = ArrayList<SgCloudListItem>(query.count)
        query.use {
            while (it.moveToNext()) {
                val itemId = it.getString(Query.LIST_ITEM_ID)
                if (itemId.isNullOrEmpty()) continue // skip, no item id
                val item = SgCloudListItem()
                item.listItemId = itemId
                items.add(item)
            }
        }
        return items
    }

    @JvmStatic
    fun migrateTvdbShowListItemsToTmdbIds(context: Context) {
        val database = SgRoomDatabase.getInstance(context)

        val tvdbShowListItems = database.sgListHelper().getTvdbShowListItems()
        if (tvdbShowListItems.isEmpty()) return

        // try to find TMDB ID for each show
        val toRemove = mutableListOf<String>()
        val toInsert = mutableListOf<SgListItem>()
        val toRemoveCloud = mutableListOf<SgCloudList>()
        val toInsertCloud = mutableListOf<SgCloudList>()
        tvdbShowListItems.forEach { oldItem ->
            val tvdbId = oldItem.itemRefId.toInt()
            val tmdbIdOrZero = database.sgShow2Helper().getShowTmdbIdByTvdbId(tvdbId)
            if (tmdbIdOrZero != 0) {
                // Database changes
                toRemove.add(oldItem.listItemId)

                // Note: do not re-insert items with empty list ID (e.g. user modified database);
                // also do not remove and re-insert to Cloud, would fail.
                val oldListId = oldItem.listId
                if (!oldListId.isNullOrEmpty()) {
                    val newListItem =
                        SgListItem(
                            tmdbIdOrZero,
                            ListItemTypes.TMDB_SHOW,
                            oldListId
                        )
                    toInsert.add(newListItem)

                    // Cloud changes
                    toRemoveCloud.add(SgCloudList().apply {
                        listId = oldListId
                        listItems = listOf(SgCloudListItem().apply {
                            listItemId = oldItem.listItemId
                        })
                    })
                    toInsertCloud.add(SgCloudList().apply {
                        listId = oldListId
                        listItems = listOf(SgCloudListItem().apply {
                            listItemId = newListItem.listItemId
                        })
                    })
                }
            }
        }

        Timber.d("Migrating %d list items to TMDB IDs", toRemove.size)

        // For cloud need list ID and item ID
        val requiresCloudUpdate = toRemoveCloud.isNotEmpty() || toInsertCloud.isNotEmpty()
        if (requiresCloudUpdate && HexagonSettings.isEnabled(context)) {
            val listsService = SgApp.getServicesComponent(context).hexagonTools().listsService
            if (listsService == null) {
                Timber.e("Cloud not signed in")
                return
            }
            try {
                if (toRemoveCloud.isNotEmpty()) {
                    listsService.removeItems(SgListList().apply {
                        lists = toRemoveCloud
                    }).execute()
                }
                if (toInsertCloud.isNotEmpty()) {
                    listsService.save(SgListList().apply {
                        lists = toInsertCloud
                    }).execute()
                }
            } catch (e: IOException) {
                Errors.logAndReportHexagon("migrate list items", e)
                if (e is HttpResponseException && e.statusCode == 400) {
                    // Bad Request, do not try again, but require a full lists sync.
                    // Note: had reports on Cloud where save failed due to not yet saved lists.
                    if (!HexagonSettings.setHasNotMergedLists(context)) {
                        // Failed to save, try again next sync
                        return
                    }
                } else {
                    // Abort and try again next sync
                    return
                }
            }
        }

        // Apply database updates
        if (toRemove.isNotEmpty()) database.sgListHelper().deleteListItems(toRemove)
        if (toInsert.isNotEmpty()) database.sgListHelper().insertListItems(toInsert)
    }
}
