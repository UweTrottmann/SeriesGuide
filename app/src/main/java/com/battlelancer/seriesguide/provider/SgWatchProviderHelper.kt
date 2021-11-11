package com.battlelancer.seriesguide.provider

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import com.battlelancer.seriesguide.model.SgWatchProvider

@Dao
interface SgWatchProviderHelper {

    @Insert(onConflict = REPLACE)
    fun insertOrReplace(providers: List<SgWatchProvider>)

    @Query("DELETE FROM sg_watch_provider")
    fun deleteAll()

    @Transaction
    fun updateWatchProviders(newProviders: List<SgWatchProvider>) {
        deleteAll()
        insertOrReplace(newProviders)
    }

    @Query("SELECT * FROM sg_watch_provider ORDER BY display_priority ASC, provider_name ASC")
    fun getAllWatchProviders(): List<SgWatchProvider>

    @Query("SELECT provider_id FROM sg_watch_provider WHERE enabled=1")
    fun getEnabledWatchProviderIds(): List<Int>

}