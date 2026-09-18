// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.lists.ListsTools
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.seriesguide.backend.lists.model.SgList
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList
import java.io.IOException

/**
 * Task to add a new list.
 */
open class AddListTask(
    context: Context,
    protected val listName: String
) : BaseActionTask(context) {

    override fun doBackgroundAction(vararg params: Void?): Int {
        // The user interface should protect against passing an empty name, but check regardless
        val listId = listId
            ?: return ERROR_DATABASE

        if (isSendingToTrakt) {
            val traktListId = TraktTools4.createList(traktSync.users, listName)
                ?: return ERROR_TRAKT_API
        }

        if (isSendingToHexagon) {
            val hexagonTools = SgApp.getServicesComponent(context).hexagonTools()
            val listsService = hexagonTools.listsService
                ?: return ERROR_HEXAGON_API // no longer signed in

            // send list to be added to hexagon
            val wrapper = SgListList()
            val lists = buildList(listId, listName)
            wrapper.setLists(lists)
            try {
                listsService.save(wrapper).execute()
            } catch (e: IOException) {
                Errors.logAndReportHexagon("add list", e)
                return ERROR_HEXAGON_API
            }
        }

        // update local state
        if (!doDatabaseUpdate(context.contentResolver, listId)) {
            return ERROR_DATABASE
        }

        return SUCCESS
    }

    @get:VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open val listId: String?
        get() = ListsTools.generateListId(listName)

    private fun buildList(listId: String, listName: String): List<SgList> {
        val lists = ArrayList<SgList>(1)
        val list = SgList()
        list.setListId(listId)
        list.setName(listName)
        lists.add(list)
        return lists
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    open fun doDatabaseUpdate(contentResolver: ContentResolver, listId: String): Boolean {
        val values = ContentValues()
        values.put(SeriesGuideContract.Lists.LIST_ID, listId)
        values.put(SeriesGuideContract.Lists.NAME, listName)
        // default value
        values.put(SeriesGuideContract.Lists.ORDER, 0)
        contentResolver.insert(SeriesGuideContract.Lists.CONTENT_URI, values)
        return true
    }

    override val successTextResId: Int
        get() = if (isSendingToHexagon) R.string.ack_list_added else 0

}
