package com.battlelancer.seriesguide.jobs

import android.app.PendingIntent
import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.JobAction

abstract class BaseNetworkJob(
    val action: JobAction,
    val jobInfo: SgJobInfo
) : NetworkJob {

    /**
     * @return JobResult.jobRemovable true if the job can be removed, false if it should be retried
     * later.
     */
    protected fun buildResult(context: Context, result: Int): JobResult {
        val error: String
        val removeJob: Boolean
        when (result) {
            SUCCESS -> {
                return JobResult(successful = true, jobRemovable = true)
            }
            ERROR_CONNECTION,
            ERROR_HEXAGON_SERVER,
            ERROR_TRAKT_SERVER -> {
                return JobResult(successful = false, jobRemovable = false)
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
            else -> return JobResult(successful = true, jobRemovable = true)
        }
        return JobResult(
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
    }
}