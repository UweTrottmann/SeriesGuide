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

import com.amazon.device.iap.PurchasingListener;
import com.amazon.device.iap.PurchasingService;
import com.amazon.device.iap.model.ProductDataResponse;
import com.amazon.device.iap.model.PurchaseResponse;
import com.amazon.device.iap.model.PurchaseUpdatesResponse;
import com.amazon.device.iap.model.Receipt;
import com.amazon.device.iap.model.UserDataResponse;
import java.util.Set;
import timber.log.Timber;

public class AmazonPurchasingListener implements PurchasingListener {

    private final AmazonIapManager iapManager;

    public AmazonPurchasingListener(final AmazonIapManager iapManager) {
        this.iapManager = iapManager;
    }

    /**
     * Callback for {@link PurchasingService#getUserData}. If successful, gets the current user from
     * {@link UserDataResponse} and saves with {@link com.battlelancer.seriesguide.billing.amazon.AmazonIapManager#setAmazonUserId}.
     */
    @Override
    public void onUserDataResponse(final UserDataResponse response) {
        Timber.d("onGetUserDataResponse: requestId (" + response.getRequestId()
                + ") userIdRequestStatus: "
                + response.getRequestStatus()
                + ")");

        final UserDataResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
            case SUCCESSFUL: {
                Timber.d("onUserDataResponse: get user id (" + response.getUserData().getUserId()
                        + ", marketplace ("
                        + response.getUserData().getMarketplace()
                        + ") ");
                iapManager.setAmazonUserId(response.getUserData().getUserId(),
                        response.getUserData().getMarketplace());
                break;
            }
            case FAILED:
            case NOT_SUPPORTED: {
                Timber.d("onUserDataResponse failed, status code is " + status);
                iapManager.setAmazonUserId(null, null);
                break;
            }
        }
    }

    /**
     * Callback for {@link PurchasingService#getProductData}. Enables or disables purchases
     * according to availability.
     */
    @Override
    public void onProductDataResponse(final ProductDataResponse response) {
        final ProductDataResponse.RequestStatus status = response.getRequestStatus();
        Timber.d("onProductDataResponse: RequestStatus (" + status + ")");

        switch (status) {
            case SUCCESSFUL:
                Timber.d(
                        "onProductDataResponse: successful.  The item data map in this response includes the valid SKUs");
                final Set<String> unavailableSkus = response.getUnavailableSkus();
                Timber.d("onProductDataResponse: " + unavailableSkus.size() + " unavailable skus");
                iapManager.enablePurchaseForSkus(response.getProductData());
                iapManager.disablePurchaseForSkus(response.getUnavailableSkus());
                iapManager.refreshPurchasesAvailability();

                break;
            case FAILED:
            case NOT_SUPPORTED:
                Timber.d("onProductDataResponse: failed, should retry request");
                iapManager.disableAllPurchases();
                break;
        }
    }

    /**
     * Callback for {@link PurchasingService#getPurchaseUpdates}.
     *
     * You will receive receipts for all possible purchase history from this callback.
     */
    @Override
    public void onPurchaseUpdatesResponse(final PurchaseUpdatesResponse response) {
        Timber.d("onPurchaseUpdatesResponse: requestId (" + response.getRequestId()
                + ") purchaseUpdatesResponseStatus ("
                + response.getRequestStatus()
                + ") userId ("
                + response.getUserData().getUserId()
                + ")");
        final PurchaseUpdatesResponse.RequestStatus status = response.getRequestStatus();
        switch (status) {
            case SUCCESSFUL:
                iapManager.setAmazonUserId(response.getUserData().getUserId(),
                        response.getUserData().getMarketplace());
                for (final Receipt receipt : response.getReceipts()) {
                    iapManager.handleReceipt(receipt, response.getUserData());
                }
                if (response.hasMore()) {
                    PurchasingService.getPurchaseUpdates(false);
                }
                iapManager.reloadPurchaseStatus();
                break;
            case FAILED:
            case NOT_SUPPORTED:
                Timber.d("onProductDataResponse: failed, should retry request");
                iapManager.disableAllPurchases();
                break;
        }
    }

    /**
     * This is the callback for {@link PurchasingService#purchase}. For each time the application
     * sends a purchase request {@link PurchasingService#purchase}, Amazon Appstore will call this
     * callback when the purchase request is completed. If the RequestStatus is Successful or
     * AlreadyPurchased then application needs to call {@link com.battlelancer.seriesguide.billing.amazon.AmazonIapManager#handleReceipt}
     * to handle the purchase fulfillment. If the RequestStatus is INVALID_SKU, NOT_SUPPORTED, or
     * FAILED, notify corresponding method of {@link com.battlelancer.seriesguide.billing.amazon.AmazonIapManager}
     * .
     */
    @Override
    public void onPurchaseResponse(final PurchaseResponse response) {
        final String requestId = response.getRequestId().toString();
        final String userId = response.getUserData().getUserId();
        final PurchaseResponse.RequestStatus status = response.getRequestStatus();
        Timber.d("onPurchaseResponse: requestId (" + requestId
                + ") userId ("
                + userId
                + ") purchaseRequestStatus ("
                + status
                + ")");

        switch (status) {
            case SUCCESSFUL:
                final Receipt receipt = response.getReceipt();
                Timber.d("onPurchaseResponse: receipt json:" + receipt.toJSON());
                iapManager.handleReceipt(receipt, response.getUserData());
                iapManager.reloadPurchaseStatus();
                break;
            case ALREADY_PURCHASED:
                Timber.i("onPurchaseResponse: already purchased, "
                        + "verify subscription purchase again");
                iapManager.reloadPurchaseStatus();
                break;
            case INVALID_SKU:
                Timber.d("onPurchaseResponse: invalid SKU!"
                        + " onProductDataResponse should have disabled buy button already.");
                iapManager.purchaseFailed();
                break;
            case FAILED:
            case NOT_SUPPORTED:
                Timber.d("onPurchaseResponse: failed, remove purchase request from local storage");
                iapManager.purchaseFailed();
                break;
        }
    }
}
