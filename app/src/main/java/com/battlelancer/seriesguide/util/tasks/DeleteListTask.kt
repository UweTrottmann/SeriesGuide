// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.Errors
import java.io.IOException

/**
 * Task to delete a list and its items.
 */
class DeleteListTask(
    context: Context,
    private val listId: String
) : BaseActionTask(context) {

    override val isSendingToTrakt: Boolean = false

    override fun doBackgroundAction(vararg params: Void?): Int {
        if (isSendingToHexagon) {
            val hexagonTools = SgApp.getServicesComponent(context).hexagonTools()
            val listsService = hexagonTools.listsService
                    ?: return ERROR_HEXAGON_API // no longer signed in

            // send list to be removed from hexagon
            try {
                listsService.remove(listId).execute()
            } catch (e: IOException) {
                Errors.logAndReportHexagon("remove list", e)
                return ERROR_HEXAGON_API
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE
        }

        return SUCCESS
    }

    private fun doDatabaseUpdate(): Boolean {
        // delete all items of the list before list to avoid violating foreign key constraints
        context.contentResolver
            .delete(
                SeriesGuideContract.ListItems.CONTENT_URI,
                SeriesGuideContract.ListItems.SELECTION_LIST,
                arrayOf(listId)
            )
        // count of deleted items is not returned, so do not check

        // delete the list
        val deleted = context.contentResolver
            .delete(SeriesGuideContract.Lists.buildListUri(listId), null, null)
        return deleted != 0
    }

    override val successTextResId: Int
        get() = if (isSendingToHexagon) R.string.ack_list_removed else 0

}
