/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.billing.amazon;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.FulfillmentResult;
import com.amazon.device.iap.model.Product;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserData;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import de.greenrobot.event.EventBus;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import timber.log.Timber;

public class AmazonIapManager {

    public static class AmazonIapMessageEvent {
        public final int messageResId;

        public AmazonIapMessageEvent(int messageResId) {
            this.messageResId = messageResId;
        }
    }

    public static class AmazonIapPriceEvent {
        public final Product product;

        public AmazonIapPriceEvent(Product product) {
            this.product = product;
        }
    }

    public static class AmazonIapAvailabilityEvent {
        public final boolean productAvailable;
        public final boolean userCanSubscribe;

        public AmazonIapAvailabilityEvent(boolean productAvailable, boolean userCanSubscribe) {
            this.productAvailable = productAvailable;
            this.userCanSubscribe = userCanSubscribe;
        }
    }

    private static AmazonIapManager amazonIapManager;

    /**
     * Sets up Amazon IAP.
     *
     * <p> Ensure to call this in {@code onCreate} of any activity before making calls to the
     * instance retrieved by {@link #get()}.
     */
    public static synchronized void setup(Context context) {
        if (amazonIapManager != null) {
            return;
        }
        AmazonIapManager iapManager = new AmazonIapManager(context);
        PurchasingService.registerListener(context.getApplicationContext(),
                new AmazonPurchasingListener(iapManager));
        Timber.i("IAP sandbox mode is: " + PurchasingService.IS_SANDBOX_MODE);
        amazonIapManager = iapManager;
    }

    /**
     * Ensure you have called {@link #setup} once in the main activity of the app or this will
     * return {@code null}.
     */
    public static AmazonIapManager get() {
        return amazonIapManager;
    }

    private final SubscriptionDataSource dataSource;

    private boolean userDataAvailable;
    private boolean subAvailable;
    private UserIapData userIapData;

    public AmazonIapManager(Context context) {
        this.dataSource = new SubscriptionDataSource(context);
        this.userDataAvailable = false;
        this.subAvailable = false;
        this.userIapData = null;
    }

    /**
     * Call this in e.g. {@code onStart} to request data about all available products.
     *
     * <p> If successful, a {@link com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapPriceEvent}
     * will be posted.
     */
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
    public void activate() {
        dataSource.open();
    }

    /**
     * Gracefully close the database in the activity's {@code onPause}.
     */
    public void deactivate() {
        dataSource.close();
    }

