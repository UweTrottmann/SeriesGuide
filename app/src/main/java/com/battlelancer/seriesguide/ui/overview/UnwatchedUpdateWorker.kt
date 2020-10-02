package com.battlelancer.seriesguide.ui.overview

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.util.DBUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

object UnwatchedUpdateWorker {

    /**
     * Updates episode counts for a specific season or all seasons of a show.
     *
     * Runs all calls on a single thread to prevent parallel execution.
     *
     * May be cancelled when the app process dies.
     */
    fun updateUnwatchedCountFor(context: Context, showTvdbId: Int, seasonTvdbId: Int = -1) {
        SgApp.coroutineScope.launch {
            updateUnwatchedCount(context.applicationContext, showTvdbId, seasonTvdbId)
        }
    }

    private suspend fun updateUnwatchedCount(context: Context, showTvdbId: Int, seasonTvdbId: Int) =
        withContext(SgApp.SINGLE) {
            if (showTvdbId < 0) {
                Timber.e("Not running: invalid showTvdbId.")
            }

            if (seasonTvdbId != -1) {
                // update one season
                DBUtils.updateUnwatchedCount(context, seasonTvdbId)
            } else {
                // update all seasons of this show, start with the most recent one
                val seasons = context.contentResolver.query(
                    SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId),
                    arrayOf(SeriesGuideContract.Seasons._ID),
                    null,
                    null,
                    SeriesGuideContract.Seasons.COMBINED + " DESC"
                ) ?: return@withContext
                while (seasons.moveToNext()) {
                    val seasonId = seasons.getInt(0)
                    DBUtils.updateUnwatchedCount(context, seasonId)

                    notifyContentProvider(context, showTvdbId)
                }
                seasons.close()
            }

            notifyContentProvider(context, showTvdbId)
            Timber.i("Updated watched count: show %d, season %d", showTvdbId, seasonTvdbId)
        }

    private fun notifyContentProvider(context: Context, showTvdbId: Int) {
        context.contentResolver.notifyChange(
            SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId), null
        )
    }

}