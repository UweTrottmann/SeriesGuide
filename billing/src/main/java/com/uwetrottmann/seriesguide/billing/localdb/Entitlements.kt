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

/**
 * Normally this would just be an interface. But since each of the entitlements only has
 * one item/row and so primary key is fixed, we can put the primary key here and so make
 * the class abstract.
 **/
abstract class Entitlement {
    @PrimaryKey
    var id: Int = 1

    /**
     * This method tells clients whether a user __should__ buy a particular item at the moment. For
     * example, if the gas tank is full the user should not be buying gas. This method is __not__
     * a reflection on whether Google Play Billing can make a purchase.
     */
    abstract fun mayPurchase(): Boolean
}

/**
 * Subscription is kept simple in this project. And so here the user either has a subscription
 * to gold status or s/he doesn't. For more on subscriptions, see the Classy Taxi sample app.
 */
@Entity(tableName = "gold_status")
data class GoldStatus(
    val entitled: Boolean,
    val isSub: Boolean,
    val sku: String?,
    val purchaseToken: String?
) : Entitlement() {
    override fun mayPurchase(): Boolean = !entitled
}

