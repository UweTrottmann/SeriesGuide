// SPDX-License-Identifier: Apache-2.0
// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019-2025 Uwe Trottmann

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
}

/**
 * Stores unlock state obtained via a subscription or one-time purchase using Play Billing.
 */
@Entity(tableName = "gold_status")
data class PlayUnlockState(
    val entitled: Boolean,
    val isSub: Boolean,
    val sku: String?,
    val purchaseToken: String?
) : Entitlement()

