// SPDX-License-Identifier: Apache-2.0
// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing.localdb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnlockStateHelper {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(unlockState: UnlockStateDb)

    fun insert(unlockState: UnlockState) {
        insert(unlockState.mapToDb())
    }

    @Query("SELECT * FROM unlock_state LIMIT 1")
    fun getUnlockStateDb(): UnlockStateDb?

    fun getUnlockState() = UnlockState.from(getUnlockStateDb())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(playUnlockState: PlayUnlockState)

    @Query("SELECT * FROM gold_status LIMIT 1")
    fun getPlayUnlockState(): PlayUnlockState?

    @Query("SELECT * FROM gold_status LIMIT 1")
    fun createPlayUnlockStateFlow(): Flow<PlayUnlockState?>

}