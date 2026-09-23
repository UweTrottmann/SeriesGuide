// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract

/**
 * Task to rename a list.
 */
class RenameListTask(
    context: Context,
    override val listId: String,
    listName: String
) : AddListTask(context, listName) {

    override val isSendingToTrakt: Boolean = false

    override fun doDatabaseUpdate(contentResolver: ContentResolver, listId: String): Boolean {
        val values = ContentValues()
        values.put(SeriesGuideContract.Lists.NAME, listName)
        val updated = contentResolver
            .update(SeriesGuideContract.Lists.buildListUri(listId), values, null, null)
        return updated != 0
    }

    override val successTextResId: Int
        get() = if (isSendingToHexagon) R.string.ack_list_renamed else 0
}
