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
import com.battlelancer.seriesguide.billing.localdb.LocalBillingDb
import com.battlelancer.seriesguide.billing.localdb.UnlockState
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.util.PackageTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.threeten.bp.Clock
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.util.concurrent.CountDownLatch

/**
 * Singleton to help manage unlock all features state and billing.
 */
object BillingTools {

    private val DEBUG = BuildConfig.DEBUG
//    private val DEBUG = false

    // At most 1 coroutine at a time should update the unlock state
    private val unlockStateUpdateDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val unlockStateInitialized = CountDownLatch(1)
    private val unlockState = MutableStateFlow(UnlockState())
    val unlockStateReadOnly = unlockState.asStateFlow()

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

        return unlockState.value.isUnlockAll
    }

    fun updateUnlockStateAsync(context: Context) {
        SgApp.coroutineScope.launch(unlockStateUpdateDispatcher) {
            updateUnlockState(context)
        }
    }

    private fun updateUnlockState(context: Context) {
        val amazonUnlockState = AdvancedSettings.getLastSupporterState(context)
        val passUnlockState = PackageTools.hasUnlockKeyInstalled(context)
        val playUnlockStateDb = LocalBillingDb.getInstance(context).unlockStateHelper()
            .getPlayUnlockState()
        val playUnlockState = playUnlockStateDb != null && playUnlockStateDb.entitled
        Timber.i(
            "updateUnlockState: pass=%s, amazon=%s, play=%s",
            passUnlockState,
            amazonUnlockState,
            playUnlockState
        )

        val isUnlockAll = if (PackageTools.isAmazonVersion()) {
            // Amazon version only supports unlocking with in-app purchase, so skip key check
            amazonUnlockState
        } else {
            passUnlockState || playUnlockState
        }

        updateUnlockState(context) { oldUnlockState ->
            getNewUnlockState(Clock.systemUTC(), oldUnlockState, isUnlockAll)
        }

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
                // Clamp to midnight UTC to avoid frequent updates
                Instant.now(clock).truncatedTo(ChronoUnit.DAYS).toEpochMilli()
            } else {
                oldUnlockState.lastUnlockedAllMs
            },
            notifyUnlockAllExpired = notifyUnlockAllExpired
        )
    }

    fun isNotifyUnlockAllExpired(): Boolean {
        return unlockState.value.notifyUnlockAllExpired
    }

    fun setNotifiedAboutExpiredUnlockState(context: Context) {
        SgApp.coroutineScope.launch(unlockStateUpdateDispatcher) {
            Timber.i("setNotifiedAboutExpiredUnlockState")
            updateUnlockState(context) { oldUnlockState ->
                oldUnlockState.copy(notifyUnlockAllExpired = false)
            }
        }
    }

    private fun updateUnlockState(context: Context, transform: (UnlockState) -> UnlockState) {
        val unlockStateHelper = LocalBillingDb.getInstance(context).unlockStateHelper()

        val oldUnlockState = unlockStateHelper.getUnlockState()
        val newUnlockState = transform(oldUnlockState)

        // Only insert if changed (to avoid log spam and unnecessary database writes)
        if (oldUnlockState != newUnlockState) {
            Timber.i(
                "updateUnlockState: %s -> %s%s",
                oldUnlockState,
                newUnlockState,
                if (DEBUG) " (debug mode: isUnlockAll overridden to true)" else ""
            )
            unlockStateHelper.insert(newUnlockState)
        }
        // Always update Flow to make sure it's initialized
        unlockState.value = if (DEBUG) newUnlockState.copy(isUnlockAll = true) else newUnlockState
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