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

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.billingclient.api.SkuDetails


/**
 * The Play [BillingClient] provides a [SkuDetails] list that the [BillingRepository] could pass
 * along to clients to tell them what the app sells. With that approach, however, clients would have
 * to figure out all correlations between SkuDetails and [entitlements][Entitlement]. For example:
 * When the gas tankis full, the client would have to figure it out and disable the
 * [SkuDetails] button for buying more gas.
 *
 * Therefore, in the spirit of being client-friendly, whereas the [BillingRepository] is in a
 * better position to determine the correlations between a [SkuDetails] and its [Entitlement],
 * the API should provide an [AugmentedSkuDetails] object instead of the basic [SkuDetails].
 * This object not only passes to clients the actual [SkuDetails] object from Google, but also
 * tells clients whether a user is allowed to purchase that item at this particular moment.
 *
 * To be thorough, your implementation may be the following
 *
 * ```
 * @Entity
 * @TypeConverters(SkuDetailsTypeConverter::class)
 * class AugmentedSkuDetails(var skuDetails: SkuDetails, var canPurchase:Boolean,@PrimaryKey val sku:String)
 *
 * // and your Dao updates would look like:
 *
 * @Update
 *fun update(skuDetails: SkuDetails, sku:String)
 *
 *@Update
 *fun update(canPurchase:Boolean, sku:String)
 *
 * ```
 * But the actual implementation below shows an alternative where you only include the fields
 * you want your clients to care about. The choice is up to you.
 *
 */
@Entity
data class AugmentedSkuDetails(
        val canPurchase: Boolean, /* Not in SkuDetails; it's the augmentation */
        @PrimaryKey val sku: String,
        val type: String?,
        val price: String?,
        val priceMicros: Long?,
        val title: String?,
        val description: String?,
        val originalJson: String?
)