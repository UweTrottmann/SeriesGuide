package com.battlelancer.seriesguide.ui.overview

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.enums.SeasonTags
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgSeason2CountUpdate
import com.battlelancer.seriesguide.util.TimeTools
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
    fun updateUnwatchedCountFor(context: Context, showRowId: Long, seasonRowId: Long = -1) {
        SgApp.coroutineScope.launch {
            updateUnwatchedCount(context.applicationContext, showRowId, seasonRowId)
        }
    }

    private suspend fun updateUnwatchedCount(context: Context, showRowId: Long, seasonRowId: Long) =
        withContext(SgApp.SINGLE) {
            if (showRowId <= 0) {
                Timber.e("Not running: invalid show row ID.")
            }

            val helper = SgRoomDatabase.getInstance(context).sgSeason2Helper()
            if (seasonRowId != -1L) {
                // update one season
                updateUnwatchedCountForSeason(context, seasonRowId)
            } else {
                // update all seasons of this show, start with the most recent one
                val seasons = helper.getSeasonIdsOfShow(showRowId)
                for (seasonId in seasons) {
                    updateUnwatchedCountForSeason(context, seasonId)
                }
            }

            Timber.d("Updated watched count: show %d, season %d", showRowId, seasonRowId)
        }

    private fun updateUnwatchedCountForSeason(context: Context, seasonRowId: Long) {
        val database = SgRoomDatabase.getInstance(context)
        val helper = database.sgEpisode2Helper()
        val currentTime = TimeTools.getCurrentTime(context)

        val skippedCount = helper.countSkippedEpisodesOfSeason(seasonRowId)

        database.sgSeason2Helper().updateSeasonCounters(
            SgSeason2CountUpdate(
                seasonRowId,
                totalCount = helper.countEpisodesOfSeason(seasonRowId),
                notWatchedReleasedCount = helper.countNotWatchedReleasedEpisodesOfSeason(
                    seasonRowId,
                    currentTime
                ),
                notWatchedToBeReleasedCount = helper.countNotWatchedToBeReleasedEpisodesOfSeason(
                    seasonRowId,
                    currentTime
                ),
                notWatchedNoReleaseCount = helper.countNotWatchedNoReleaseEpisodesOfSeason(seasonRowId),
                tags = if (skippedCount > 0) SeasonTags.SKIPPED else SeasonTags.NONE
            )
        )
    }

}