package com.uwetrottmann.seriesguide.billing

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.SkuDetails
import com.android.billingclient.api.SkuDetailsParams
import com.android.billingclient.api.queryPurchasesAsync
import com.android.billingclient.api.querySkuDetails
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails
import com.uwetrottmann.seriesguide.billing.localdb.Entitlement
import com.uwetrottmann.seriesguide.billing.localdb.GoldStatus
import com.uwetrottmann.seriesguide.billing.localdb.LocalBillingDb
import com.uwetrottmann.seriesguide.common.SingleLiveEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.roundToLong

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
            .enablePendingPurchases()  // Not used for subscriptions.
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

    private var disconnectCount = 0

    /**
     * This list tells clients what subscriptions are available for sale.
     */
    val subsSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>> by lazy {
        if (!::localCacheBillingClient.isInitialized) {
            localCacheBillingClient = LocalBillingDb.getInstance(applicationContext)
        }
        localCacheBillingClient.skuDetailsDao().getSubscriptionSkuDetails()
    }

    /**
     * Tracks whether this user is entitled to gold status. This call returns data from the app's
     * own local DB; this way if Play and the secure server are unavailable, users still have
     * access to features they purchased.  Normally this would be a good place to update the local
     * cache to make sure it's always up-to-date. However, onBillingSetupFinished already called
     * queryPurchasesAsync for you; so no need.
     */
    val goldStatusLiveData: LiveData<GoldStatus> by lazy {
        if (!::localCacheBillingClient.isInitialized) {
            localCacheBillingClient = LocalBillingDb.getInstance(applicationContext)
        }
        localCacheBillingClient.entitlementsDao().getGoldStatusLiveData()
    }

    /** Triggered when the entitlement was revoked. Use only with one observer at a time. */
    val entitlementRevokedEvent = SingleLiveEvent<Void>()

    /** Triggered if there was an error. Contains an error message to display. */
    val errorEvent = SingleLiveEvent<String>()

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
        var result = playStoreBillingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP)
        Timber.d("queryPurchasesAsync INAPP results: ${result.purchasesList.size}")
        result.purchasesList.apply { purchasesResult.addAll(this) }
        if (isSubscriptionSupported()) {
            result = playStoreBillingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS)
            result.purchasesList.apply { purchasesResult.addAll(this) }
            Timber.d("queryPurchasesAsync SUBS results: ${result.purchasesList.size}")
        }
        processPurchases(purchasesResult)
    }

    private suspend fun processPurchases(purchasesResult: Set<Purchase>) =
        withContext(Dispatchers.IO) {
            Timber.d("processPurchases called")
            val validPurchasesSet = HashSet<Purchase>(purchasesResult.size)
            Timber.d("processPurchases newBatch content $purchasesResult")

            purchasesResult.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (isSignatureValid(purchase)) {
                        validPurchasesSet.add(purchase)
                    }
                }
            }

            val validPurchases = validPurchasesSet.toList()
            Timber.d("processPurchases valid purchases $validPurchases")
            /*
              As is being done in this sample, for extra reliability you may store the
              receipts/purchases to a your own remote/local database for until after you
              disburse entitlements. That way if the Google Play Billing library fails at any
              given point, you can independently verify whether entitlements were accurately
              disbursed. In this sample, the receipts are then removed upon entitlement
              disbursement.
             */
            val testing = localCacheBillingClient.purchaseDao().getPurchases()
            Timber.d("processPurchases purchases in the db ${testing.size}")

            if (validPurchases.isEmpty()) {
                // This should only happen after querying all purchases.
                // When called from PurchasesUpdatedListener there should always be one.
                revokeEntitlement()
            } else {
                localCacheBillingClient.purchaseDao().insert(validPurchases)
                acknowledgeNonConsumablePurchasesAsync(validPurchases)
            }
        }

    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchase] inside your app.
     */
    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: List<Purchase>) {
        nonConsumables.forEach { purchase ->
            if (purchase.isAcknowledged) {
                // Already acknowledged, immediately grant entitlement.
                disburseEntitlement(purchase)
            } else {
                // Acknowledge purchase.
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                playStoreBillingClient.acknowledgePurchase(params) { billingResult ->
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            disburseEntitlement(purchase)
                        }
                        else -> {
                            "acknowledgeNonConsumablePurchasesAsync failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                                Timber.e(it)
                                errorEvent.postValue(it)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This is the final step, where purchases/receipts are converted to premium contents.
     */
    private fun disburseEntitlement(purchase: Purchase) =
        coroutineScope.launch(Dispatchers.IO) {
            when (val purchaseSku = purchase.skus[0]) {
                SeriesGuideSku.X_PASS_IN_APP,
                SeriesGuideSku.X_SUB_LEGACY,
                SeriesGuideSku.X_SUB_2014_02,
                SeriesGuideSku.X_SUB_2016_05,
                SeriesGuideSku.X_SUB_ALL_ACCESS,
                SeriesGuideSku.X_SUB_SUPPORTER,
                SeriesGuideSku.X_SUB_SPONSOR -> {
                    val isSub = SeriesGuideSku.X_PASS_IN_APP != purchaseSku

                    val goldStatus = GoldStatus(true, isSub, purchaseSku, purchase.purchaseToken)
                    insert(goldStatus)

                    // You can only have one subscription. Prevent re-purchase of active one.
                    val activeSku = when (purchaseSku) {
                        SeriesGuideSku.X_SUB_SUPPORTER,
                        SeriesGuideSku.X_SUB_SPONSOR -> purchaseSku
                        else -> {
                            // Show deprecated subscription SKUs and in-app SKU
                            // as if they were All Access.
                            SeriesGuideSku.X_SUB_ALL_ACCESS
                        }
                    }
                    localCacheBillingClient.skuDetailsDao()
                        .insertOrUpdate(activeSku, goldStatus.mayPurchase())
                    // Allow up- and downgrading to other subscription tiers.
                    SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE.forEach { otherSku ->
                        if (otherSku != activeSku) {
                            localCacheBillingClient.skuDetailsDao()
                                .insertOrUpdate(otherSku, !goldStatus.mayPurchase())
                        }
                    }
                }
                else -> Timber.e("Sku $purchaseSku not recognized.")
            }
            // Entitlement processed, remove receipt.
            localCacheBillingClient.purchaseDao().delete(purchase)
        }

    private fun revokeEntitlement() =
        coroutineScope.launch(Dispatchers.IO) {
            // Save if existing entitlement is getting revoked.
            val wasEntitled =
                localCacheBillingClient.entitlementsDao().getGoldStatus()?.entitled ?: false

            val goldStatus = GoldStatus(false, isSub = true, sku = null, purchaseToken = null)
            insert(goldStatus)
            /* Enable all available subscriptions. */
            SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE.forEach { sku ->
                localCacheBillingClient.skuDetailsDao()
                    .insertOrUpdate(sku, goldStatus.mayPurchase())
            }

            // Notify if existing entitlement was revoked.
            if (wasEntitled) {
                withContext(Dispatchers.Main) {
                    entitlementRevokedEvent.call()
                }
            }
        }

    @WorkerThread
    private suspend fun insert(entitlement: Entitlement) = withContext(Dispatchers.IO) {
        localCacheBillingClient.entitlementsDao().insert(entitlement)
    }

    /**
     * Requests SKU details from Google Play and adds or updates the associated [AugmentedSkuDetails].
     */
    private suspend fun querySkuDetailsAsync() {
        val skuType = BillingClient.SkuType.SUBS
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE)
            .setType(skuType)
            .build()
        Timber.d("querySkuDetailsAsync for $skuType")
        val (billingResult, skuDetailsList) = playStoreBillingClient.querySkuDetails(params)
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (skuDetailsList != null && skuDetailsList.isNotEmpty()) {
                    skuDetailsList.forEach {
                        coroutineScope.launch(Dispatchers.IO) {
                            localCacheBillingClient.skuDetailsDao().insertOrUpdate(it)
                        }
                    }
                }
            }
            else -> {
                "querySkuDetailsAsync failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                    Timber.e(it)
                    errorEvent.postValue(it)
                }
            }
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [purchasesUpdatedListener].
     */
    fun launchBillingFlow(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) {
        if (augmentedSkuDetails.originalJson == null) {
            Timber.e("augmentedSkuDetails.originalJson is null")
            return
        }
        val skuDetails = SkuDetails(augmentedSkuDetails.originalJson)

        // Check if this is a subscription up- or downgrade.
        val goldStatusOrNull = localCacheBillingClient.entitlementsDao().getGoldStatus()
        val oldSku = goldStatusOrNull?.let {
            if (it.isSub) it.sku else null
        }
        val purchaseToken = goldStatusOrNull?.purchaseToken

        val prorationMode = if (
            oldSku == SeriesGuideSku.X_SUB_SPONSOR
            || (oldSku == SeriesGuideSku.X_SUB_SUPPORTER && skuDetails.sku == SeriesGuideSku.X_SUB_ALL_ACCESS)
        ) {
            // Downgrade immediately, bill new price once renewed.
            BillingFlowParams.ProrationMode.IMMEDIATE_WITHOUT_PRORATION
        } else {
            // Upgrade immediately, credit existing purchase. Or oldSku == null.
            BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION
        }

        val purchaseParams = BillingFlowParams.newBuilder().apply {
            setSkuDetails(skuDetails)
            if (oldSku != null && purchaseToken != null) {
                setSubscriptionUpdateParams(
                    BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                        .setOldSkuPurchaseToken(purchaseToken)
                        .setReplaceSkusProrationMode(prorationMode)
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
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> connectToPlayBillingService()
            BillingClient.BillingResponseCode.OK -> succeeded = true
            else -> {
                "isSubscriptionSupported failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                    Timber.e(it)
                    errorEvent.postValue(it)
                }
            }
        }
        return succeeded
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        /**
         * This method is called by the [playStoreBillingClient] when new purchases are detected.
         * The purchase list in this method is not the same as the one in
         * [queryPurchases][BillingClient.queryPurchases]. Whereas queryPurchases returns everything
         * this user owns, this only returns the items that were just now purchased or
         * billed.
         */
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // will handle server verification, consumables, and updating the local cache
                purchases?.also {
                    coroutineScope.launch {
                        processPurchases(it.toSet())
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // item already owned? call queryPurchasesAsync to verify and process all such items
                Timber.d(billingResult.debugMessage)
                coroutineScope.launch {
                    queryPurchasesAsync()
                }
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                connectToPlayBillingService()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Timber.i("onPurchasesUpdated: User canceled the purchase.")
            }
            else -> {
                "onPurchasesUpdated failed. ${billingResult.responseCode}: ${billingResult.debugMessage}".let {
                    Timber.e(it)
                    errorEvent.postValue(it)
                }
            }
        }
    }

    private val billingClientStateListener = object : BillingClientStateListener {
        /**
         * This is the callback for when connection to the Play [BillingClient] has been successfully
         * established. It might make sense to get [SkuDetails] and [Purchases][Purchase] at this point.
         */
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            disconnectCount = 0
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Timber.d("onBillingSetupFinished successfully")
                    coroutineScope.launch {
                        querySkuDetailsAsync()
                        queryPurchasesAsync()
                    }
                }
                else -> {
                    Timber.d(billingResult.debugMessage)
                    errorEvent.postValue("${billingResult.responseCode}: ${billingResult.debugMessage}")
                }
            }
        }

        /**
         * This method is called when the app has inadvertently disconnected from the [BillingClient].
         * An attempt should be made to reconnect using a retry policy. Note the distinction between
         * [endConnection][BillingClient.endConnection] and disconnected:
         * - disconnected means it's okay to try reconnecting.
         * - endConnection means the [playStoreBillingClient] must be re-instantiated and then start
         *   a new connection because a [BillingClient] instance is invalid after endConnection has
         *   been called.
         */
        override fun onBillingServiceDisconnected() {
            Timber.d("onBillingServiceDisconnected")
            if (disconnectCount > 3) {
                "Billing service reconnection failed.".let {
                    Timber.e(it)
                    errorEvent.postValue(it)
                }
                return // Do not try again. Wait until BillingClient is started again.
            }
            disconnectCount++
            coroutineScope.launch {
                delay((2.toDouble().pow(disconnectCount) * 1000).roundToLong())
                connectToPlayBillingService()
            }
        }

    }

    companion object {
        @Volatile
        private var INSTANCE: BillingRepository? = null

        fun getInstance(context: Context, coroutineScope: CoroutineScope): BillingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingRepository(context.applicationContext, coroutineScope)
                    .also { INSTANCE = it }
            }
    }

    private object SeriesGuideSku {
        // The SKU product ids as set in the Developer Console
        const val X_PASS_IN_APP = "x_upgrade"

        const val X_SUB_LEGACY = "x_subscription"
        const val X_SUB_2014_02 = "x_sub_2014_02"
        const val X_SUB_2016_05 = "x_sub_2016_05"
        const val X_SUB_ALL_ACCESS = "x_sub_2017_08"
        const val X_SUB_SUPPORTER = "sub_supporter"
        const val X_SUB_SPONSOR = "sub_sponsor"

        val SUBS_SKUS_FOR_PURCHASE = listOf(
            X_SUB_ALL_ACCESS,
            X_SUB_SUPPORTER,
            X_SUB_SPONSOR
        )
    }

}