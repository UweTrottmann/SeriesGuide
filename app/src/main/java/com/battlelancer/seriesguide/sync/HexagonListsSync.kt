// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2025 Uwe Trottmann

package com.battlelancer.seriesguide.sync

import android.content.ContentProviderOperation
import android.content.Context
import android.content.OperationApplicationException
import android.text.TextUtils
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.lists.ListsTools
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.google.api.client.util.DateTime
import com.uwetrottmann.seriesguide.backend.lists.model.SgList
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList
import timber.log.Timber
import java.io.IOException

class HexagonListsSync(
    private val context: Context,
    private val hexagonTools: HexagonTools
) {

    fun download(hasMergedLists: Boolean): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastSyncTime = DateTime(HexagonSettings.getLastListsSyncTime(context))

        if (hasMergedLists) {
            Timber.d("download: lists changed since %s.", lastSyncTime)
        } else {
            Timber.d("download: all lists.")
        }

        val localListIds = ListsTools.getListIds(context)
        var lists: List<SgList>?
        var cursor: String? = null
        do {
            try {
                // get service each time to check if auth was removed
                val listsService = hexagonTools.listsService
                    ?: return false // no longer signed in

                val request = listsService.get() // use default server limit
                if (hasMergedLists) {
                    request.updatedSince = lastSyncTime
                }
                if (!TextUtils.isEmpty(cursor)) {
                    request.cursor = cursor
                }

                val response = request.execute()
                if (response == null) {
                    Timber.d("download: failed, response is null.")
                    break
                }

                cursor = response.cursor
                lists = response.lists
            } catch (e: IOException) {
                Errors.logAndReportHexagon("get lists", e)
                return false
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get lists", e)
                return false
            }

            if (lists.isNullOrEmpty()) {
                break // empty response, assume we are done
            }

            if (!doListsDatabaseUpdate(lists, localListIds, hasMergedLists)) {
                return false // database update failed, abort
            }
        } while (!TextUtils.isEmpty(cursor)) // fetch next batch

        if (hasMergedLists) {
            HexagonSettings.setLastListsSyncTime(context, currentTime)
        }

        return true
    }

    private fun doListsDatabaseUpdate(
        lists: List<SgList>,
        localListIds: HashSet<String>,
        hasMergedLists: Boolean
    ): Boolean {
        val batch = ArrayList<ContentProviderOperation>()
        for (list in lists) {
            // add or update the list
            val listId = list.listId
            var builder: ContentProviderOperation.Builder? = null
            if (localListIds.contains(listId)) {
                // update
                if (hasMergedLists) {
                    // only overwrite name and order if data was already merged
                    // use case: user disconnected for a while, changed lists, then reconnects
                    builder = ContentProviderOperation
                        .newUpdate(SeriesGuideContract.Lists.buildListUri(listId))
                }
            } else {
                // insert
                builder = ContentProviderOperation
                    .newInsert(SeriesGuideContract.Lists.CONTENT_URI)
                    .withValue(SeriesGuideContract.Lists.LIST_ID, listId)
            }
            builder?.let { nonNullBuilder ->
                nonNullBuilder.withValue(SeriesGuideContract.Lists.NAME, list.name)
                list.order?.let { nonNullBuilder.withValue(SeriesGuideContract.Lists.ORDER, it) }
                batch.add(nonNullBuilder.build())
            }

            // keep track of items not in the list on hexagon
            var listItemsToRemove: HashSet<String>? = null
            if (hasMergedLists) {
                listItemsToRemove = ListsTools.getListItemIds(context, listId)
                if (listItemsToRemove == null) {
                    return false // list item query failed
                }
            }
            // add or update items of the list
            val listItems = list.listItems
            if (listItems != null) {
                for (listItem in listItems) {
                    val listItemId = listItem.listItemId
                    val brokenUpId = SeriesGuideContract.ListItems.splitListItemId(listItemId)
                        ?: continue // could not break up list item id
                    var itemReferenceId = -1
                    var itemType = -1
                    try {
                        itemReferenceId = brokenUpId[0].toInt()
                        itemType = brokenUpId[1].toInt()
                    } catch (ignored: NumberFormatException) {
                    }
                    if (itemReferenceId == -1
                        || !SeriesGuideContract.ListItems.isValidItemType(itemType)) {
                        continue // failed to extract item ref id or item type not known
                    }

                    // just insert the list item, if the id already exists it will be replaced
                    builder = ContentProviderOperation
                        .newInsert(SeriesGuideContract.ListItems.CONTENT_URI)
                        .withValue(SeriesGuideContract.ListItems.LIST_ITEM_ID, listItemId)
                        .withValue(SeriesGuideContract.ListItems.ITEM_REF_ID, itemReferenceId)
                        .withValue(SeriesGuideContract.ListItems.TYPE, itemType)
                        .withValue(SeriesGuideContract.Lists.LIST_ID, listId)
                    batch.add(builder.build())

                    if (hasMergedLists) {
                        // do not remove this list item
                        listItemsToRemove!!.remove(listItemId)
                    }
                }
            }
            if (hasMergedLists) {
                // remove items no longer in the list
                for (listItemId in listItemsToRemove!!) {
                    builder = ContentProviderOperation
                        .newDelete(SeriesGuideContract.ListItems.buildListItemUri(listItemId))
                    batch.add(builder.build())
                }
            }
        }

        return try {
            DBUtils.applyInSmallBatches(context, batch)
            true
        } catch (e: OperationApplicationException) {
            Timber.e(e, "doListsDatabaseUpdate: failed.")
            false
        }
    }

    fun pruneRemovedLists(): Boolean {
        Timber.d("pruneRemovedLists")
        val localListIds = ListsTools.getListIds(context)
            ?: return false // query failed
        if (localListIds.size <= 1) {
            return true // one or no list, can not remove any list
        }

        // get list of ids of lists on hexagon
        val hexagonListIds = ArrayList<String>(localListIds.size)
        var cursor: String? = null
        do {
            try {
                // get service each time to check if auth was removed
                val listsService = hexagonTools.listsService
                    ?: return false // no longer signed in

                val request = listsService.ids
                if (!TextUtils.isEmpty(cursor)) {
                    request.cursor = cursor
                }

                val response = request.execute()
                if (response == null) {
                    Timber.d("pruneRemovedLists: failed, response is null.")
                    return false
                }

                val listIds = response.listIds
                if (listIds == null || listIds.isEmpty()) {
                    break // empty response, assume we got all ids
                }
                hexagonListIds.addAll(listIds)

                cursor = response.cursor
            } catch (e: IOException) {
                Errors.logAndReportHexagon("get list ids", e)
                return false
            } catch (e: IllegalArgumentException) {
                // Note: JSON parser may throw IllegalArgumentException.
                Errors.logAndReportHexagon("get list ids", e)
                return false
            }
        } while (!TextUtils.isEmpty(cursor)) // fetch next batch

        if (hexagonListIds.size <= 1) {
            return true // one or no list on hexagon, can not remove any list
        }

        // exclude any lists that are on hexagon
        for (listId in hexagonListIds) {
            localListIds.remove(listId)
        }

        // remove any list not on hexagon
        if (localListIds.isNotEmpty()) {
            val batch = ArrayList<ContentProviderOperation>()
            for (listId in localListIds) {
                // note: this matches what RemoveListTask does
                // delete all list items before the list to avoid violating foreign key constraints
                batch.add(
                    ContentProviderOperation
                        .newDelete(SeriesGuideContract.ListItems.CONTENT_URI)
                        .withSelection(
                            SeriesGuideContract.ListItems.SELECTION_LIST,
                            arrayOf(listId)
                        )
                        .build()
                )
                // delete list
                batch.add(
                    ContentProviderOperation
                        .newDelete(SeriesGuideContract.Lists.buildListUri(listId))
                        .build()
                )
            }
            try {
                DBUtils.applyInSmallBatches(context, batch)
            } catch (e: OperationApplicationException) {
                Timber.e(e, "pruneRemovedLists: deleting lists failed.")
                return false
            }
        }

        return true
    }

    fun uploadAll(): Boolean {
        Timber.d("uploadAll")

        val listsWrapper = SgListList()
        val lists = ArrayList<SgList>(LISTS_MAX_BATCH_SIZE)
        listsWrapper.lists = lists

        val listsQuery = context.contentResolver.query(
            SeriesGuideContract.Lists.CONTENT_URI,
            ListsTools.Query.PROJECTION_LIST, null, null, null
        ) ?: return false // query failed

        while (listsQuery.moveToNext()) {
            val list = SgList()
            // add list properties
            val listId = listsQuery.getString(ListsTools.Query.LIST_ID)
            val listName = listsQuery.getString(ListsTools.Query.NAME)
            if (TextUtils.isEmpty(listId)) {
                continue // skip, no list id
            }
            list.listId = listId
            list.name = listName
            val order = listsQuery.getInt(ListsTools.Query.ORDER)
            if (order != 0) {
                list.order = order
            }
            // add list items
            val listItems = ListsTools.getListItems(context, listId)
            if (listItems != null) {
                list.listItems = listItems
            } else {
                Timber.d("uploadAll: no items to upload for list %s.", listId)
            }

            lists.add(list)

            if (lists.size == LISTS_MAX_BATCH_SIZE || listsQuery.isLast) {
                if (doUploadSomeLists(listsWrapper)) {
                    lists.clear()
                } else {
                    return false // part upload failed, next sync will try again
                }
            }
        }
        listsQuery.close()

        return true
    }

    private fun doUploadSomeLists(listsWrapper: SgListList): Boolean {
        val listsService = hexagonTools.listsService
            ?: return false // no longer signed in
        return try {
            listsService.save(listsWrapper).execute()
            true
        } catch (e: IOException) {
            Errors.logAndReportHexagon("save lists", e)
            false
        }
    }

    companion object {
        private const val LISTS_MAX_BATCH_SIZE = 10
    }
}
