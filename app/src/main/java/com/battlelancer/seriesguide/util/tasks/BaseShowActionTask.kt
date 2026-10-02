// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.services.Sync
import org.greenrobot.eventbus.EventBus

abstract class BaseShowActionTask(
    context: Context,
    protected val showTmdbId: Int
) : BaseActionTask(context) {

    class ShowChangedEvent

    override val isSendingToHexagon: Boolean
        get() = false

    override suspend fun doBackgroundAction(): Int {
        if (isSendingToTrakt) {
            val trakt = SgApp.getServicesComponent(context).trakt()
            val traktSync = trakt.sync()

            val result = trakt.awaitAndHandleAuthErrorNonNull {
                sendToTrakt(traktSync)
            }.toActionResult {
                // If show was not found on Trakt
                if (TraktTools4.isNotFound(it)) ERROR_TRAKT_API_NOT_FOUND else SUCCESS
            }
            if (result != SUCCESS) {
                return result
            }
        }

        return SUCCESS
    }

    override fun onPostExecute(result: Int) {
        super.onPostExecute(result)

        if (result == SUCCESS) {
            EventBus.getDefault().post(ShowChangedEvent())
        }
    }

    protected abstract suspend fun sendToTrakt(traktSync: Sync): TraktNonNullResponse<SyncResponse>

}
