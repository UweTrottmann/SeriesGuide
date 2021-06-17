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
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase

/**
 * This [Entity] is used partly for convenience and partly for security.
 *
 * - Convenience: when Play Billing calls onConsumeResponse, entitlements can be disbursed without
 *   first consulting the secure server, which might be temporarily down for, say, maintenance.
 *
 * - Security: Imagine a situation where four users of Trivial
 *   Drive decide to share one premium car purchase amongst themselves. So one user would buy the
 *   car and then log into the Play Store on each phone and as soon as they open the Trivial Drive
 *   app on a phone, they would see a Premium car. That would be fraudulent, but since this [Entity] is part
 *   of the local cache, the [BillingRepository] would notice there is a purchase in this local
 *   cache that the secure server does not recognize as belonging to this user. The secure server
 *   would then conduct further investigations and discover the fraud.
 *
 * This [Entity] will only be tracking active purchases. Hence, it's unlikely that very much data
 * will ever be saved here. It will keep subscriptions and non-consumables definitely, but
 * temporarily store consumables until Play confirms they have been consumed.
 *
 * While it would be more natural to simply call this class "Purchase," that might confuse new
 * developers to your team since [BillingClient] already calls its data [Purchase]. So it's better
 * to give it a different name. Also recall that [BillingRepository] must handle three different
 * data sources. So letting each source call its data by a slightly different name might make
 * reading the code easier.
 */

//TODO: This is the preferred implementation. Blocked on issue with ignoreColumns.
//@Entity(tableName = "purchase_table", ignoredColumns = arrayOf("mParsedJson"))
//class CachedPurchase(mOriginalJson: String, mSignature: String) : Purchase(mOriginalJson, mSignature) {
//    @PrimaryKey(autoGenerate = true)
//    var id: Int = 0
//}


@Entity(tableName = "purchase_table")
@TypeConverters(PurchaseTypeConverter::class)
class CachedPurchase(val data: Purchase) {

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    @Ignore
    val purchaseToken = data.purchaseToken
    @Ignore
    val sku: String = data.skus[0]

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is CachedPurchase -> data == other.data
            is Purchase -> data == other
            else -> false
        }
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

}

class PurchaseTypeConverter {
    @TypeConverter
    fun toString(purchase: Purchase): String = purchase.originalJson + '|' + purchase.signature

    @TypeConverter
    fun toPurchase(data: String): Purchase = data.split('|').let {
        Purchase(it[0], it[1])
    }
}