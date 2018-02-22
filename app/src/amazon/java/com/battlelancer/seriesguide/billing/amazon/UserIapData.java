package com.battlelancer.seriesguide.billing.amazon;

import java.util.List;

/**
 * Holder object for the customer's InAppPurchase data.
 */
public class UserIapData {
    private List<PurchaseRecord> purchaseRecords;

    private boolean onePurchaseActive;
    private long currentPurchaseValidFrom;
    private final String amazonUserId;
    private final String amazonMarketplace;

    /**
     * Replaces the current purchases and updates the current supporter status.
     */
    public void setPurchaseRecords(final List<PurchaseRecord> purchaseRecords) {
        this.purchaseRecords = purchaseRecords;
        reloadSupporterStatus();
    }

    public String getAmazonUserId() {
        return amazonUserId;
    }

    /**
     * Get the Amazon market place the current user is signed into.
     */
    public String getAmazonMarketplace() {
        return amazonMarketplace;
    }

    /**
     * If there is an active purchase, returns the date that purchase is valid from.
     */
    public long getActivePurchaseValidFrom() {
        return currentPurchaseValidFrom;
    }

    /**
     * Returns if the user has a valid purchase (either active subscription or entitlement) and
     * should get access to all features of the app.
     */
    public boolean hasActivePurchase() {
        return onePurchaseActive;
    }

    public UserIapData(final String amazonUserId, final String amazonMarketplace) {
        this.amazonUserId = amazonUserId;
        this.amazonMarketplace = amazonMarketplace;
    }

    private void reloadSupporterStatus() {
        this.onePurchaseActive = false;
        for (final PurchaseRecord record : purchaseRecords) {
            if (record.isActiveNow()) {
                onePurchaseActive = true;
                currentPurchaseValidFrom = record.getValidFrom();
                return;
            }
        }
    }
}
