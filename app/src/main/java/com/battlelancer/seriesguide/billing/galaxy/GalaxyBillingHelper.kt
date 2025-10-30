// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing.galaxy

import android.content.Context
import com.battlelancer.seriesguide.BuildConfig
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.listener.OnAcknowledgePurchasesListener
import com.samsung.android.sdk.iap.lib.listener.OnChangeSubscriptionPlanListener
import com.samsung.android.sdk.iap.lib.listener.OnConsumePurchasedItemsListener
import com.samsung.android.sdk.iap.lib.listener.OnGetOwnedListListener
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class GalaxyBillingHelper(private val context: Context) {

    val iapHelper = IapHelper.getInstance(context)

    init {

        if (BuildConfig.DEBUG) {
            iapHelper.setOperationMode(HelperDefine.OperationMode.OPERATION_MODE_TEST)
        }
    }

    fun purchaseItem(itemId: String, callback: OnPaymentListener) {
        val accountId = "iaptest2@samsung.com"
        val profileId = "profile2"
        val obfuscatedAccountId: String = getObfuscatedString(accountId)
        val obfuscatedProfileId: String = getObfuscatedString(profileId)

        iapHelper.startPayment(itemId, obfuscatedAccountId, obfuscatedProfileId, callback)
    }

    fun changeSubscriptionPlan(
        oldItemId: String,
        newItemId: String,
        callback: OnChangeSubscriptionPlanListener
    ) {
        val accountId = "iaptest2@samsung.com"
        val profileId = "profile2"
        val obfuscatedAccountId: String = getObfuscatedString(accountId)
        val obfuscatedProfileId: String = getObfuscatedString(profileId)
        iapHelper.changeSubscriptionPlan(
            oldItemId,
            newItemId,
            HelperDefine.ProrationMode.INSTANT_PRORATED_DATE,
            obfuscatedAccountId,
            obfuscatedProfileId,
            callback
        )
    }

    fun consumeItem(purchaseId: String, callback: OnConsumePurchasedItemsListener) {
        iapHelper.consumePurchasedItems(purchaseId, callback)
    }

    fun acknowledgePurchases(purchaseId: String?, callback: OnAcknowledgePurchasesListener) {
        iapHelper.acknowledgePurchases(purchaseId!!, callback)
    }

    fun getOwnedList(itemType: String, callback: OnGetOwnedListListener) {
        iapHelper.getOwnedList(itemType, callback)
    }

    private fun getObfuscatedString(originId: String): String {
        try {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            messageDigest.update(originId.toByteArray(StandardCharsets.UTF_8))
            val bytes = messageDigest.digest()

            val stringBuilder = StringBuilder()
            for (b in bytes) {
                stringBuilder.append(String.format("%02x", b))
            }
            return stringBuilder.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

}