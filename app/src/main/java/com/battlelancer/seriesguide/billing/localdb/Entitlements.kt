// SPDX-License-Identifier: Apache-2.0
// Copyright (C) 2018 Google Inc. All Rights Reserved.
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing.localdb

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.Instant

/**
 * Each entitlement table only has one item/row, so use a fixed primary key.
 */
abstract class Entitlement {
    @PrimaryKey
    var id: Int = 1
}

/**
 * Null-safe variant of the database entity [UnlockStateDb].
 */
data class UnlockState(
    /**
     * Whether all features should be unlocked because there is an X pass install, active
     * subscription, one-time purchase from any billing method.
     */
    val isUnlockAll: Boolean = false,
    /**
     * The last time (in milliseconds) all features were unlocked.
     */
    val lastUnlockedAllMs: Long = 0L,
    /**
     * If the user should be notified upon app launch that access to all features has expired, for
     * ex. when a subscription has expired or X Pass was uninstalled.
     */
    val notifyUnlockAllExpired: Boolean = false
) {
    fun mapToDb(): UnlockStateDb {
        return UnlockStateDb(isUnlockAll, lastUnlockedAllMs, notifyUnlockAllExpired)
    }

    override fun toString(): String {
        // Make timestamp easier to read in log output
        val lastUnlockedAll = if (lastUnlockedAllMs != 0L) {
            Instant.ofEpochMilli(lastUnlockedAllMs).toString()
        } else "never"
        return "UnlockState(isUnlockAll=$isUnlockAll, lastUnlockedAll=$lastUnlockedAll, notifyUnlockAllExpired=$notifyUnlockAllExpired)"
    }


    companion object {
        fun from(unlockStateDb: UnlockStateDb?): UnlockState {
            val default = UnlockState()
            return if (unlockStateDb == null) {
                default
            } else {
                UnlockState(
                    unlockStateDb.isUnlockAll ?: default.isUnlockAll,
                    unlockStateDb.lastUnlockedAllMs ?: default.lastUnlockedAllMs,
                    unlockStateDb.notifyUnlockAllExpired ?: default.notifyUnlockAllExpired
                )
            }
        }
    }
}

@Entity(tableName = "unlock_state")
data class UnlockStateDb(
    @ColumnInfo(name = "is_unlock_all") val isUnlockAll: Boolean?,
    @ColumnInfo(name = "last_unlocked_all_ms") val lastUnlockedAllMs: Long?,
    @ColumnInfo(name = "notify_unlock_all_expired") val notifyUnlockAllExpired: Boolean?
) : Entitlement()

/**
 * Stores unlock state obtained via a subscription or one-time purchase using Play Billing.
 */
@Entity(tableName = "gold_status")
data class PlayUnlockState(
    val entitled: Boolean,
    val isSub: Boolean,
    val sku: String?,
    val purchaseToken: String?,
    @ColumnInfo(name = "last_updated_ms") val lastUpdatedMs: Long?
) : Entitlement() {

    companion object {
        fun withLastUpdatedNow(
            entitled: Boolean,
            isSub: Boolean,
            sku: String?,
            purchaseToken: String?
        ) = PlayUnlockState(entitled, isSub, sku, purchaseToken, Instant.now().toEpochMilli())

        fun revoked() = withLastUpdatedNow(
            entitled = false,
            isSub = true,
            sku = null,
            purchaseToken = null
        )
    }
}

