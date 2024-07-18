// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util.tasks

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.annotation.CallSuper
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceActiveEvent
import com.battlelancer.seriesguide.ui.BaseMessageActivity.ServiceCompletedEvent
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.TraktV2
import org.greenrobot.eventbus.EventBus
import retrofit2.Call

@Suppress("DEPRECATION") // Just a warning that AsyncTask should not be used for new code
abstract class BaseActionTask(context: Context) : AsyncTask<Void?, Void?, Int?>() {

    @SuppressLint("StaticFieldLeak") // using application context
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

    @Deprecated("Deprecated in Java")
    override fun onPreExecute() {
        _isSendingToHexagon = HexagonSettings.isEnabled(context)
        _isSendingToTrakt = TraktCredentials.get(context).hasCredentials()

        // show message to which service we send
        EventBus.getDefault().postSticky(
            ServiceActiveEvent(isSendingToHexagon, isSendingToTrakt)
        )
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): Int? {
        if (isCancelled) {
            return null
        }

        // if sending to service, check for connection
        if (isSendingToHexagon || isSendingToTrakt) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return ERROR_NETWORK
            }
        }

        return doBackgroundAction(*params)
    }

    protected abstract fun doBackgroundAction(vararg params: Void?): Int?

    interface ResponseCallback<T> {
        fun handleSuccessfulResponse(body: T): Int
    }

    fun <T> executeTraktCall(
        call: Call<T>,
        trakt: TraktV2,
        action: String,
        callbackOnSuccess: ResponseCallback<T>
    ): Int {
        try {
            val response = call.execute()
            if (response.isSuccessful) {
                val body = response.body() ?: return ERROR_TRAKT_API_CLIENT
                return callbackOnSuccess.handleSuccessfulResponse(body)
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return ERROR_TRAKT_AUTH
                }
                Errors.logAndReport(
                    action, response,
                    SgTrakt.checkForTraktError(trakt, response)
                )
                val code = response.code()
                return if (code == 429 || code >= 500) {
                    ERROR_TRAKT_API_SERVER
                } else if (code == 420) {
                    ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED
                } else if (code == 423) {
                    ERROR_TRAKT_ACCOUNT_LOCKED
                } else {
                    ERROR_TRAKT_API_CLIENT
                }
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            return ERROR_NETWORK
        }
    }

    @Deprecated("Deprecated in Java")
    @CallSuper
    override fun onPostExecute(result: Int?) {
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
                ERROR_TRAKT_API_CLIENT, ERROR_TRAKT_API_SERVER -> context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.trakt)
                )

                ERROR_TRAKT_API_NOT_FOUND -> context.getString(R.string.trakt_error_not_exists)
                ERROR_HEXAGON_API -> context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.hexagon)
                )

                ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED -> context.getString(R.string.trakt_error_limit_exceeded)
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
        private const val ERROR_TRAKT_API_SERVER = -7

        /**
         * Account limit exceeded (list count, item count, ...).
         */
        private const val ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED = -8

        /**
         * Locked User Account, have the user contact Trakt support.
         */
        private const val ERROR_TRAKT_ACCOUNT_LOCKED = -9
    }
}
