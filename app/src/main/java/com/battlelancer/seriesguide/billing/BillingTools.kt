// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.util.PackageTools
import com.uwetrottmann.seriesguide.billing.localdb.LocalBillingDb

object BillingTools {

    /**
     * Returns if the user should get access to paid features.
     */
    fun hasAccessToPaidFeatures(context: Context): Boolean {
        // Debug builds, installed X Pass key or subscription unlock all features
        if (PackageTools.isAmazonVersion()) {
            // Amazon version only supports all access as in-app purchase, so skip key check
            return AdvancedSettings.getLastSupporterState(context)
        } else {
            if (hasUnlockKey(context)) {
                return true
            } else {
                val goldStatus = LocalBillingDb.getInstance(context).entitlementsDao()
                    .getGoldStatus()
                return goldStatus != null && goldStatus.entitled
            }
        }
    }

    /**
     * Returns if X pass is installed and a purchase check with Google Play is not necessary to
     * determine access to paid features.
     */
    fun hasUnlockKey(context: Context): Boolean {
        return BuildConfig.DEBUG || PackageTools.hasUnlockKeyInstalled(context)
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