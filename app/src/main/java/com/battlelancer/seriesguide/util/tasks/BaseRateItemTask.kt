// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.services.Sync

/**
 * Stores the [rating] in the database and sends it to Trakt.
 *
 * If it is `null`, removes the rating instead.
 */
abstract class BaseRateItemTask(
    context: Context,
    protected val rating: Rating?
) : BaseActionTask(context) {

    override val isSendingToHexagon: Boolean
        get() = false // Hexagon does not support ratings.

    override suspend fun doBackgroundAction(): Int {
        if (isSendingToTrakt) {
            if (!loadTraktIds()) return ERROR_DATABASE

            val trakt = SgApp.getServicesComponent(context).trakt()
            val traktSync = trakt.sync()

            val result = trakt.awaitAndHandleAuthErrorNonNull {
                sendToTrakt(traktSync)
            }.toActionResult {
                // If movie, show or episode was not found on Trakt
                if (TraktTools4.isNotFound(it)) ERROR_TRAKT_API_NOT_FOUND else SUCCESS
            }
            if (result != SUCCESS) {
                return result
            }
        }

        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE
        }

        return SUCCESS
    }

    override val successTextResId: Int
        get() = R.string.ack_rated

    /**
     * Loads the IDs required to identify the item on Trakt. Returns `false` on database error (if
     * required data is missing).
     */
    protected abstract fun loadTraktIds(): Boolean

    /**
     * Sends the [rating] to Trakt. Only called if [loadTraktIds] was successful.
     */
    protected abstract suspend fun sendToTrakt(traktSync: Sync): TraktNonNullResponse<SyncResponse>

    protected abstract fun doDatabaseUpdate(): Boolean
}
