// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.util.PackageTools
import com.uwetrottmann.seriesguide.billing.localdb.LocalBillingDb
import com.uwetrottmann.seriesguide.billing.localdb.UnlockState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import timber.log.Timber
import java.util.concurrent.CountDownLatch

/**
 * Singleton to help manage unlock all features state and billing.
 */
object BillingTools {

    // At most 1 coroutine at a time should update the unlock state
    private val unlockStateUpdateDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val unlockStateInitialized = CountDownLatch(1)
    private val unlockState = MutableStateFlow(false)

    /**
     * Returns if the user should get access to paid features.
     *
     * Note: this method will block until the unlock state is initialized.
     */
    fun hasAccessToPaidFeatures(): Boolean {
        try {
            unlockStateInitialized.await()
        } catch (_: InterruptedException) {
            // Caller was interrupted, so should not care about the result
            Timber.d("hasAccessToPaidFeatures: interrupted while waiting")
            return false
        }

        return unlockState.value
    }

    fun updateUnlockStateAsync(context: Context) {
        SgApp.coroutineScope.launch(unlockStateUpdateDispatcher) {
            updateUnlockState(context)
        }
    }

    private fun updateUnlockState(context: Context) {
        val unlockStateHelper = LocalBillingDb.getInstance(context).unlockStateHelper()

        // Debug builds, installed X Pass key or subscription unlock all features
        val isUnlockAll = if (PackageTools.isAmazonVersion()) {
            // Amazon version only supports all access as in-app purchase, so skip key check
            AdvancedSettings.getLastSupporterState(context)
        } else {
            if (PackageTools.hasUnlockKeyInstalled(context)) {
                true
            } else {
                // TODO Auto-expire after 1 year if not updated by Play Billing (for ex. when user
                //  plans to switch billing provider after changing installer source)
                val playUnlockState = unlockStateHelper.getPlayUnlockState()
                playUnlockState != null && playUnlockState.entitled
            }
        }

        val oldUnlockState = unlockStateHelper.getUnlockState() ?: UnlockState()

        unlockStateHelper.insert(getNewUnlockState(Clock.systemUTC(), oldUnlockState, isUnlockAll))

        Timber.i(
            "updateUnlockState: unlockState=%s, newUnlockState=%s%s",
            unlockState.value,
            isUnlockAll,
            if (BuildConfig.DEBUG) " (debug mode: overridden to true)" else ""
        )
        unlockState.value = if (BuildConfig.DEBUG) true else isUnlockAll
        unlockStateInitialized.countDown()
    }

    fun getNewUnlockState(
        clock: Clock,
        oldUnlockState: UnlockState,
        isUnlockAll: Boolean
    ): UnlockState {
        // TODO Grace period? But likely notify already and support purchasing?
//        val lastUnlockedInstant = Instant.ofEpochMilli(oldUnlockState.lastUnlockedAllMs)
//        val aDayAgo = now.minus(24, ChronoUnit.HOURS)
//        if (lastUnlockedInstant.isBefore(aDayAgo)) {
//            true
//        } else {
//            false
//        }

        // Only change if unlock state changes
        val notifyUnlockAllExpired = if (isUnlockAll != oldUnlockState.isUnlockAll) {
            !isUnlockAll
        } else oldUnlockState.notifyUnlockAllExpired

        return UnlockState(
            isUnlockAll = isUnlockAll,
            lastUnlockedAllMs = if (isUnlockAll) {
                Instant.now(clock).toEpochMilli()
            } else {
                oldUnlockState.lastUnlockedAllMs
            },
            notifyUnlockAllExpired = notifyUnlockAllExpired
        )
    }

    /**
     * Notifies that something is only available with a subscription and launches
     * [AmazonBillingActivity] or [BillingActivity].
     */
    fun advertiseSubscription(context: Context) {
        Toast.makeText(context, R.string.onlyx, Toast.LENGTH_SHORT).show()
        context.startActivity(getBillingActivityIntent(context))
    }

    fun getBillingActivityIntent(context: Context): Intent {
        return if (PackageTools.isAmazonVersion()) {
            Intent(context, AmazonBillingActivity::class.java)
        } else {
            BillingActivity.intent(context)
        }
    }

}