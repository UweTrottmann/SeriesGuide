// SPDX-License-Identifier: Apache-2.0
// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing.localdb

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

@Entity(tableName = "unlock_state")
data class UnlockState(
    /**
     * Whether all features should be unlocked because there is an X pass install, active
     * subscription, one-time purchase from any billing method.
     */
    val isUnlockAll: Boolean = false,
    /**
     * The last time (in milliseconds) all features were unlocked. Use to give a grace period, for
     * ex. in case a billing provider is temporarily unavailable.
     */
    val lastUnlockedAllMs: Long = 0L,
    /**
     * If the user should be notified upon app launch that access to all features has expired, for
     * ex. when a subscription has expired or X Pass was uninstalled.
     */
    val notifyUnlockAllExpired: Boolean = false
) : Entitlement()

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

