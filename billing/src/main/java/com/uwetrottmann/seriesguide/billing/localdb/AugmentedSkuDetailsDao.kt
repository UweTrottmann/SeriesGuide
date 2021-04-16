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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.SkuDetails

/**
 * The rest of the app needs a list of the [SkuDetails] so to show users what to buy
 * and for how much. [LiveData] should be used so the appropriate UIs get the most up-to-date
 * data. Notice that two sets is being created: one for subscriptions and one for managed products.
 * That's because in this sample subscriptions and in-app products are listed separately. However,
 * some use cases may have more than two sets; for instance, if each Fragment/Activity must list
 * different set of SKUs.
 */
@Dao
interface AugmentedSkuDetailsDao {

    @Query("SELECT * FROM AugmentedSkuDetails WHERE type = '${BillingClient.SkuType.SUBS}' ORDER BY priceMicros ASC")
    fun getSubscriptionSkuDetails(): LiveData<List<AugmentedSkuDetails>>

    @Transaction
    fun insertOrUpdate(skuDetails: SkuDetails) = skuDetails.apply {
        val result = getById(sku)
        val canPurchase = if (result == null) true else result.canPurchase
        val originalJson = toString().substring("SkuDetails: ".length)
        val detail = AugmentedSkuDetails(
            canPurchase,
            sku,
            type,
            price,
            priceAmountMicros,
            title,
            description,
            originalJson
        )
        insert(detail)
    }

    @Transaction
    fun insertOrUpdate(sku: String, canPurchase: Boolean) {
        val result = getById(sku)
        if (result != null) {
            update(sku, canPurchase)
        } else {
            insert(AugmentedSkuDetails(canPurchase, sku, null, null, null, null, null, null))
        }
    }

    @Query("SELECT * FROM AugmentedSkuDetails WHERE sku = :sku")
    fun getById(sku: String): AugmentedSkuDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(augmentedSkuDetails: AugmentedSkuDetails)

    @Query("UPDATE AugmentedSkuDetails SET canPurchase = :canPurchase WHERE sku = :sku")
    fun update(sku: String, canPurchase: Boolean)
}