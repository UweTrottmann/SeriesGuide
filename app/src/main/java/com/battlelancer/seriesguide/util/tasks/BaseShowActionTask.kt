// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.SyncShow
import com.uwetrottmann.trakt5.services.Sync
import org.greenrobot.eventbus.EventBus
import retrofit2.Call

abstract class BaseShowActionTask(
    context: Context,
    private val showTmdbId: Int
) : BaseActionTask(context) {

    class ShowChangedEvent

    override val isSendingToHexagon: Boolean
        get() = false

    override suspend fun doBackgroundAction(): Int {
        if (isSendingToTrakt) {
            if (!TraktCredentials.get(context).hasCredentials()) {
                return ERROR_TRAKT_AUTH
            }

            val items = SyncItems().shows(SyncShow().id(ShowIds.tmdb(showTmdbId)))
            val trakt = SgApp.getServicesComponent(context).trakt()
            val traktSync = trakt.sync()

            val result = executeTraktCall(
                buildTraktCall(traktSync, items),
                trakt,
                traktAction,
                object : ResponseCallback<SyncResponse> {
                    override fun handleSuccessfulResponse(body: SyncResponse): Int {
                        return if (isShowNotFound(body)) {
                            ERROR_TRAKT_API_NOT_FOUND
                        } else {
                            SUCCESS
                        }
                    }
                })
            if (result != SUCCESS) {
                return result
            }
        }

        return SUCCESS
    }

    private fun isShowNotFound(response: SyncResponse): Boolean {
        // if show was not found on trakt
        return response.not_found?.shows?.isNotEmpty()
                ?: false
    }

    override fun onPostExecute(result: Int) {
        super.onPostExecute(result)

        if (result == SUCCESS) {
            EventBus.getDefault().post(ShowChangedEvent())
        }
    }

    protected abstract val traktAction: String

    protected abstract fun buildTraktCall(traktSync: Sync, items: SyncItems): Call<SyncResponse>

}
