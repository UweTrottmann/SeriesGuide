package com.battlelancer.seriesguide.provider

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.battlelancer.seriesguide.model.SgActivity

@Dao
interface SgActivityHelper {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertActivity(activity: SgActivity)

    @Query("DELETE FROM activity WHERE activity_time < :deleteOlderThanMs")
    fun deleteOldActivity(deleteOlderThanMs: Long): Int

    @Query("DELETE FROM activity WHERE activity_episode = :episodeTvdbOrTmdbId")
    fun deleteActivity(episodeTvdbOrTmdbId: String): Int

    @Query("SELECT * FROM activity ORDER BY activity_time DESC")
    fun getActivityByLatest(): List<SgActivity>

}