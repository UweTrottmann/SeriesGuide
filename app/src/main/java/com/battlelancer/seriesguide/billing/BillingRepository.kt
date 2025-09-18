// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.billing.localdb.Entitlement
import com.battlelancer.seriesguide.billing.localdb.LocalBillingDb
import com.battlelancer.seriesguide.billing.localdb.PlayUnlockState
import com.battlelancer.seriesguide.common.SingleLiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import timber.log.Timber
import kotlin.math.max

/**
 * ## Play Billing
 *
 * * https://developer.android.com/google/play/billing/integrate
 * * https://developer.android.com/google/play/billing/errors
 * * https://developer.android.com/reference/com/android/billingclient/classes
 * * https://github.com/googlesamples/android-play-billing
 *
 * To test the legacy one-time purchase use a gift code on the test account, refund and revoke it
 * using the Play Console web interface.
 *
 * To test trial available state in the UI, there is a test subscription.
 *
 * To test response codes (and also trial state), can use Play Billing Lab app. Make sure to
 * uncomment relevant tags in AndroidManifest.xml.
 * https://developer.android.com/google/play/billing/test-response-codes
 */
class BillingRepository private constructor(
    private val applicationContext: Context,
    private val coroutineScope: CoroutineScope
) {

    /**
     * The [BillingClient] is the most reliable and primary source of truth for all purchases
     * made through the Google Play Store. The Play Store takes security precautions in guarding
     * the data. Also, the data is available offline in most cases, which means the app incurs no
     * network charges for checking for purchases using the [BillingClient]. The offline bit is
     * because the Play Store caches every purchase the user owns, in an
     * [eventually consistent manner](https://developer.android.com/google/play/billing/billing_library_overview#Keep-up-to-date).
     * This is the only billing client an app is actually required to have on Android.
     * localCacheBillingClient is optional.
     *
     * ASIDE. Notice that the connection to [playStoreBillingClient] is created using the
     * applicationContext. This means the instance is not [Activity]-specific. And since it's also
     * not expensive, it can remain open for the life of the entire [Application]. So whether it is
     * (re)created for each Activity or Fragment or is kept open for the life of the application
     * is a matter of choice.
     */
    private val playStoreBillingClient: BillingClient by lazy {
        BillingClient.newBuilder(applicationContext)
            .enablePendingPurchases(
                // For now not supporting it (prepaid plans) for subscriptions
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .setListener(purchasesUpdatedListener)
            .build()
    }

    /**
     * A local cache billing client is important in that the Play Store may be temporarily
     * unavailable during updates. In such cases, it may be important that the users
     * continue to get access to premium data that they own.
     *
     * The data that lives here should be refreshed at regular intervals so that it reflects what's
     * in the Google Play Store.
     */
    private lateinit var localCacheBillingClient: LocalBillingDb

    private var reconnectBackOffSeconds = RECONNECT_BACK_OFF_DEFAULT_SECONDS

    private val _productDetails = MutableStateFlow(
        SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE.map {
            AugmentedProductDetails(it, false, null)
        })

    /**
     * List of supported products. Updated if they can be purchased after [queryPurchasesAsync] and
     * if they are available on Google Play after [queryProductDetailsAsync].
     * See [AugmentedProductDetails].
     */
    val productDetails: StateFlow<List<AugmentedProductDetails>> = _productDetails

    data class BillingError(
        val debugMessage: String,
        /**
         * Only `true` if connecting to the billing service failed with
         * [BillingResponseCode.BILLING_UNAVAILABLE] or the subscription feature is indicated as
         * not available (but at least currently subscriptions are supported in every region where
         * Play Billing is supported).
         */
        val isUnavailable: Boolean = true
    )

    /** Triggered if there was an error. Contains an error message to display. */
    @Suppress("DEPRECATION")
    val errorEvent = SingleLiveEvent<BillingError>()

    fun startDataSourceConnections() {
        Timber.d("startDataSourceConnections")
        localCacheBillingClient = LocalBillingDb.getInstance(applicationContext)
        if (!connectToPlayBillingService()) {
            // Already connected, so trigger purchases query directly.
            coroutineScope.launch {
                queryPurchasesAsync()
            }
        }
    }

    private fun connectToPlayBillingService(): Boolean {
        Timber.d("connectToPlayBillingService")
        if (!playStoreBillingClient.isReady) {
            playStoreBillingClient.startConnection(billingClientStateListener)
            return true
        }
        return false
    }

    /**
     * BACKGROUND
     *
     * Google Play Billing refers to receipts as [Purchases][Purchase]. So when a user buys
     * something, Play Billing returns a [Purchase] object that the app then uses to release the
     * [Entitlement] to the user. Receipts are pivotal within the [BillingRepository]; but they are
     * not part of the repo’s public API, because clients don’t need to know about them. When
     * the release of entitlements occurs depends on the type of purchase. For consumable products,
     * the release may be deferred until after consumption by Google Play; for non-consumable
     * products and subscriptions, the release may be deferred until after
     * [BillingClient.acknowledgePurchase] is called. You should keep receipts in the local
     * cache for augmented security and for making some transactions easier.
     *
     * THIS METHOD
     *
     * [This method][queryPurchasesAsync] grabs all the active purchases of this user and makes them
     * available to this app instance. Whereas this method plays a central role in the billing
     * system, it should be called at key junctures, such as when user the app starts.
     *
     * Because purchase data is vital to the rest of the app, this method is called each time
     * the [BillingViewModel] successfully establishes connection with the Play [BillingClient]:
     * the call comes through [billingClientStateListener]. Recall also from Figure 4 that this method
     * gets called from inside [purchasesUpdatedListener] in the event that a purchase is "already
     * owned," which can happen if a user buys the item around the same time
     * on a different device.
     */
    private suspend fun queryPurchasesAsync() {
        Timber.d("queryPurchasesAsync called")
        val purchasesResult = HashSet<Purchase>()
        var result = playStoreBillingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        Timber.d("queryPurchasesAsync INAPP results: ${result.purchasesList.size}")
        result.purchasesList.apply { purchasesResult.addAll(this) }
        if (isSubscriptionSupported()) {
            result = playStoreBillingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
            result.purchasesList.apply { purchasesResult.addAll(this) }
            Timber.d("queryPurchasesAsync SUBS results: ${result.purchasesList.size}")
        }
        processPurchases(purchasesResult)
    }

    /**
     * Verifies purchases and passes valid ones to [acknowledgeNonConsumablePurchases].
     *
     * Assumes [allPurchases] has all purchases for a user, not just updated ones. See
     * [acknowledgeNonConsumablePurchases] for details.
     */
    private suspend fun processPurchases(allPurchases: Set<Purchase>) =
        withContext(Dispatchers.IO) {
            val validPurchasesSet = HashSet<Purchase>(allPurchases.size)
            Timber.d("processPurchases called with %s", allPurchases)

            allPurchases.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (isSignatureValid(purchase)) {
                        validPurchasesSet.add(purchase)
                    }
                }
            }

            val validPurchases = validPurchasesSet.toList()
            Timber.d("processPurchases valid purchases %s", validPurchases)

            // To be able to verify purchases are properly processed, they are stored in a local
            // database until they have been processed.
            val testing = localCacheBillingClient.purchaseDao().getPurchases()
            Timber.d("processPurchases purchases in the db ${testing.size}")

            // Note: the following are safe to call with an empty list. Can't decide if unlock state
            // should be revoked here because it depends on if a purchase has a supported product.
            localCacheBillingClient.purchaseDao().insert(validPurchases)
            acknowledgeNonConsumablePurchases(validPurchases)
        }

    /**
     * If needed, acknowledges purchases (so Play will not refund it within a few days of the
     * transaction) and for all purchases where the [Purchase.isAcknowledged] (or an empty list if
     * there aren't any) calls [processAckedPurchases].
     *
     * Assumes all purchases for the user are passed. Otherwise, if only passing a new purchase and
     * acking it fails, unlock state gets revoked even if there might be a purchase that can be used
     * to unlock.
     */
    private suspend fun acknowledgeNonConsumablePurchases(allPurchases: List<Purchase>) {
        val ackedPurchases = mutableListOf<Purchase>()

        allPurchases.forEach { purchase ->
            if (purchase.isAcknowledged) {
                ackedPurchases.add(purchase)
            } else {
                // Acknowledge purchase
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                val billingResult = playStoreBillingClient.acknowledgePurchase(params)
                when (billingResult.responseCode) {
                    BillingResponseCode.OK -> {
                        Timber.i("Acknowledged purchase %s", purchase)
                        ackedPurchases.add(purchase)
                    }

                    else -> {
                        // As currently the user can only make a single purchase at a time, this
                        // should never overwrite an existing ack fail.
                        "Acknowledging purchase failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                            Timber.e(it)
                            errorEvent.postValue(BillingError(it))
                        }
                    }
                }
            }
        }

        processAckedPurchases(ackedPurchases)
    }

    /**
     * Calls [unlockWith] for the first purchase with a supported product (SKU), preferably a
     * subscription (to display the active/purchasable state and enable up- and downgrades in the
     * UI).
     *
     * If there isn't a supported product, revokes unlock state and [enableAllProductsForPurchase].
     *
     * Also removes purchase receipts for all [ackedPurchases] once done.
     */
    private suspend fun processAckedPurchases(ackedPurchases: List<Purchase>) {
        val subscriptionPurchase = ackedPurchases
            .firstNotNullOfOrNull { purchase ->
                purchase.products
                    .find { SeriesGuideSku.supportedSubscriptionSkus.contains(it) }
                    ?.let { Pair(purchase, it) }
            }
        if (subscriptionPurchase != null) {
            unlockWith(
                purchase = subscriptionPurchase.first,
                product = subscriptionPurchase.second,
                isSub = true
            )
        } else {
            // If there is no subscription purchase, take the first non-subscription SKU
            val oneTimePurchase = ackedPurchases
                .firstNotNullOfOrNull { purchase ->
                    purchase.products
                        .find { SeriesGuideSku.supportedOneTimePurchaseSkus.contains(it) }
                        ?.let { Pair(purchase, it) }
                }
            if (oneTimePurchase != null) {
                unlockWith(
                    purchase = oneTimePurchase.first,
                    product = oneTimePurchase.second,
                    isSub = false
                )
            } else {
                Timber.i("Revoking unlock, no purchase with a supported product found")
                revokeUnlock()
                enableAllProductsForPurchase()
            }
        }

        // Remove receipts for (acknowledged and processed) purchases
        ackedPurchases.forEach { purchase ->
            localCacheBillingClient.purchaseDao().delete(purchase)
        }
    }

    /**
     * Sets unlock state to unlocked and adds purchase info, updates [_productDetails] with
     * what can be purchased.
     */
    private suspend fun unlockWith(purchase: Purchase, product: String, isSub: Boolean) {
        Timber.i("Unlocking (isSub=%s, product=%s, purchase=%s)", isSub, product, purchase)
        val unlockState = PlayUnlockState.withLastUpdatedNow(
            true,
            isSub,
            product,
            purchase.purchaseToken
        )
        insertUnlockState(unlockState)
        // Pass unlocked state on as soon as possible
        BillingTools.updateUnlockStateAsync(applicationContext)

        if (isSub) {
            // A user must only have one active subscription.
            // Prevent re-purchase of active one (or the equivalent tier for legacy products),
            // but allow up/downgrade to others.
            val purchasedProduct = when (product) {
                SeriesGuideSku.X_SUB_SUPPORTER,
                SeriesGuideSku.X_SUB_SPONSOR -> product

                else -> {
                    // Also map purchase state of deprecated subscriptions to the base tier
                    // subscription. This will allow upgrades.
                    SeriesGuideSku.X_SUB_ALL_ACCESS
                }
            }
            _productDetails.update { productDetails ->
                productDetails.map {
                    it.copy(canPurchase = it.productId != purchasedProduct)
                }
            }
        } else {
            // If the user only has a one-time purchase, enable purchasing of a subscription to help
            // support future updates.
            enableAllProductsForPurchase()
        }
    }

    private fun enableAllProductsForPurchase() {
        _productDetails.update { currentProducts ->
            currentProducts.map { it.copy(canPurchase = true) }
        }
    }

    /**
     * If unlock state is unlocked and it could not be updated for a year (current period of all
     * subscriptions), revokes unlock state.
     *
     * This is for cases when purchasing using Play, but then Play becomes unavailable (possibly
     * when updating the app from another source). This will honor the past purchase but eventually
     * enable a purchase using another billing method.
     */
    private suspend fun revokeUnlockStateIfLastUpdatedLongAgo() {
        val playUnlockState = localCacheBillingClient.unlockStateHelper().getPlayUnlockState()
        if (playUnlockState == null || !playUnlockState.entitled) {
            return
        }
        // lastUpdatedMs may be null and trigger a revoke if unlock state wasn't updated since
        // lastUpdatedMs was introduced. However, this should be unlikely as the state here is
        // unlocked, indicating Play Billing should be available. In which case this is never
        // called.
        val lastUpdated = Instant.ofEpochMilli(playUnlockState.lastUpdatedMs ?: 0)
        val oneYearAgo = Instant.now().minus(REVOKE_NOT_UPDATED_AFTER_DURATION)
        if (lastUpdated.isBefore(oneYearAgo)) {
            Timber.i("Revoking unlock, purchase state was last updated %s", lastUpdated)
            revokeUnlock()
        }
    }

    private suspend fun revokeUnlock() {
        insertUnlockState(PlayUnlockState.revoked())
    }

    @WorkerThread
    private suspend fun insertUnlockState(unlockState: PlayUnlockState) =
        withContext(Dispatchers.IO) {
            localCacheBillingClient.unlockStateHelper().insert(unlockState)
        }

    /**
     * State may be null if there is no active purchase.
     */
    fun createUnlockStateFlow(): Flow<PlayUnlockState?> {
        return localCacheBillingClient.unlockStateHelper().createPlayUnlockStateFlow()
    }

    /**
     * Requests product details from Google Play and updates the associated [productDetails].
     */
    private suspend fun queryProductDetailsAsync() {
        val products = SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()
        Timber.d("queryProductDetailsAsync for ${SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE}")

        val (billingResult, availableProducts) = playStoreBillingClient.queryProductDetails(params)
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                if (!availableProducts.isNullOrEmpty()) {
                    // Update in the background to avoid blocking the main thread.
                    coroutineScope.launch(Dispatchers.IO) {
                        _productDetails.update { supportedProducts ->
                            supportedProducts.map { supportedProduct ->
                                // Update a supported product with details from Google Play.
                                // It is also possible, that a supported product is not available
                                // on Google Play. Then product details will remain null.
                                availableProducts.find { it.productId == supportedProduct.productId }
                                    ?.let { supportedProduct.copy(productDetails = it) }
                                    ?: supportedProduct
                            }
                        }
                    }
                }
            }

            else -> {
                "querySkuDetailsAsync failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                    Timber.e(it)
                    errorEvent.postValue(BillingError(it))
                }
            }
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [purchasesUpdatedListener].
     */
    fun launchBillingFlow(
        activity: Activity,
        augmentedProductDetails: SafeAugmentedProductDetails
    ) {
        val productId = augmentedProductDetails.productId
        val productDetails = augmentedProductDetails.productDetails
        val offers = productDetails.subscriptionOfferDetails
        if (offers.isNullOrEmpty()) {
            Timber.e("No offers for $productId, can not purchase.")
            return
        }
        // The Billing client sends both offers (as in Play Console) and base plans as offers (as
        // by this API). So if a base plan has a free trial offer it sends:
        // - a free trial offer with two (!) pricing phases: free and price of base plan.
        // - a base plan offer with one pricing phase: price of the base plan.

        // As this app only offers free trials next to the base plan
        // and the free trial offer is not longer sent by Google Play if it was used before:
        // getting the offer with the cheapest first pricing phase will get the trial if available,
        // otherwise the base plan.
        val cheapestOffer = offers
            .filter { it.pricingPhases.pricingPhaseList.isNotEmpty() }
            .minByOrNull { it.pricingPhases.pricingPhaseList[0].priceAmountMicros }
        if (cheapestOffer == null) {
            Timber.e("No offer with pricing phase for $productId, can not purchase.")
            return
        }

        // Check if this is a subscription up- or downgrade.
        val unlockStateOrNull = localCacheBillingClient.unlockStateHelper().getPlayUnlockState()
        val oldSubProductId = unlockStateOrNull?.let { if (it.isSub) it.sku else null }
        val oldPurchaseToken = unlockStateOrNull?.purchaseToken

        val purchaseParams = BillingFlowParams.newBuilder().apply {
            setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(cheapestOffer.offerToken)
                        .build()
                )
            )
            if (oldSubProductId != null && oldPurchaseToken != null) {
                val prorationMode = if (
                    oldSubProductId == SeriesGuideSku.X_SUB_SPONSOR
                    || (oldSubProductId == SeriesGuideSku.X_SUB_SUPPORTER
                            && productId == SeriesGuideSku.X_SUB_ALL_ACCESS)
                ) {
                    // Downgrade immediately, bill new price once renewed.
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION
                } else {
                    // Upgrade immediately, credit existing purchase.
                    BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION
                }
                setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldPurchaseToken(oldPurchaseToken)
                        .setSubscriptionReplacementMode(prorationMode)
                        .build()
                )
            }
        }.build()
        playStoreBillingClient.launchBillingFlow(activity, purchaseParams)
    }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        return Security.verifyPurchase(
            BuildConfig.IAP_KEY_A + BuildConfig.IAP_KEY_B
                    + BuildConfig.IAP_KEY_C + BuildConfig.IAP_KEY_D,
            purchase.originalJson,
            purchase.signature
        )
    }

    /**
     * Checks if the user's device supports subscriptions
     */
    private fun isSubscriptionSupported(): Boolean {
        val billingResult =
            playStoreBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        var succeeded = false
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> succeeded = true
            else -> {
                "isSubscriptionSupported failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                    Timber.e(it)
                    errorEvent.postValue(BillingError(it, isUnavailable = true))
                }
            }
        }
        return succeeded
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        /**
         * This method is called by the [playStoreBillingClient] when new purchases are detected.
         * The purchase list in this method is not the same as the one in
         * [queryPurchasesAsync]. Whereas it returns everything this user owns, this only returns
         * the items that were just now purchased or billed.
         */
        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                // Just get all purchases to avoid unlock state getting revoked if ack for an
                // updated purchase fails, but there is an existing one that can be used to unlock.
                coroutineScope.launch {
                    queryPurchasesAsync()
                }
            }

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Get all purchases to refresh can purchase and unlock state
                Timber.d(billingResult.debugMessage)
                coroutineScope.launch {
                    queryPurchasesAsync()
                }
            }

            BillingResponseCode.USER_CANCELED -> {
                Timber.i("onPurchasesUpdated: User canceled the purchase.")
            }

            else -> {
                "Processing updated purchases failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                    Timber.e(it)
                    errorEvent.postValue(BillingError(it))
                }
            }
        }
    }

    private val billingClientStateListener = object : BillingClientStateListener {
        /**
         * The connection to the Play [BillingClient] has been established.
         * If the connection is OK, get available products, then get purchases.
         * Otherwise post an error event.
         */
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            reconnectBackOffSeconds = RECONNECT_BACK_OFF_DEFAULT_SECONDS
            when (billingResult.responseCode) {
                BillingResponseCode.OK -> {
                    Timber.d("onBillingSetupFinished successfully")
                    coroutineScope.launch {
                        queryProductDetailsAsync()
                        queryPurchasesAsync()
                    }
                }

                else -> {
                    Timber.d(billingResult.debugMessage)
                    val debugMessage =
                        "${billingResult.responseCode}: ${billingResult.debugMessage}"
                    val isUnavailable =
                        billingResult.responseCode == BillingResponseCode.BILLING_UNAVAILABLE
                    errorEvent.postValue(BillingError(debugMessage, isUnavailable))
                    if (isUnavailable) {
                        coroutineScope.launch {
                            revokeUnlockStateIfLastUpdatedLongAgo()
                        }
                    }
                }
            }
        }

        /**
         * This method is called when the app has inadvertently disconnected from the [BillingClient].
         * An attempt should be made to reconnect using a retry policy.
         *
         * This is a pretty unusual occurrence. It happens primarily if the Google Play Store
         * self-upgrades or is force closed.
         */
        override fun onBillingServiceDisconnected() {
            Timber.d("onBillingServiceDisconnected")
            // Try to reconnect indefinitely, the app might be running a long time and the user
            // might not regularly visit the billing activity triggering a reconnect.
            val backOffSeconds = max(reconnectBackOffSeconds * 2, RECONNECT_BACK_OFF_MAX_SECONDS)
                .also { reconnectBackOffSeconds = it }
            coroutineScope.launch {
                delay(backOffSeconds * 1000L)
                // Fine to call multiple times, will do nothing if already connected.
                connectToPlayBillingService()
            }
        }

    }

    companion object {
        @Volatile
        private var INSTANCE: BillingRepository? = null

        private const val RECONNECT_BACK_OFF_DEFAULT_SECONDS = 1
        private const val RECONNECT_BACK_OFF_MAX_SECONDS = 15 * 60 // 15 minutes
        private val REVOKE_NOT_UPDATED_AFTER_DURATION = Duration.ofDays(365)

        fun getInstance(context: Context, coroutineScope: CoroutineScope): BillingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingRepository(context.applicationContext, coroutineScope)
                    .also { INSTANCE = it }
            }
    }

    object SeriesGuideSku {
        // The SKU product ids as set in the Developer Console
        const val X_PASS_IN_APP = "x_upgrade"

        const val X_SUB_LEGACY = "x_subscription"
        const val X_SUB_2014_02 = "x_sub_2014_02"
        const val X_SUB_2016_05 = "x_sub_2016_05"
        const val X_SUB_ALL_ACCESS = "x_sub_2017_08"
        const val X_SUB_SUPPORTER = "sub_supporter"
        const val X_SUB_SPONSOR = "sub_sponsor"

        // Only to test display of trial, should never be purchased
        const val SUB_TEST = "sub_test"

        val SUBS_SKUS_FOR_PURCHASE = if (BuildConfig.DEBUG) {
            listOf(
                X_SUB_ALL_ACCESS,
                X_SUB_SUPPORTER,
                X_SUB_SPONSOR,
                SUB_TEST
            )
        } else {
            listOf(
                X_SUB_ALL_ACCESS,
                X_SUB_SUPPORTER,
                X_SUB_SPONSOR
            )
        }

        val supportedSubscriptionSkus = listOf(
            X_SUB_LEGACY,
            X_SUB_2014_02,
            X_SUB_2016_05,
            X_SUB_ALL_ACCESS,
            X_SUB_SUPPORTER,
            X_SUB_SPONSOR
        )
        val supportedOneTimePurchaseSkus = listOf(
            X_PASS_IN_APP
        )
    }

}