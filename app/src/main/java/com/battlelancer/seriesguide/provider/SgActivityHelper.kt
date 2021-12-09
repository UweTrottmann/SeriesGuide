package com.battlelancer.seriesguide.provider

import android.content.Context
import android.text.format.DateUtils
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.battlelancer.seriesguide.model.ActivityType
import com.battlelancer.seriesguide.model.SgActivity
import timber.log.Timber

/**
 * Helper methods for adding or removing local episode watch activity.
 */
@Dao
interface SgActivityHelper {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertActivity(activity: SgActivity)

    @Query("DELETE FROM activity WHERE activity_time < :deleteOlderThanMs")
    fun deleteOldActivity(deleteOlderThanMs: Long): Int

    @Query("DELETE FROM activity WHERE activity_episode = :episodeStableId AND activity_type = :type")
    fun deleteActivity(episodeStableId: String, type: Int): Int

    @Query("SELECT * FROM activity ORDER BY activity_time DESC")
    fun getActivityByLatest(): List<SgActivity>

    companion object {
        private const val HISTORY_THRESHOLD = 30 * DateUtils.DAY_IN_MILLIS

        /**
         * Adds an activity entry for the given episode with the current time as timestamp.
         * If an entry already exists it is replaced.
         *
         * Also cleans up old entries.
         */
        @JvmStatic
        fun addActivity(context: Context, episodeId: Long, showId: Long) {
            // Need to use global IDs (in case a show is removed and added again).
            val database = SgRoomDatabase.getInstance(context)

            // Try using TMDB ID
            var type = ActivityType.TMDB_ID
            var showStableIdOrZero = database.sgShow2Helper().getShowTmdbId(showId)
            var episodeStableIdOrZero = database.sgEpisode2Helper().getEpisodeTmdbId(episodeId)

            // Fall back to TVDB ID
            if (showStableIdOrZero == 0 || episodeStableIdOrZero == 0) {
                type = ActivityType.TVDB_ID
                showStableIdOrZero = database.sgShow2Helper().getShowTvdbId(showId)
                episodeStableIdOrZero = database.sgEpisode2Helper().getEpisodeTvdbId(episodeId)
                if (showStableIdOrZero == 0 || episodeStableIdOrZero == 0) {
                    // Should never happen: have neither TMDB or TVDB ID.
                    Timber.e(
                        "Failed to add activity, no TMDB or TVDB ID for show %d episode %d",
                        showId,
                        episodeId
                    )
                    return
                }
            }

            val timeMonthAgo = System.currentTimeMillis() - HISTORY_THRESHOLD
            val helper = database.sgActivityHelper()

            // delete all entries older than 30 days
            val deleted = helper.deleteOldActivity(timeMonthAgo)
            Timber.d("addActivity: removed %d outdated activities", deleted)

            // add new entry
            val currentTime = System.currentTimeMillis()
            val activity = SgActivity(
                null,
                episodeStableIdOrZero.toString(),
                showStableIdOrZero.toString(),
                currentTime,
                type
            )
            helper.insertActivity(activity)
            Timber.d("addActivity: episode: %d timestamp: %d", episodeId, currentTime)
        }

        /**
         * Tries to remove any activity with the given episode id.
         */
        @JvmStatic
        fun removeActivity(context: Context, episodeId: Long) {
            // Need to use global IDs (in case a show is removed and added again).
            val database = SgRoomDatabase.getInstance(context)

            // Try removal using TMDB ID.
            var deleted = 0
            val episodeTmdbIdOrZero = database.sgEpisode2Helper().getEpisodeTmdbId(episodeId)
            if (episodeTmdbIdOrZero != 0) {
                deleted += database.sgActivityHelper()
                    .deleteActivity(episodeTmdbIdOrZero.toString(), ActivityType.TMDB_ID)
            }
            // Try removal using TVDB ID.
            val episodeTvdbIdOrZero = database.sgEpisode2Helper().getEpisodeTvdbId(episodeId)
            if (episodeTvdbIdOrZero != 0) {
                deleted += database.sgActivityHelper()
                    .deleteActivity(episodeTvdbIdOrZero.toString(), ActivityType.TVDB_ID)
            }
            Timber.d("removeActivity: deleted %d activity entries", deleted)
        }
    }

}