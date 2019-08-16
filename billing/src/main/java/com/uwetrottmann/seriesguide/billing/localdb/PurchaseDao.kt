/**
 * Copyright (C) 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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