    /**
     * Checks if the current user has an active subscription. If the user was subscribed, but now is
     * not {@link com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity} is launched.
     *
     * <p> If user or sub data could not be fetched, keeps the last subscription state.
     */
    public void validateSubscription(Activity activity) {
        Timber.d("validateSubscription");
        boolean isSubscribed;
        if (userIapData != null) {
            loadSubscriptionRecords();

            isSubscribed = userIapData.isSubsActiveCurrently();
        } else if (userDataAvailable) {
            // user is not logged in
            isSubscribed = false;
        } else {
            // data could not be fetched, keep last sub state
            return;
        }

        // update state
        boolean isSubscribedOld = AdvancedSettings.getLastSubscriptionState(activity);
        AdvancedSettings.setSubscriptionState(activity, isSubscribed);

        // notify if subscription has expired
        if (isSubscribedOld && !isSubscribed) {
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
                refreshSubsAvailability();
            }
        } else if (userIapData == null || !newAmazonUserId.equals(userIapData.getAmazonUserId())) {
            // If there was no existing Amazon user then either no customer was
            // previously registered or the application has just started.

            // If the user id does not match then another Amazon user has registered.
            userIapData = new UserIapData(newAmazonUserId, newAmazonMarketplace);
            refreshSubsAvailability();
        }
    }

    protected void enablePurchaseForSkus(final Map<String, Product> productData) {
        // currently only one sku (try-then-buy subscription)
        Product product = productData.get(AmazonSku.SERIESGUIDE_SUB.getSku());
        if (product != null) {
            subAvailable = true;
            EventBus.getDefault().post(new AmazonIapPriceEvent(product));
        }
    }

    protected void disablePurchaseForSkus(final Set<String> unavailableSkus) {
        if (unavailableSkus.contains(AmazonSku.SERIESGUIDE_SUB.toString())) {
            subAvailable = false;
            // reasons for product not available can be:
            // * Item not available for this country
            // * Item pulled off from Appstore by developer
            // * Item pulled off from Appstore by Amazon
            EventBus.getDefault()
                    .post(new AmazonIapMessageEvent(R.string.subscription_unavailable));
        }
    }

    protected void disableAllPurchases() {
        this.subAvailable = false;
        refreshSubsAvailability();
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

                grantSubscriptionPurchase(receipt, userData);
            }
        } catch (Throwable e) {
            EventBus.getDefault().post(new AmazonIapMessageEvent(R.string.subscription_failed));
        }
    }

    private void grantSubscriptionPurchase(final Receipt receipt, final UserData userData) {
        final AmazonSku amazonSku = AmazonSku.fromSku(receipt.getSku());
        // Verify that the SKU is still applicable.
        if (amazonSku != AmazonSku.SERIESGUIDE_SUB) {
            Timber.w("The SKU [" + receipt.getSku() + "] in the receipt is not valid anymore ");
            // if the sku is not applicable anymore, call
            // PurchasingService.notifyFulfillment with status "UNAVAILABLE"
            PurchasingService.notifyFulfillment(receipt.getReceiptId(),
                    FulfillmentResult.UNAVAILABLE);
            return;
        }
        try {
            // Set the purchase status to fulfilled for your application
            saveSubscriptionRecord(receipt, userData.getUserId());
            PurchasingService.notifyFulfillment(receipt.getReceiptId(),
                    FulfillmentResult.FULFILLED);
        } catch (final Throwable e) {
            // If for any reason the app is not able to fulfill the purchase,
            // add your own error handling code here.
            Timber.e("Failed to grant entitlement purchase, with error " + e.getMessage());
        }
    }

    protected void handleReceipt(final Receipt receipt, final UserData userData) {
        switch (receipt.getProductType()) {
            case CONSUMABLE:
                // check consumable sample for how to handle consumable purchases
                break;
            case ENTITLED:
                // check entitlement sample for how to handle consumable purchases
                break;
            case SUBSCRIPTION:
                handleSubscriptionPurchase(receipt, userData);
                break;
        }
    }

    protected void purchaseFailed() {
        EventBus.getDefault().post(new AmazonIapMessageEvent(R.string.subscription_failed));
    }

    protected void refreshSubsAvailability() {
        final boolean available = subAvailable && userIapData != null
                && userIapData.getAmazonUserId() != null;
        EventBus.getDefault().post(new AmazonIapAvailabilityEvent(available,
                userIapData != null && !userIapData.isSubsActiveCurrently()));
    }

    /**
     * Reload the subscription history from database
     */
    protected void reloadSubscriptionStatus() {
        loadSubscriptionRecords();
        refreshSubsAvailability();
    }

    private void loadSubscriptionRecords() {
        final List<SubscriptionRecord> subsRecords = dataSource.getSubscriptionRecords(
                userIapData.getAmazonUserId());
        userIapData.setSubscriptionRecords(subsRecords);
    }

    /**
     * Save subscription purchase detail locally.
     */
    private void saveSubscriptionRecord(final Receipt receipt, final String userId) {
        dataSource
                .insertOrUpdateSubscriptionRecord(receipt.getReceiptId(),
                        userId,
                        receipt.getPurchaseDate().getTime(),
                        receipt.getCancelDate() == null ? SubscriptionRecord.TO_DATE_NOT_SET
                                : receipt.getCancelDate().getTime(),
                        receipt.getSku());
    }

    private void revokeSubscription(final Receipt receipt) {
        final String receiptId = receipt.getReceiptId();
        dataSource.cancelSubscription(receiptId, receipt.getCancelDate().getTime());
    }
}
