// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.jobs

import android.app.NotificationManager
import android.content.ContentProviderOperation
import android.content.Context
import android.content.OperationApplicationException
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Jobs
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.DBUtils
import com.uwetrottmann.androidutils.AndroidUtils
import timber.log.Timber
import java.nio.ByteBuffer

/**
 * Gets jobs from the [Jobs] table, executes them one by one starting with the oldest.
 * Based on the job result shows an error notification and maybe removes the job.
 * If the job isn't removed, will stop processing further jobs. The job will be tried
 * again the next time jobs are processed.
 */
class NetworkJobProcessor(private val context: Context) {

    private val shouldSendToHexagon = HexagonSettings.isEnabled(context)
    private val shouldSendToTrakt = TraktCredentials.get(context).hasCredentials()

    fun process() {
        // query for jobs
        val query = context.contentResolver
            .query(Jobs.CONTENT_URI, Jobs.PROJECTION, null, null, Jobs.SORT_OLDEST)
            ?: return  // query failed

        // process jobs, starting with oldest
        val jobsToRemove: MutableList<Long> = ArrayList()
        while (query.moveToNext()) {
            val jobId = query.getLong(0)
            val typeId = query.getInt(1)
            val action = JobAction.fromId(typeId)

            if (action != JobAction.UNKNOWN) {
                Timber.d("Running job %d %s", jobId, action)

                val createdAt = query.getLong(2)
                val jobInfoArr = query.getBlob(3)
                val jobInfoBuffered = ByteBuffer.wrap(jobInfoArr)
                val jobInfo = SgJobInfo.getRootAsSgJobInfo(jobInfoBuffered)

                if (!doNetworkJob(jobId, action, createdAt, jobInfo)) {
                    Timber.e("Job %d failed, will retry.", jobId)
                    break // abort to avoid ordering issues
                }
                Timber.d("Job %d completed, will remove.", jobId)
            }
            jobsToRemove.add(jobId)
        }
        query.close()

        // remove completed jobs
        if (jobsToRemove.isNotEmpty()) {
            removeJobs(jobsToRemove)
        }
    }

    /**
     * Returns true if the job can be removed, false if it should be retried later.
     */
    private fun doNetworkJob(
        jobId: Long,
        action: JobAction,
        createdAt: Long,
        jobInfo: SgJobInfo
    ): Boolean {
        // upload to hexagon
        if (shouldSendToHexagon) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false
            }
            val hexagonTools = SgApp.getServicesComponent(context).hexagonTools()
            val hexagonJob = getHexagonJobForAction(hexagonTools, action, jobInfo)
            if (hexagonJob != null) {
                val result = hexagonJob.execute(context)
                if (!result.successful) {
                    showNotification(jobId, createdAt, result)
                    return result.jobRemovable
                }
            }
        }

        // upload to trakt
        if (shouldSendToTrakt) {
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false
            }
            val traktJob = getTraktJobForAction(action, jobInfo, createdAt)
            if (traktJob != null) {
                val result = traktJob.execute(context)
                // may need to show notification if successful (for not found error)
                showNotification(jobId, createdAt, result)
                if (!result.successful) {
                    return result.jobRemovable
                }
            }
        }
        return true
    }

    private fun getHexagonJobForAction(
        hexagonTools: HexagonTools, action: JobAction,
        jobInfo: SgJobInfo
    ): NetworkJob? {
        return when (action) {
            JobAction.EPISODE_COLLECTION,
            JobAction.EPISODE_WATCHED_FLAG -> {
                HexagonEpisodeJob(hexagonTools, action, jobInfo)
            }
            JobAction.MOVIE_COLLECTION_ADD,
            JobAction.MOVIE_COLLECTION_REMOVE,
            JobAction.MOVIE_WATCHLIST_ADD,
            JobAction.MOVIE_WATCHLIST_REMOVE,
            JobAction.MOVIE_WATCHED_SET,
            JobAction.MOVIE_WATCHED_REMOVE -> {
                HexagonMovieJob(hexagonTools, action, jobInfo)
            }
            else -> {
                null // Action not supported by hexagon.
            }
        }
    }

    private fun getTraktJobForAction(
        action: JobAction,
        jobInfo: SgJobInfo,
        createdAt: Long
    ): NetworkJob? {
        return when (action) {
            JobAction.EPISODE_COLLECTION,
            JobAction.EPISODE_WATCHED_FLAG -> {
                TraktEpisodeJob(action, jobInfo, createdAt)
            }
            JobAction.MOVIE_COLLECTION_ADD,
            JobAction.MOVIE_COLLECTION_REMOVE, JobAction.MOVIE_WATCHLIST_ADD, JobAction.MOVIE_WATCHLIST_REMOVE, JobAction.MOVIE_WATCHED_SET, JobAction.MOVIE_WATCHED_REMOVE -> // action not supported by trakt
            {
                TraktMovieJob(action, jobInfo, createdAt)
            }
            else -> {
                null // Action not supported by Trakt.
            }
        }
    }

    private fun showNotification(jobId: Long, jobCreatedAt: Long, result: NetworkJobResult) {
        if (result.action == null || result.error == null || result.item == null) {
            return // missing required values
        }
        val nb = NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_ERRORS)
        NotificationSettings.setDefaultsForChannelErrors(context, nb)

        nb.setSmallIcon(R.drawable.ic_notification)
        // like: 'Failed: Remove from collection · BoJack Horseman'
        nb.setContentTitle(
            context.getString(R.string.api_failed, "${result.action} · ${result.item}")
        )
        nb.setContentText(result.error)
        nb.setStyle(
            NotificationCompat.BigTextStyle().bigText(
                getErrorDetails(result.item, result.error, result.action, jobCreatedAt)
            )
        )
        nb.setContentIntent(result.contentIntent)
        nb.setAutoCancel(true)

        // notification for each failed job
        val nm = context.getSystemService<NotificationManager>()
        nm?.notify(jobId.toString(), SgApp.NOTIFICATION_JOB_ID, nb.build())
    }

    private fun getErrorDetails(
        item: String, error: String,
        action: String, jobCreatedAt: Long
    ): String {
        val builder = StringBuilder()
        // build message like:
        // 'Could not talk to server.
        // BoJack Horseman · Set watched · 5 sec ago'
        builder.append(error).append("\n").append(item).append(" · ").append(action)

        // append time if job is executed a while after it was created
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - jobCreatedAt > 3 * DateUtils.SECOND_IN_MILLIS) {
            builder.append(" · ")
            builder.append(
                DateUtils.getRelativeTimeSpanString(
                    jobCreatedAt,
                    currentTimeMillis, DateUtils.SECOND_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_ALL
                )
            )
        }
        return builder.toString()
    }

    private fun removeJobs(jobsToRemove: List<Long>) {
        val batch = ArrayList<ContentProviderOperation>()
        for (jobId in jobsToRemove) {
            batch.add(
                ContentProviderOperation.newDelete(Jobs.buildJobUri(jobId))
                    .build()
            )
        }
        try {
            DBUtils.applyInSmallBatches(context, batch)
        } catch (e: OperationApplicationException) {
            Timber.e(e, "process: failed to delete completed jobs")
        }
    }

    /**
     * If neither Trakt or Cloud are connected, clears all remaining jobs.
     */
    fun removeObsoleteJobs(ignoreHexagonState: Boolean) {
        if (!ignoreHexagonState && shouldSendToHexagon || shouldSendToTrakt) {
            return // Still signed in to either service, do not clear jobs.
        }
        context.contentResolver.delete(Jobs.CONTENT_URI, null, null)
    }
}