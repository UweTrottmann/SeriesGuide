package com.battlelancer.seriesguide.billing.amazon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.ProductType;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

public class AmazonIapManager implements AmazonIapManagerInterface {

    public static class AmazonIapMessageEvent {
        public final int messageResId;

        public AmazonIapMessageEvent(int messageResId) {
            this.messageResId = messageResId;
        }
    }

    /**
     * Posted if the contained product/SKU is available on the current store, contains pricing
     * information. This does not yet mean that the user can or still needs to purchase the
     * product.
     */
    public static class AmazonIapProductEvent {
        public final Product product;

        public AmazonIapProductEvent(Product product) {
            this.product = product;
        }
    }

    /**
     * Posted once it can be determined if the user could purchase a SKU and if she has already an
     * active purchase to support the app.
     */
    public static class AmazonIapAvailabilityEvent {
        public final boolean subscriptionAvailable;
        public final boolean passAvailable;
        public final boolean userHasActivePurchase;

        public AmazonIapAvailabilityEvent(boolean subscriptionAvailable, boolean passAvailable,
                boolean userHasActivePurchase) {
            this.subscriptionAvailable = subscriptionAvailable;
            this.passAvailable = passAvailable;
            this.userHasActivePurchase = userHasActivePurchase;
        }
    }

    private final Context context;
    private final PurchaseDataSource dataSource;
    private final AmazonPurchasingListener purchasingListener;

    private boolean userDataAvailable;
    private boolean subscriptionAvailable;
    private boolean passAvailable;
    private UserIapData userIapData;

    public AmazonIapManager(Context context) {
        this.context = context.getApplicationContext();
        this.dataSource = new PurchaseDataSource(context);
        this.purchasingListener = new AmazonPurchasingListener(this);
        this.userDataAvailable = false;
        this.subscriptionAvailable = false;
        this.userIapData = null;
    }

    /**
     * Sets up Amazon IAP.
     *
     * Ensure to call this in `onCreate` of any activity before making calls to any other methods.
     */
    @Override
    public void register() {
        // Internally registers ActivityLifecycleCallbacks on application context
        // if no instance exists, so safe to call multiple times.
        // Purchase listener is always updated.
        PurchasingService.registerListener(context, getPurchasingListener());
        // Note: Documented getAppstoreSDKMode() API is not available.
        // Timber.i("IAP sandbox mode is: %s", PurchasingService.IS_SANDBOX_MODE);
    }

    private AmazonPurchasingListener getPurchasingListener() {
        return purchasingListener;
    }

    /**
     * Call this in e.g. {@code onStart} to request data about all available products.
     *
     * <p> If successful, a {@link AmazonIapProductEvent} will be posted.
     */
    @Override
    public void requestProductData() {
        // get product availability
        Timber.d("getProductData");
        final Set<String> productSkus = new HashSet<>();
        for (final AmazonSku mySku : AmazonSku.values()) {
            productSkus.add(mySku.getSku());
        }
        PurchasingService.getProductData(productSkus);
    }

    /**
     * Call this in {@code onResume} to update the current user's data and to request the latest
     * purchase updates from Amazon.
     *
     * <p> If successful, one or more {@link com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapAvailabilityEvent}
     * will be posted.
     *
     * <p> In addition on failure, a {@link com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapMessageEvent}
     * with an error message will be posted.
     */
    @Override
    public void requestUserDataAndPurchaseUpdates() {
        Timber.d("getUserData");
        PurchasingService.getUserData();
        Timber.d("getPurchaseUpdates");
        PurchasingService.getPurchaseUpdates(false);
    }

    /**
     * Connect to the database when main activity's {@code onResume} before calling {@link
     * #requestUserDataAndPurchaseUpdates()}.
     */
    @Override
    public void activate() {
        dataSource.open();
    }

    /**
     * Gracefully close the database in the activity's {@code onPause}.
     */
    @Override
    public void deactivate() {
        dataSource.close();
    }

    /**
     * Checks if the current user has an active subscription or pass purchase. If the purchase of
     * the user is not valid any longer {@link com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity}
     * is launched.
     *
     * <p>If user or purchase data could not be fetched, keeps the last subscription state.
     */
    @Override
    public void validateSupporterState(@NonNull Activity activity) {
        Timber.d("validateSubscription");
        boolean isSupporter;
        if (userIapData != null) {
            loadPurchaseRecords();

            isSupporter = userIapData.hasActivePurchase();
        } else if (userDataAvailable) {
            // user is not logged in
            isSupporter = false;
        } else {
            // data could not be fetched, keep last sub state
            return;
        }

        // update state
        boolean isSupporterOld = AdvancedSettings.getLastSupporterState(activity);
        AdvancedSettings.setSupporterState(activity, isSupporter);

        // notify if purchase has expired
        if (isSupporterOld && !isSupporter) {
            activity.startActivity(new Intent(activity, AmazonBillingActivity.class));
            activity.finish();
        }
    }

