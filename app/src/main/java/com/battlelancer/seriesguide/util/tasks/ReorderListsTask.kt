// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.ContentProviderOperation
import android.content.Context
import android.content.OperationApplicationException
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.seriesguide.backend.lists.model.SgList
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList
import timber.log.Timber
import java.io.IOException

/**
 * Task to reorder all lists.
 *
 * The given lists order number will be changed to their position in the given list.
 */
class ReorderListsTask(
    context: Context,
    private val listIdsInOrder: List<String>
) : BaseActionTask(context) {

    override val isSendingToTrakt: Boolean
        get() = false

    override fun doBackgroundAction(vararg params: Void?): Int {
        if (isSendingToHexagon) {
            val hexagonTools = SgApp.getServicesComponent(context).hexagonTools()
            val listsService = hexagonTools.listsService
                ?: return ERROR_HEXAGON_API // no longer signed in

            // send lists with updated order to hexagon
            val wrapper = SgListList()
            val lists = buildListsList(listIdsInOrder)
            wrapper.lists = lists
            try {
                listsService.save(wrapper).execute()
            } catch (e: IOException) {
                Errors.logAndReportHexagon("reorder lists", e)
                return ERROR_HEXAGON_API
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE
        }

        return SUCCESS
    }

    private fun buildListsList(listsToChange: List<String>): List<SgList> {
        val lists: MutableList<SgList> = ArrayList(listsToChange.size)
        for (position in listsToChange.indices) {
            val listId = listsToChange[position]
            val list = SgList()
            list.listId = listId
            list.order = position
            lists.add(list)
        }
        return lists
    }

    private fun doDatabaseUpdate(): Boolean {
        val batch = ArrayList<ContentProviderOperation>()
        for (position in listIdsInOrder.indices) {
            val listId = listIdsInOrder[position]
            batch.add(
                ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Lists.buildListUri(listId)
                )
                    .withValue(SeriesGuideContract.Lists.ORDER, position)
                    .build()
            )
        }

        try {
            DBUtils.applyInSmallBatches(context, batch)
        } catch (e: OperationApplicationException) {
            Timber.e(e, "doDatabaseUpdate: failed to save reordered lists.")
            return false
        }
        return true
    }

    override val successTextResId: Int
        get() = if (isSendingToHexagon) R.string.ack_lists_reordered else 0
}
