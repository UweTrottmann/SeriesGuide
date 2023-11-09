// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0
package com.uwetrottmann.seriesguide.billing.localdb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * No update methods necessary since for each table there is ever expecting one row, hence why
 * the primary key is hardcoded.
 */
@Dao
interface EntitlementsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(goldStatus: GoldStatus)

    @Update
    fun update(goldStatus: GoldStatus)

    @Query("SELECT * FROM gold_status LIMIT 1")
    fun getGoldStatus(): GoldStatus?

    @Query("SELECT * FROM gold_status LIMIT 1")
    fun getGoldStatusLiveData(): LiveData<GoldStatus>

    @Delete
    fun delete(goldStatus: GoldStatus)

    /**
     * This is purely for convenience. The clients of this DAO
     * can simply send in a list of [entitlements][Entitlement].
     */
    @Transaction
    fun insert(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is GoldStatus -> insert(it)
            }
        }
    }

    @Transaction
    fun update(vararg entitlements: Entitlement) {
        entitlements.forEach {
            when (it) {
                is GoldStatus -> update(it)
            }
        }
    }
}