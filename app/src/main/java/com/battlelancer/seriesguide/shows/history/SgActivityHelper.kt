// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.history

import android.content.Context
import android.text.format.DateUtils
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import timber.log.Timber

/**
 * Helper methods for adding or removing local episode watch activity.
 */
@Dao
interface SgActivityHelper {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertActivities(activities: List<SgActivity>)

    @Query("DELETE FROM activity WHERE activity_time < :deleteOlderThanMs")
    fun deleteOldActivity(deleteOlderThanMs: Long): Int

    @Query("DELETE FROM activity WHERE activity_episode = :episodeStableId AND activity_type = :type")
    fun deleteActivity(episodeStableId: String, type: Int): Int

    @Transaction
    fun deleteActivities(episodeStableId: List<String>, type: Int): Int {
        var deleted = 0
        episodeStableId.forEach {
            deleted += deleteActivity(it, type)
        }
        return deleted
    }

    @Query("SELECT * FROM activity ORDER BY activity_time DESC")
    fun getActivityByLatest(): List<SgActivity>

    companion object {
        private const val HISTORY_THRESHOLD = 90 * DateUtils.DAY_IN_MILLIS

        /**
         * Adds activity entries for the given episode TMDB IDs with the current time as timestamp.
         * If an entry already exists it is replaced.
         *
         * Also cleans up old entries.
         */
        fun addActivitiesForEpisodes(
            context: Context,
            showTmdbId: Int,
            episodeTmdbIds: List<Int>
        ) {
            val database = SgRoomDatabase.getInstance(context)
            val helper = database.sgActivityHelper()

            // delete all entries older than 30 days
            val timeMonthAgo = System.currentTimeMillis() - HISTORY_THRESHOLD
            val deleted = helper.deleteOldActivity(timeMonthAgo)
            Timber.d("addActivity: removed %d outdated activities", deleted)

            // add new entry
            val currentTime = System.currentTimeMillis()
            episodeTmdbIds.map {
                SgActivity(
                    null,
                    it.toString(),
                    showTmdbId.toString(),
                    currentTime,
                    ActivityType.TMDB_ID
                )
            }.also {
                helper.insertActivities(it)
                Timber.d("Added %d activities with time %d", it.size, currentTime)
            }
        }

        /**
         * Tries to remove any activity with the given episode TMDB IDs.
         */
        fun removeActivitiesForEpisodes(
            context: Context,
            episodeTmdbIds: List<Int>
        ) {
            SgRoomDatabase.getInstance(context).sgActivityHelper()
                .deleteActivities(episodeTmdbIds.map { it.toString() }, ActivityType.TMDB_ID)
                .also {
                    Timber.d("Deleted %d activity entries for %d episodes", it, episodeTmdbIds.size)
                }
        }
    }

}