    /**
     * Method to set the app's amazon user id and marketplace from IAP SDK responses.
     */
    protected void setAmazonUserId(final String newAmazonUserId,
            final String newAmazonMarketplace) {
        userDataAvailable = true;

        // Reload everything if the Amazon user has changed.
        if (newAmazonUserId == null) {
            // A null user id typically means there is no registered Amazon account.
            if (userIapData != null) {
                userIapData = null;
                refreshPurchasesAvailability();
            }
        } else if (userIapData == null || !newAmazonUserId.equals(userIapData.getAmazonUserId())) {
            // If there was no existing Amazon user then either no customer was
            // previously registered or the application has just started.

            // If the user id does not match then another Amazon user has registered.
            userIapData = new UserIapData(newAmazonUserId, newAmazonMarketplace);
            refreshPurchasesAvailability();
        }
    }

    protected void enablePurchaseForSkus(final Map<String, Product> productData) {
        Product product = productData.get(AmazonSku.SERIESGUIDE_SUB_YEARLY.getSku());
        if (product != null) {
            subscriptionAvailable = true;
            EventBus.getDefault().post(new AmazonIapProductEvent(product));
        }
        product = productData.get(AmazonSku.SERIESGUIDE_PASS.getSku());
        if (product != null) {
            passAvailable = true;
            EventBus.getDefault().post(new AmazonIapProductEvent(product));
        }
    }

    protected void disablePurchaseForSkus(final Set<String> unavailableSkus) {
        // reasons for product not available can be:
        // * Item not available for this country
        // * Item pulled off from Appstore by developer
        // * Item pulled off from Appstore by Amazon
        if (unavailableSkus.contains(AmazonSku.SERIESGUIDE_SUB_YEARLY.toString())) {
            subscriptionAvailable = false;
            EventBus.getDefault()
                    .post(new AmazonIapMessageEvent(R.string.subscription_unavailable));
        }
        if (unavailableSkus.contains(AmazonSku.SERIESGUIDE_PASS.toString())) {
            passAvailable = false;
            EventBus.getDefault().post(new AmazonIapMessageEvent(R.string.pass_unavailable));
        }
    }

    protected void disableAllPurchases() {
        subscriptionAvailable = false;
        passAvailable = false;
        refreshPurchasesAvailability();
    }

    /**
     * Method to handle Entitlement Purchase
     */
    private void handleEntitlementPurchase(final Receipt receipt, final UserData userData) {
        try {
            if (receipt.isCanceled()) {
                // Check whether this receipt is to revoke a entitlement
                // purchase
                revokeEntitlement(receipt, userData.getUserId());
            } else {
                // We strongly recommend that you verify the receipt
                // server-side.

                // don't care for now
//                if (!verifyReceiptFromYourService(receipt.getReceiptId(), userData)) {
//                    // if the purchase cannot be verified,
//                    // show relevant error message to the customer.
//                    return;
//                }
                grantPurchase(receipt, userData);
            }
        } catch (final Throwable e) {
            EventBus.getDefault().post(new AmazonIapMessageEvent(R.string.subscription_failed));
        }
    }

    /**
     * This method contains the business logic to fulfill the customer's purchase based on the
     * receipt received from InAppPurchase SDK's {@link com.amazon.device.iap.PurchasingListener#onPurchaseResponse}
     * or {@link com.amazon.device.iap.PurchasingListener#onPurchaseUpdatesResponse}.
     */
    protected void handleSubscriptionPurchase(final Receipt receipt, final UserData userData) {
        try {
            if (receipt.isCanceled()) {
                // Check whether this receipt is for an expired or canceled subscription
                revokeSubscription(receipt);
            } else {
                // We strongly recommend that you verify the receipt on server-side.

                // don't care for now
                //if (!verifyReceiptFromYourService(receipt.getReceiptId(), userData)) {
                //    // if the purchase cannot be verified,
                //    // show relevant error message to the customer.
                //    return;
                //}

                grantPurchase(receipt, userData);
            }
        } catch (Throwable e) {
            EventBus.getDefault().post(new AmazonIapMessageEvent(R.string.subscription_failed));
        }
    }

