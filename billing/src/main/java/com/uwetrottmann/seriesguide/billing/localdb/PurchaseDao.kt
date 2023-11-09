// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0
package com.uwetrottmann.seriesguide.billing.localdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.android.billingclient.api.Purchase

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchase_table")
    fun getPurchases(): List<CachedPurchase>

    @Insert
    fun insert(purchase: CachedPurchase)

    @Transaction
    fun insert(purchases: List<Purchase>) {
        purchases.forEach {
            insert(CachedPurchase(data = it))
        }
    }

    @Delete
    fun delete(vararg purchases: CachedPurchase)

    @Query("DELETE FROM purchase_table")
    fun deleteAll()

    @Query("DELETE FROM purchase_table WHERE data = :purchase")
    fun delete(purchase: Purchase)
}