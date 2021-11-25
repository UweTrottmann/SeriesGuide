package com.battlelancer.seriesguide.provider

import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.battlelancer.seriesguide.model.SgWatchProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface SgWatchProviderHelper {

    @Insert(onConflict = REPLACE)
    fun insertOrReplace(providers: List<SgWatchProvider>)

    @Update
    fun update(providers: List<SgWatchProvider>)

    @Delete
    fun delete(providers: List<SgWatchProvider>)

    @Transaction
    fun updateWatchProviders(
        inserts: List<SgWatchProvider>,
        updates: List<SgWatchProvider>,
        deletes: List<SgWatchProvider>
    ) {
        delete(deletes)
        update(updates)
        insertOrReplace(inserts)
    }

    @Query("SELECT * FROM sg_watch_provider WHERE type=:type")
    fun getAllWatchProviders(type: Int): List<SgWatchProvider>

    @Query("SELECT * FROM sg_watch_provider WHERE type=:type ORDER BY display_priority ASC, provider_name ASC")
    fun allWatchProvidersPagingSource(type: Int): PagingSource<Int, SgWatchProvider>

    @Query("SELECT provider_id FROM sg_watch_provider WHERE type=:type AND enabled=1")
    fun getEnabledWatchProviderIds(type: Int): LiveData<List<Int>>

    @Query("SELECT provider_id FROM sg_watch_provider WHERE type=:type AND enabled=1")
    fun getEnabledWatchProviderIdsFlow(type: Int): Flow<List<Int>>

    @Query("UPDATE sg_watch_provider SET enabled=:enabled WHERE _id=:id")
    fun setEnabled(id: Int, enabled: Boolean)

    @Query("UPDATE sg_watch_provider SET enabled=0 WHERE type=:type")
    fun setAllDisabled(type: Int)

}