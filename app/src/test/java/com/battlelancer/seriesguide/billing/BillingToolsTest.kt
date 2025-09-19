// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing

import com.battlelancer.seriesguide.billing.localdb.UnlockState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.temporal.ChronoUnit

class BillingToolsTest {

    @Test
    fun unlockStateChanges() {
        val testClock = Clock.fixed(Instant.now(), ZoneOffset.UTC)

        // default -> unlocked
        BillingTools.getNewUnlockState(testClock, UnlockState(), isUnlockAll = true)
            .also {
                assertThat(it.isUnlockAll).isTrue()
                assertThat(Instant.ofEpochMilli(it.lastUnlockedAllMs)).isEqualTo(
                    testClock.instant().truncatedTo(ChronoUnit.DAYS)
                )
                assertThat(it.notifyUnlockAllExpired).isFalse()
            }

        // default -> not unlocked
        BillingTools.getNewUnlockState(testClock, UnlockState(), isUnlockAll = false)
            .also {
                assertThat(it.isUnlockAll).isFalse()
                assertThat(it.notifyUnlockAllExpired).isFalse()
            }

        // unlocked -> not unlocked: notifies
        BillingTools.getNewUnlockState(
            testClock,
            UnlockState(isUnlockAll = true),
            isUnlockAll = false
        ).also {
            assertThat(it.isUnlockAll).isFalse()
            assertThat(it.notifyUnlockAllExpired).isTrue()
        }

        // not unlocked -> unlocked: clears notify flag
        BillingTools.getNewUnlockState(
            testClock,
            UnlockState(notifyUnlockAllExpired = true),
            isUnlockAll = true
        ).also {
            assertThat(it.isUnlockAll).isTrue()
            assertThat(it.notifyUnlockAllExpired).isFalse()
        }

        // notify flag not changed if unlock state remains
        // locked + false
        BillingTools.getNewUnlockState(
            testClock,
            UnlockState(),
            isUnlockAll = false
        ).also {
            assertThat(it.isUnlockAll).isFalse()
            assertThat(it.notifyUnlockAllExpired).isFalse()
        }
        // locked + true
        BillingTools.getNewUnlockState(
            testClock,
            UnlockState(notifyUnlockAllExpired = true),
            isUnlockAll = false
        ).also {
            assertThat(it.isUnlockAll).isFalse()
            assertThat(it.notifyUnlockAllExpired).isTrue()
        }
        // unlocked + true
        BillingTools.getNewUnlockState(
            testClock,
            UnlockState(isUnlockAll = true, notifyUnlockAllExpired = true),
            isUnlockAll = true
        ).also {
            assertThat(it.isUnlockAll).isTrue()
            assertThat(it.notifyUnlockAllExpired).isTrue()
        }
        // unlocked + false
        BillingTools.getNewUnlockState(
            testClock,
            UnlockState(isUnlockAll = true),
            isUnlockAll = true
        ).also {
            assertThat(it.isUnlockAll).isTrue()
            assertThat(it.notifyUnlockAllExpired).isFalse()
        }
    }

}