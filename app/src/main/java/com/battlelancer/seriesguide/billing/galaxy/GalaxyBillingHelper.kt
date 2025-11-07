// SPDX-License-Identifier: Apache-2.0
// Copyright 2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing.galaxy

import android.content.Context
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.util.PackageTools
import com.samsung.android.sdk.iap.lib.constants.HelperDefine
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.listener.OnAcknowledgePurchasesListener
import com.samsung.android.sdk.iap.lib.listener.OnChangeSubscriptionPlanListener
import com.samsung.android.sdk.iap.lib.listener.OnConsumePurchasedItemsListener
import com.samsung.android.sdk.iap.lib.listener.OnPaymentListener
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * https://developer.samsung.com/iap/programming-guide/iap-helper-programming.html
 */
class GalaxyBillingHelper(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {

    private val iapHelper by lazy {
        IapHelper.getInstance(context)
            .apply {
                if (BuildConfig.DEBUG) {
                    setOperationMode(HelperDefine.OperationMode.OPERATION_MODE_TEST)
                }
            }
            .also { Timber.i("IapHelper initialized") }
    }

    fun updateAvailableAndOwnedProducts() {
        coroutineScope.launch {
            // Don't call IapHelper methods if the store package is not installed, otherwise
            // HelperUtil.isGalaxyStoreValid() will show an error dialog.
            if (PackageTools.isGalaxyStoreInstalled(context)) {
                getProductDetails()
                getOwnedList()
            } else {
                Timber.i("Galaxy Store not installed, not checking for products")
            }
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

    private suspend fun getOwnedList() {
        val (error, ownedProducts) = suspendCoroutine { continuation ->
            // Returns true if the request was sent to server successfully and the result will be sent
            // to OnGetOwnedListListener interface listener.
            val sent = iapHelper.getOwnedList(IapHelper.PRODUCT_TYPE_ALL) { error, ownedProducts ->
                continuation.resume(OwnedListResult(error, ownedProducts))
            }
            Timber.i("getOwnedList sent=$sent")
        }
        if (error.errorCode == IapHelper.IAP_ERROR_NONE) {
            // TODO
            if (ownedProducts.isEmpty()) {
                Timber.i("No products owned")
            } else {
                ownedProducts.forEach { ownedProduct ->
                    Timber.i(ownedProduct.dump())
                }
            }
        } else {
            // TODO
            printErrorLog("getOwnedList", error)
        }
    }

    data class OwnedListResult(
        val error: ErrorVo,
        val ownedProducts: ArrayList<OwnedProductVo>
    )

    private suspend fun getProductDetails() {
        val (error, products) = suspendCoroutine { continuation ->
            // _productIds: Empty string ("") that designates all in-app products or
            // One or more unique in-app product ID values, comma delimited (for example, "coins,blocks,lives")
            iapHelper.getProductsDetails(SUB_ALL_ACCESS) { error: ErrorVo, products ->
                continuation.resume(ProductDetailsResult(error, products))
            }
        }
        if (error.errorCode == IapHelper.IAP_ERROR_NONE) {
            // TODO
            products.forEach { productVo ->
                Timber.i(productVo.dump())
            }
        } else {
            // TODO
            printErrorLog("getProductsDetails", error)
        }
    }

    data class ProductDetailsResult(
        val error: ErrorVo,
        val products: ArrayList<ProductVo>
    )

    fun printErrorLog(tag: String, errorVO: ErrorVo) {
        Timber.e("$tag failed (${errorVO.errorCode} ${errorVO.errorString} ${errorVO.errorDetailsString})")
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

    companion object {
        const val SUB_ALL_ACCESS = "galaxy_sub_all_access"
    }

}