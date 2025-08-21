// SPDX-License-Identifier: Apache-2.0
// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019-2025 Uwe Trottmann

package com.uwetrottmann.seriesguide.billing.localdb

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UnlockStateHelper {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(playUnlockState: PlayUnlockState)

    @Query("SELECT * FROM gold_status LIMIT 1")
    fun getPlayUnlockState(): PlayUnlockState?

    @Query("SELECT * FROM gold_status LIMIT 1")
    fun getPlayUnlockStateLiveData(): LiveData<PlayUnlockState>

}