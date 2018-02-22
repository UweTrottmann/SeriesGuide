package com.battlelancer.seriesguide.billing.amazon;

/**
 * Holds the in-app purchase details, may be an entitlement or a subscription.
 *
 * <p>If this is a subscription (check via the SKU), the valid from date is the beginning and the
 * valid to date the end date of the subscription.
 *
 * <p>If this is an entitlement (check via the SKU), the valid from date is the purchase date and
 * the to valid to date is the optional cancel date of the purchase.
 */
public class PurchaseRecord {
    public static int VALID_TO_DATE_NOT_SET = -1;
    private String amazonReceiptId;
    private long validFrom;
    private long validTo = VALID_TO_DATE_NOT_SET;
    private String amazonUserId;
    private String sku;

    public long getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(final long validFrom) {
        this.validFrom = validFrom;
    }

    public long getValidTo() {
        return validTo;
    }

    public void setValidTo(final long subscriptionTo) {
        this.validTo = subscriptionTo;
    }

    public boolean isActiveNow() {
        return VALID_TO_DATE_NOT_SET == validTo;
    }

    public boolean isActiveForDate(final long date) {
        return date >= validFrom && (isActiveNow() || date <= validTo);
    }

    public String getAmazonReceiptId() {
        return amazonReceiptId;
    }

    public void setAmazonReceiptId(final String receiptId) {
        this.amazonReceiptId = receiptId;
    }

    public String getAmazonUserId() {
        return amazonUserId;
    }

    public void setAmazonUserId(final String amazonUserId) {
        this.amazonUserId = amazonUserId;
    }

    public void setSku(final String sku) {
        this.sku = sku;
    }

    public String getSku() {
        return this.sku;
    }
}