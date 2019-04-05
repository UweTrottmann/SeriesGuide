package com.battlelancer.seriesguide.ui.overview

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.DBUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

/**
 * Updates episode counts for a specific season or all seasons of a show.
 */
class UnwatchedUpdateWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override val coroutineContext = Dispatchers.IO

    override suspend fun doWork(): Result = coroutineScope {
        val job = async {
            updateUnwatchedCount()
        }

        // if await throws an exception, CoroutineWorker will treat it as a failure
        job.await()
        Result.success()
    }

    @SuppressLint("Recycle")
    private fun updateUnwatchedCount() {
        val showTvdbId = inputData.getInt(EXTRA_SHOW_TVDB_ID, -1)
        if (showTvdbId < 0) {
            Timber.e("Not running: no showTvdbId.")
        }

        val seasonTvdbId = inputData.getInt(EXTRA_OPTIONAL_SEASON_TVDB_ID, -1)
        if (seasonTvdbId != -1) {
            // update one season
            DBUtils.updateUnwatchedCount(applicationContext, seasonTvdbId)
        } else {
            // update all seasons of this show, start with the most recent one
            val seasons = applicationContext.contentResolver.query(
                SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId),
                arrayOf(SeriesGuideContract.Seasons._ID),
                null,
                null,
                SeriesGuideContract.Seasons.COMBINED + " DESC"
            ) ?: return
            while (seasons.moveToNext()) {
                val seasonId = seasons.getInt(0)
                DBUtils.updateUnwatchedCount(applicationContext, seasonId)

                notifyContentProvider(showTvdbId)
            }
            seasons.close()
        }

        notifyContentProvider(showTvdbId)
        Timber.i("Updated watched count: show %d, season %d", showTvdbId, seasonTvdbId)
    }

    private fun notifyContentProvider(showTvdbId: Int) {
        applicationContext.contentResolver.notifyChange(
            SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId), null
        )
    }

    companion object {

        private const val TAG = "unwatched_updater"
        private const val WORK_NAME = "unwatched_updaters"
        private const val EXTRA_SHOW_TVDB_ID = "showTvdbId"
        private const val EXTRA_OPTIONAL_SEASON_TVDB_ID = "seasonTvdbId"

        fun enqueue(showTvdbId: Int, seasonTvdbId: Int? = null) {
            val data = workDataOf(
                EXTRA_SHOW_TVDB_ID to showTvdbId,
                EXTRA_OPTIONAL_SEASON_TVDB_ID to seasonTvdbId
            )

            val workRequest = OneTimeWorkRequestBuilder<UnwatchedUpdateWorker>()
                .addTag(TAG)
                .setInputData(data)
                .build()

            WorkManager.getInstance().enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.APPEND,
                workRequest
            )
        }

    }

}