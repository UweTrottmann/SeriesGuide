// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.streaming

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SgWatchProviderHelper {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(providers: List<SgWatchProvider>)

    @Update
    suspend fun update(providers: List<SgWatchProvider>)

    @Delete
    suspend fun delete(providers: List<SgWatchProvider>)

    @Transaction
    suspend fun updateWatchProviders(
        inserts: List<SgWatchProvider>,
        updates: List<SgWatchProvider>,
        deletes: List<SgWatchProvider>
    ) {
        delete(deletes)
        update(updates)
        insertOrReplace(inserts)
    }

    @Query("SELECT * FROM sg_watch_provider WHERE type=:type")
    suspend fun getAllWatchProviders(type: Int): List<SgWatchProvider>

    // Android provides the UNICODE collator,
    // use to correctly order characters with e.g. accents.
    // https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase
    @Query("SELECT * FROM sg_watch_provider WHERE type=:type ORDER BY provider_name COLLATE UNICODE ASC")
    fun allWatchProvidersPagingSource(type: Int): PagingSource<Int, SgWatchProvider>

    /**
     * Watch providers of all shows sorted by name.
     *
     * Android provides the UNICODE collator,
     * use to correctly order characters with e.g. accents and ignore case.
     * https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase
     */
    @Query("SELECT sg_watch_provider.* FROM sg_watch_provider JOIN sg_watch_provider_show_mappings ON sg_watch_provider.provider_id=sg_watch_provider_show_mappings.provider_id WHERE type=:type GROUP BY _id ORDER BY provider_name COLLATE UNICODE ASC")
    fun usedWatchProvidersFlow(type: Int): Flow<List<SgWatchProvider>>

    /* Note: never just get those with filter_local=1 as once a show does not longer use a provider
     it is not longer shown in the filter UI, so it can not be disabled. */
    @Query("SELECT sg_watch_provider.* FROM sg_watch_provider JOIN sg_watch_provider_show_mappings ON sg_watch_provider.provider_id=sg_watch_provider_show_mappings.provider_id WHERE type=:type AND filter_local=1 GROUP BY _id")
    fun filterLocalWatchProviders(type: Int): Flow<List<SgWatchProvider>>

    @Query("SELECT provider_id FROM sg_watch_provider WHERE type=:type AND enabled=1")
    fun getEnabledWatchProviderIdsFlow(type: Int): Flow<List<Int>>

    @Query("UPDATE sg_watch_provider SET enabled=:enabled WHERE _id=:id")
    fun setEnabled(id: Int, enabled: Boolean)

    @Query("UPDATE sg_watch_provider SET filter_local=:enabled WHERE _id=:id")
    suspend fun setFilterLocal(id: Int, enabled: Boolean)

    @Query("UPDATE sg_watch_provider SET filter_local=0 WHERE type=:type")
    suspend fun setFilterLocalFalseAll(type: Int)

    @Query("UPDATE sg_watch_provider SET enabled=0 WHERE type=:type")
    fun setAllDisabled(type: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addShowMappings(mappings: List<SgWatchProviderShowMapping>)

    @Query("DELETE FROM sg_watch_provider_show_mappings WHERE show_id=:showId")
    suspend fun deleteShowMappings(showId: Long)
}