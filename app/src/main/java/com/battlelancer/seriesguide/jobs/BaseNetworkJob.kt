package com.battlelancer.seriesguide.jobs

import android.app.PendingIntent
import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.sync.NetworkJobProcessor.JobResult

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
            NetworkJob.SUCCESS -> {
                return JobResult(true, true)
            }
            NetworkJob.ERROR_CONNECTION,
            NetworkJob.ERROR_HEXAGON_SERVER,
            NetworkJob.ERROR_TRAKT_SERVER -> {
                return JobResult(
                    false,
                    false
                )
            }
            NetworkJob.ERROR_HEXAGON_AUTH -> {
                // TODO ut better error message if auth is missing, or drop?
                error = context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.hexagon)
                )
                removeJob = false
            }
            NetworkJob.ERROR_TRAKT_AUTH -> {
                error = context.getString(R.string.trakt_error_credentials)
                removeJob = false
            }
            NetworkJob.ERROR_HEXAGON_CLIENT -> {
                error = context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.hexagon)
                )
                removeJob = true
            }
            NetworkJob.ERROR_TRAKT_CLIENT -> {
                error = context.getString(
                    R.string.api_error_generic,
                    context.getString(R.string.trakt)
                )
                removeJob = true
            }
            NetworkJob.ERROR_TRAKT_NOT_FOUND -> {
                // show not on trakt: notify, but complete successfully
                error = context.getString(R.string.trakt_notice_not_exists)
                removeJob = true
            }
            else -> return JobResult(true, true)
        }
        val jobResult = JobResult(false, removeJob)
        jobResult.item = getItemTitle(context)
        jobResult.action = getActionDescription(context)
        jobResult.error = error
        jobResult.contentIntent = getErrorIntent(context)
        return jobResult
    }

    protected abstract fun getItemTitle(context: Context): String?
    protected abstract fun getActionDescription(context: Context): String?
    protected abstract fun getErrorIntent(context: Context): PendingIntent
}