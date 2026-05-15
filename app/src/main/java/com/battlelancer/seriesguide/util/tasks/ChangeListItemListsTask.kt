// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.ContentProviderOperation
import android.content.Context
import android.content.OperationApplicationException
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.seriesguide.backend.lists.model.SgList
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList
import timber.log.Timber
import java.io.IOException

/**
 * Task to add or remove an item to and from lists.
 */
class ChangeListItemListsTask(
    context: Context,
    private val itemStableId: Int,
    private val itemType: Int,
    private val addToTheseLists: List<String>,
    private val removeFromTheseLists: List<String>
) : BaseActionTask(context) {

    override val isSendingToTrakt: Boolean = false

    override fun doBackgroundAction(vararg params: Void?): Int {
        if (isSendingToHexagon) {
            val hexagonTools = SgApp.getServicesComponent(context).hexagonTools()
            val listsService = hexagonTools.listsService
                ?: return ERROR_HEXAGON_API

            val wrapper = SgListList()
            if (addToTheseLists.isNotEmpty()) {
                val lists = buildListItemLists(addToTheseLists)
                wrapper.setLists(lists)

                try {
                    listsService.save(wrapper).execute()
                } catch (e: IOException) {
                    Errors.logAndReportHexagon("add list items", e)
                    return ERROR_HEXAGON_API
                }
            }

            if (removeFromTheseLists.isNotEmpty()) {
                val lists = buildListItemLists(removeFromTheseLists)
                wrapper.setLists(lists)

                try {
                    listsService.removeItems(wrapper).execute()
                } catch (e: IOException) {
                    Errors.logAndReportHexagon("remove list items", e)
                    return ERROR_HEXAGON_API
                }
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE
        }

        return SUCCESS
    }

    private fun buildListItemLists(listsToChange: List<String>): List<SgList> {
        return listsToChange.map { listId ->
            SgList().also { list ->
                list.setListId(listId)

                val item = SgListItem().also {
                    val listItemId = ListItems.generateListItemId(itemStableId, itemType, listId)
                    it.setListItemId(listItemId)
                }

                list.setListItems(listOf(item))
            }
        }
    }

    private fun doDatabaseUpdate(): Boolean {
        val batch = ArrayList<ContentProviderOperation>(
            addToTheseLists.size + removeFromTheseLists.size
        )
        for (listId in addToTheseLists) {
            val listItemId = ListItems.generateListItemId(itemStableId, itemType, listId)

            batch.add(
                ContentProviderOperation
                    .newInsert(ListItems.CONTENT_URI)
                    .withValue(ListItems.LIST_ITEM_ID, listItemId)
                    .withValue(ListItems.ITEM_REF_ID, itemStableId)
                    .withValue(ListItems.TYPE, itemType)
                    .withValue(SeriesGuideContract.Lists.LIST_ID, listId)
                    .build()
            )
        }
        for (listId in removeFromTheseLists) {
            val listItemId = ListItems.generateListItemId(itemStableId, itemType, listId)
            batch.add(
                ContentProviderOperation
                    .newDelete(ListItems.buildListItemUri(listItemId))
                    .build()
            )
        }

        // apply ops
        try {
            DBUtils.applyInSmallBatches(context, batch)
        } catch (e: OperationApplicationException) {
            Timber.e(e, "Applying list changes failed")
            return false
        }

        return true
    }

    override val successTextResId: Int
        get() = if (isSendingToHexagon) R.string.ack_list_item_manage else 0
}
