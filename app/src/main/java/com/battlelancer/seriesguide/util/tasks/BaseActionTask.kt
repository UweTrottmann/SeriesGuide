// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import androidx.annotation.CallSuper
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceActiveEvent
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.battlelancer.seriesguide.util.TaskManager
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

abstract class BaseActionTask(context: Context) {

    protected val context: Context = context.applicationContext

    private var _isSendingToHexagon: Boolean = false

    /**
     * Will be true if signed in with hexagon. Override and return `false` to not send to
     * hexagon.
     */
    protected open val isSendingToHexagon: Boolean
        get() = _isSendingToHexagon

    private var _isSendingToTrakt: Boolean = false

    /**
     * Will be true if signed in with trakt.
     */
    protected open val isSendingToTrakt: Boolean
        get() = _isSendingToTrakt

    /**
     * String resource for message to display to the user on success (recommended if a network
     * request is required), or 0 to display no message (if doing just a database update and there
     * is immediate UI feedback).
     */
    protected abstract val successTextResId: Int

    /**
     * Runs the task. Network and database work is done using [SgApp.coroutineScope], but only with
     * permit from [TaskManager.modifyOrExportShowsSemaphore].
     *
     * A [ServiceActiveEvent] sticky event is posted while running the task.
     * A [ServiceCompletedEvent] is posted once the task completes.
     */
    fun run() {
        SgApp.coroutineScope.launch {
            _isSendingToHexagon = HexagonSettings.isEnabled(context)
            _isSendingToTrakt = TraktCredentials.get(context).hasCredentials()

            // Show message to which service this sends
            EventBus.getDefault().postSticky(
                ServiceActiveEvent(isSendingToHexagon, isSendingToTrakt)
            )

            // Run this task only when other tasks are not modifying the database. Also don't use
            // SgApp.SINGLE, as if it suspends, other tasks might do breaking database changes.
            // The semaphore also guarantees tasks are executed in FIFO order, preventing issues
            // such as a rename getting scheduled before a deletion.
            TaskManager.modifyOrExportShowsSemaphore.withPermit {
                val result = withContext(Dispatchers.IO) {
                    // If sending to service, check for connection
                    if (isSendingToHexagon || isSendingToTrakt) {
                        if (!AndroidUtils.isNetworkConnected(context)) {
                            return@withContext ERROR_NETWORK
                        }
                    }

                    doBackgroundAction()
                }

                withContext(Dispatchers.Main) {
                    onPostExecute(result)
                }
            }
        }
    }

    protected abstract suspend fun doBackgroundAction(): Int

    /**
     * Maps a Trakt response to a result of [doBackgroundAction]. On success, returns the result of
     * [onSuccess].
     */
    protected fun <T> TraktNonNullResponse<T>.toActionResult(onSuccess: (T) -> Int): Int =
        when (this) {
            is TraktNonNullResponse.Success -> onSuccess(data)
            is TraktErrorResponse.IsUnauthorized -> ERROR_TRAKT_AUTH
            is TraktErrorResponse.IsAccountLimitExceeded -> ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED
            is TraktErrorResponse.IsAccountLocked -> ERROR_TRAKT_ACCOUNT_LOCKED
            is TraktErrorResponse.IsNotVip, is TraktErrorResponse.Other -> ERROR_TRAKT_API_CLIENT
        }

    @CallSuper
    protected open fun onPostExecute(result: Int) {
        EventBus.getDefault().removeStickyEvent(ServiceActiveEvent::class.java)

        val displaySuccess: Boolean
        val confirmationText: String?
        if (result == SUCCESS) {
            // success!
            displaySuccess = true
            confirmationText =
                if (successTextResId != 0) context.getString(successTextResId) else null
        } else {
            // handle errors
            displaySuccess = false
            confirmationText = when (result) {
                ERROR_NETWORK -> context.getString(R.string.offline)
                ERROR_DATABASE -> context.getString(R.string.database_error)
                ERROR_TRAKT_AUTH -> context.getString(R.string.trakt_error_credentials)
                ERROR_TRAKT_API_CLIENT -> context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.trakt)
                )

                ERROR_TRAKT_API_NOT_FOUND -> context.getString(R.string.trakt_error_not_exists)
                ERROR_HEXAGON_API -> context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.hexagon)
                )

                ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED -> context.getString(R.string.trakt_error_limit_exceeded_add)
                ERROR_TRAKT_ACCOUNT_LOCKED -> context.getString(R.string.trakt_error_account_locked)
                else -> null
            }
        }
        EventBus.getDefault().post(
            ServiceCompletedEvent(confirmationText, displaySuccess, null)
        )
    }

    companion object {
        const val SUCCESS: Int = 0
        private const val ERROR_NETWORK = -1
        const val ERROR_DATABASE: Int = -2
        const val ERROR_TRAKT_AUTH: Int = -3
        private const val ERROR_TRAKT_API_CLIENT = -4
        const val ERROR_TRAKT_API_NOT_FOUND: Int = -5
        const val ERROR_HEXAGON_API: Int = -6

        /**
         * Account limit exceeded (list count, item count, ...). Should currently only occur when
         * adding to watchlist.
         */
        private const val ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED = -8

        /**
         * Locked User Account, have the user contact Trakt support.
         */
        private const val ERROR_TRAKT_ACCOUNT_LOCKED = -9
    }
}
