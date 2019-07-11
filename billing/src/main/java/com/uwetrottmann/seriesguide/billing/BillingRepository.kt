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
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails
import com.uwetrottmann.seriesguide.billing.localdb.Entitlement
import com.uwetrottmann.seriesguide.billing.localdb.GoldStatus
import com.uwetrottmann.seriesguide.billing.localdb.LocalBillingDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class BillingRepository(private val applicationContext: Context) {

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
    private lateinit var playStoreBillingClient: BillingClient

    /**
     * A local cache billing client is important in that the Play Store may be temporarily
     * unavailable during updates. In such cases, it may be important that the users
     * continue to get access to premium data that they own.
     *
     * The data that lives here should be refreshed at regular intervals so that it reflects what's
     * in the Google Play Store.
     */
    private lateinit var localCacheBillingClient: LocalBillingDb

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
        localCacheBillingClient.entitlementsDao().getGoldStatus()
    }

    /**
     * Correlated data sources belong inside a repository module so that the rest of
     * the app can have appropriate access to the data it needs. Still, it may be effective to
     * track the opening (and sometimes closing) of data source connections based on lifecycle
     * events. One convenient way of doing that is by calling this
     * [startDataSourceConnections] when the [BillingViewModel] is instantiated and
     * [endDataSourceConnections] inside [BillingViewModel.onCleared].
     */
    fun startDataSourceConnections() {
        Timber.d("startDataSourceConnections")
        localCacheBillingClient = LocalBillingDb.getInstance(applicationContext)
        instantiateAndConnectToPlayBillingService()
    }

    fun endDataSourceConnections() {
        playStoreBillingClient.endConnection()
        // normally you don't worry about closing a DB connection unless you have more than
        // one DB open. so no need to call 'localCacheBillingClient.close()'
        Timber.d("endDataSourceConnections")
    }

    private fun instantiateAndConnectToPlayBillingService() {
        playStoreBillingClient = BillingClient.newBuilder(applicationContext)
            .enablePendingPurchases() // required or app will crash
            .setListener(purchasesUpdatedListener)
            .build()
        connectToPlayBillingService()
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
    fun queryPurchasesAsync() {
        Timber.d("queryPurchasesAsync called")
        val purchasesResult = HashSet<Purchase>()
        var result = playStoreBillingClient.queryPurchases(BillingClient.SkuType.INAPP)
        Timber.d("queryPurchasesAsync INAPP results: ${result?.purchasesList?.size}")
        result?.purchasesList?.apply { purchasesResult.addAll(this) }
        if (isSubscriptionSupported()) {
            result = playStoreBillingClient.queryPurchases(BillingClient.SkuType.SUBS)
            result?.purchasesList?.apply { purchasesResult.addAll(this) }
            Timber.d("queryPurchasesAsync SUBS results: ${result?.purchasesList?.size}")
        }
        processPurchases(purchasesResult)
    }

    private fun processPurchases(purchasesResult: Set<Purchase>) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            Timber.d("processPurchases called")
            val validPurchases = HashSet<Purchase>(purchasesResult.size)
            Timber.d("processPurchases newBatch content $purchasesResult")

            purchasesResult.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    if (isSignatureValid(purchase)) {
                        validPurchases.add(purchase)
                    }
                } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                    Timber.d("Received a pending purchase of SKU: ${purchase.sku}")
                    // handle pending purchases, e.g. confirm with users about the pending
                    // purchases, prompt them to complete it, etc.
                }
            }

            Timber.d("processPurchases non-consumables content $validPurchases")
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
            localCacheBillingClient.purchaseDao().insert(*validPurchases.toTypedArray())
            acknowledgeNonConsumablePurchasesAsync(validPurchases.toList())
        }

    /**
     * If you do not acknowledge a purchase, the Google Play Store will provide a refund to the
     * users within a few days of the transaction. Therefore you have to implement
     * [BillingClient.acknowledgePurchase] inside your app.
     */
    private fun acknowledgeNonConsumablePurchasesAsync(nonConsumables: List<Purchase>) {
        nonConsumables.forEach { purchase ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            playStoreBillingClient.acknowledgePurchase(params) { billingResult ->
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        disburseNonConsumableEntitlement(purchase)
                    }
                    else -> Timber.d("acknowledgeNonConsumablePurchasesAsync response is ${billingResult.debugMessage}")
                }
            }

        }
    }

    /**
     * This is the final step, where purchases/receipts are converted to premium contents.
     */
    private fun disburseNonConsumableEntitlement(purchase: Purchase) =
        CoroutineScope(Job() + Dispatchers.IO).launch {
            when (purchase.sku) {
                SeriesGuideSku.X_PASS_IN_APP,
                SeriesGuideSku.X_SUB_LEGACY,
                SeriesGuideSku.X_SUB_2014_02,
                SeriesGuideSku.X_SUB_2016_05,
                SeriesGuideSku.X_SUB_ALL_ACCESS,
                SeriesGuideSku.X_SUB_SUPPORTER,
                SeriesGuideSku.X_SUB_SPONSOR -> {
                    val goldStatus = GoldStatus(true)
                    insert(goldStatus)
                    /* You can only buy all access once. Disable all available subscriptions. */
                    SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE.forEach { sku ->
                        localCacheBillingClient.skuDetailsDao()
                            .insertOrUpdate(sku, goldStatus.mayPurchase())
                    }
                }
                else -> Timber.e("Sku ${purchase.sku} not recognized.")
            }
            // Entitlement processed, remove receipt.
            localCacheBillingClient.purchaseDao().delete(purchase)
        }

    @WorkerThread
    private suspend fun insert(entitlement: Entitlement) = withContext(Dispatchers.IO) {
        localCacheBillingClient.entitlementsDao().insert(entitlement)
    }

    /**
     * Requests SKU details from Google Play and adds or updates the associated [AugmentedSkuDetails].
     */
    private fun querySkuDetailsAsync(
        @BillingClient.SkuType skuType: String,
        skuList: List<String>
    ) {
        val params = SkuDetailsParams.newBuilder()
            .setSkusList(skuList)
            .setType(skuType)
            .build()
        Timber.d("querySkuDetailsAsync for $skuType")
        playStoreBillingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (skuDetailsList.orEmpty().isNotEmpty()) {
                        skuDetailsList.forEach {
                            CoroutineScope(Job() + Dispatchers.IO).launch {
                                localCacheBillingClient.skuDetailsDao().insertOrUpdate(it)
                            }
                        }
                    }
                }
                else -> {
                    Timber.e(billingResult.debugMessage)
                }
            }
        }
    }

    /**
     * This is the function to call when user wishes to make a purchase. This function will
     * launch the Google Play Billing flow. The response to this call is returned in
     * [purchasesUpdatedListener].
     */
    fun launchBillingFlow(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) =
        launchBillingFlow(activity, SkuDetails(augmentedSkuDetails.originalJson))

    fun launchBillingFlow(activity: Activity, skuDetails: SkuDetails) {
        val purchaseParams = BillingFlowParams.newBuilder()
            .setSkuDetails(skuDetails)
            .build()
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
            else -> Timber.w("isSubscriptionSupported() error: ${billingResult.debugMessage}")
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
                purchases?.apply { processPurchases(this.toSet()) }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // item already owned? call queryPurchasesAsync to verify and process all such items
                Timber.d(billingResult.debugMessage)
                queryPurchasesAsync()
            }
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED -> {
                connectToPlayBillingService()
            }
            else -> {
                Timber.i(billingResult.debugMessage)
            }
        }
    }

    private val billingClientStateListener = object : BillingClientStateListener {
        /**
         * This is the callback for when connection to the Play [BillingClient] has been successfully
         * established. It might make sense to get [SkuDetails] and [Purchases][Purchase] at this point.
         */
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    Timber.d("onBillingSetupFinished successfully")
                    querySkuDetailsAsync(
                        BillingClient.SkuType.SUBS,
                        SeriesGuideSku.SUBS_SKUS_FOR_PURCHASE
                    )
                    queryPurchasesAsync()
                }
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> {
                    //Some apps may choose to make decisions based on this knowledge.
                    Timber.d(billingResult.debugMessage)
                }
                else -> {
                    // Do nothing. Someone else will connect it through retry policy.
                    // May choose to send to server though.
                    Timber.d(billingResult.debugMessage)
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
            // TODO Exponential back-off.
            connectToPlayBillingService()
        }

    }

    companion object {
        @Volatile
        private var INSTANCE: BillingRepository? = null

        @JvmStatic
        fun getInstance(context: Context): BillingRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: BillingRepository(context.applicationContext)
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