// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.jobs

import android.app.PendingIntent
import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.util.Errors
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.uwetrottmann.trakt5.TraktV2
import retrofit2.Call
import retrofit2.Response

abstract class BaseNetworkJob(
    val action: JobAction,
    val jobInfo: SgJobInfo
) : NetworkJob {

    /**
     * @return JobResult.jobRemovable true if the job can be removed, false if it should be retried
     * later.
     */
    protected fun buildResult(context: Context, result: Int): NetworkJobResult {
        val error: String
        val removeJob: Boolean
        when (result) {
            SUCCESS -> {
                return NetworkJobResult(successful = true, jobRemovable = true)
            }
            ERROR_CONNECTION,
            ERROR_HEXAGON_SERVER,
            ERROR_TRAKT_SERVER -> {
                return NetworkJobResult(successful = false, jobRemovable = false)
            }
            ERROR_HEXAGON_AUTH -> {
                // TODO ut better error message if auth is missing, or drop?
                error = context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.hexagon)
                )
                removeJob = false
            }
            ERROR_TRAKT_AUTH -> {
                error = context.getString(R.string.trakt_error_credentials)
                removeJob = false
            }
            ERROR_HEXAGON_CLIENT -> {
                error = context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.hexagon)
                )
                removeJob = true
            }
            ERROR_TRAKT_CLIENT -> {
                error = context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.trakt)
                )
                removeJob = true
            }
            ERROR_TRAKT_NOT_FOUND -> {
                // show not on trakt: notify, but complete successfully
                error = context.getString(R.string.trakt_notice_not_exists)
                removeJob = true
            }
            ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED -> {
                error = context.getString(R.string.trakt_error_limit_exceeded)
                removeJob = true
            }
            ERROR_TRAKT_ACCOUNT_LOCKED -> {
                error = context.getString(R.string.trakt_error_account_locked)
                removeJob = true
            }
            else -> return NetworkJobResult(successful = true, jobRemovable = true)
        }
        return NetworkJobResult(
            successful = false,
            jobRemovable = removeJob,
            item = getItemTitle(context),
            action = getActionDescription(context),
            error = error,
            contentIntent = getErrorIntent(context)
        )
    }

    protected abstract fun getItemTitle(context: Context): String?
    protected abstract fun getActionDescription(context: Context): String?
    protected abstract fun getErrorIntent(context: Context): PendingIntent

    fun <T, R> executeTraktCall(
        context: Context,
        trakt: TraktV2,
        call: Call<T>,
        action: String,
        bodyAction: (Response<T>, T) -> Result<R, Int>
    ): Result<R, Int> {
        return runCatching {
            call.execute()
        }.mapError {
            Errors.logAndReport(action, it)
            ERROR_CONNECTION
        }.andThen { response ->
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    return@andThen bodyAction(response, body)
                } else {
                    Errors.logAndReport(action, response, "body is null")
                    return@andThen Err(ERROR_TRAKT_CLIENT)
                }
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return@andThen Err(ERROR_TRAKT_AUTH)
                }
                Errors.logAndReport(
                    action, response,
                    SgTrakt.checkForTraktError(trakt, response)
                )
                val code = response.code()
                val resultCode = when {
                    // 429 Rate Limit Exceeded or server error
                    code == 429 || code >= 500 -> ERROR_TRAKT_SERVER
                    code == 420 -> ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED
                    code == 423 -> ERROR_TRAKT_ACCOUNT_LOCKED
                    else -> ERROR_TRAKT_CLIENT
                }
                return Err(resultCode)
            }
        }
    }

    companion object {
        const val SUCCESS = 0

        /** Issue connecting or reading a response, should retry.  */
        const val ERROR_CONNECTION = -1
        const val ERROR_TRAKT_AUTH = -2

        /** Issue with request, do not retry.  */
        const val ERROR_TRAKT_CLIENT = -3

        /** Issue with connection or server, do retry.  */
        const val ERROR_TRAKT_SERVER = -4

        /** Show, season or episode not found, do not retry, but notify.  */
        const val ERROR_TRAKT_NOT_FOUND = -5

        /** Issue with the request, do not retry.  */
        const val ERROR_HEXAGON_CLIENT = -6

        /** Issue with connection or server, should retry.  */
        const val ERROR_HEXAGON_SERVER = -7
        const val ERROR_HEXAGON_AUTH = -8

        /** Account limit exceeded (list count, item count, ...), do not retry, but notify.  */
        private const val ERROR_TRAKT_ACCOUNT_LIMIT_EXCEEDED = -9

        /** Locked User Account, have the user contact Trakt support. */
        private const val ERROR_TRAKT_ACCOUNT_LOCKED = -10
    }
}