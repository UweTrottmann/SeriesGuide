package com.battlelancer.seriesguide.util

import android.content.Context
import android.text.format.DateUtils
import com.battlelancer.seriesguide.model.SgActivity
import com.battlelancer.seriesguide.provider.SgRoomDatabase.Companion.getInstance
import timber.log.Timber

/**
 * Helper methods for adding or removing local episode watch activity.
 */
object ActivityTools {

    private const val HISTORY_THRESHOLD = 30 * DateUtils.DAY_IN_MILLIS

    /**
     * Adds an activity entry for the given episode with the current time as timestamp. If an entry
     * already exists it is replaced.
     *
     * Also cleans up old entries.
     */
    fun addActivity(context: Context, episodeId: Long, showId: Long) {
        // Need to use global IDs (in case a show is removed and added again).
        val database = getInstance(context)
        val showTvdbIdOrZero = database.sgShow2Helper().getShowTvdbId(showId)
        if (showTvdbIdOrZero == 0) return
        val episodeTvdbIdOrZero = database.sgEpisode2Helper().getEpisodeTvdbId(episodeId)
        if (episodeTvdbIdOrZero == 0) return

        val timeMonthAgo = System.currentTimeMillis() - HISTORY_THRESHOLD
        val helper = database.sgActivityHelper()

        // delete all entries older than 30 days
        val deleted = helper.deleteOldActivity(timeMonthAgo)
        Timber.d("addActivity: removed %d outdated activities", deleted)

        // add new entry
        val currentTime = System.currentTimeMillis()
        val activity = SgActivity(
            null,
            episodeTvdbIdOrZero.toString(),
            showTvdbIdOrZero.toString(),
            currentTime
        )
        helper.insertActivity(activity)
        Timber.d("addActivity: episode: %d timestamp: %d", episodeId, currentTime)
    }

    /**
     * Tries to remove any activity with the given episode id.
     */
    fun removeActivity(context: Context, episodeId: Long) {
        // Need to use global IDs (in case a show is removed and added again).
        val database = getInstance(context)
        val episodeTvdbIdOrZero = database.sgEpisode2Helper().getEpisodeTvdbId(episodeId)
        if (episodeTvdbIdOrZero == 0) return
        val deleted = getInstance(context).sgActivityHelper()
            .deleteActivity(episodeTvdbIdOrZero.toString())
        Timber.d("removeActivity: deleted %d activity entries", deleted)
    }
}