    private void grantPurchase(final Receipt receipt, final UserData userData) {
        final AmazonSku amazonSku = AmazonSku.fromSku(receipt.getSku());
        // Verify that the SKU is still applicable.
        // for subscriptions receipts contain the parent SKU, not the purchased period SKU
        if (amazonSku != AmazonSku.SERIESGUIDE_SUB_PARENT
                && amazonSku != AmazonSku.SERIESGUIDE_PASS) {
            Timber.w("The SKU [%s] in the receipt is not valid anymore", receipt.getSku());
            // if the sku is not applicable anymore, call
            // PurchasingService.notifyFulfillment with status "UNAVAILABLE"
            PurchasingService.notifyFulfillment(receipt.getReceiptId(),
                    FulfillmentResult.UNAVAILABLE);
            return;
        }
        try {
            // Set the purchase status to fulfilled for your application
            savePurchaseRecord(receipt, userData.getUserId());
            AdvancedSettings.setSupporterState(context, true);
            PurchasingService.notifyFulfillment(receipt.getReceiptId(),
                    FulfillmentResult.FULFILLED);
        } catch (final Throwable e) {
            // If for any reason the app is not able to fulfill the purchase,
            // add your own error handling code here.
            Timber.e("Failed to grant purchase, with error %s", e.getMessage());
        }
    }

    protected void handleReceipt(final Receipt receipt, final UserData userData) {
        ProductType productType = receipt.getProductType();
        if (productType == ProductType.CONSUMABLE) {
            // check consumable sample for how to handle consumable purchases
            return;
        }
        if (productType == ProductType.ENTITLED) {
            handleEntitlementPurchase(receipt, userData);
            return;
        }
        if (productType == ProductType.SUBSCRIPTION) {
            handleSubscriptionPurchase(receipt, userData);
        }
    }

    protected void purchaseFailed() {
        EventBus.getDefault().post(new AmazonIapMessageEvent(R.string.subscription_failed));
    }

    /**
     * Sends a {@link com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapAvailabilityEvent}
     * with info about if a user could purchase a product and if they already purchased it.
     */
    protected void refreshPurchasesAvailability() {
        boolean userDataAvailable = userIapData != null && userIapData.getAmazonUserId() != null;
        EventBus.getDefault().post(new AmazonIapAvailabilityEvent(
                subscriptionAvailable && userDataAvailable,
                passAvailable && userDataAvailable,
                userIapData != null && userIapData.hasActivePurchase()
        ));
    }

    /**
     * Reload the purchase history from the database and update if the user is a supporter.
     */
    protected void reloadPurchaseStatus() {
        loadPurchaseRecords();
        refreshPurchasesAvailability();
    }

    private void loadPurchaseRecords() {
        final List<PurchaseRecord> purchaseRecords = dataSource.getPurchaseRecords(
                userIapData.getAmazonUserId());
        userIapData.setPurchaseRecords(purchaseRecords);
    }

    /**
     * Save purchase details locally.
     */
    private void savePurchaseRecord(final Receipt receipt, final String userId) {
        dataSource
                .insertOrUpdateSubscriptionRecord(receipt.getReceiptId(),
                        userId,
                        receipt.getPurchaseDate().getTime(),
                        receipt.getCancelDate() == null ? PurchaseRecord.VALID_TO_DATE_NOT_SET
                                : receipt.getCancelDate().getTime(),
                        receipt.getSku());
    }

    /**
     * Revokes an entitlement purchase from the customer.
     */
    private void revokeEntitlement(final Receipt receipt, final String userId) {
        String receiptId = receipt.getReceiptId();
        final PurchaseRecord record;
        if (receiptId == null) {
            // The revoked receipt's receipt id may be null on older devices.
            record = dataSource.getLatestEntitlementRecordBySku(userId, receipt.getSku());
            if (record == null) {
                return;
            }
            receiptId = record.getAmazonReceiptId();
        } else {
            record = dataSource.getEntitlementRecordByReceiptId(receiptId);
        }
        if (record == null) {
            // No purchase record for the entitlement before, do nothing.
            return;
        }
        if (record.getValidTo() == PurchaseRecord.VALID_TO_DATE_NOT_SET
                || record.getValidTo() > System.currentTimeMillis()) {
            final long cancelDate = receipt.getCancelDate() != null ? receipt.getCancelDate()
                    .getTime() : System.currentTimeMillis();
            dataSource.cancelEntitlement(receiptId, cancelDate);
        }
    }

    private void revokeSubscription(final Receipt receipt) {
        final String receiptId = receipt.getReceiptId();
        dataSource.cancelSubscription(receiptId, receipt.getCancelDate().getTime());
    }
}
