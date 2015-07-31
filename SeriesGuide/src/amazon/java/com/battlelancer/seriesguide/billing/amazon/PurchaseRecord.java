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

/**
 * Holds the InAppPurchase Subscription details.
 */
public class PurchaseRecord {
    public static int TO_DATE_NOT_SET = -1;
    private String amazonReceiptId;
    private long from;
    private long to = TO_DATE_NOT_SET;
    private String amazonUserId;
    private String sku;

    public long getFrom() {
        return from;
    }

    public void setFrom(final long subscriptionFrom) {
        this.from = subscriptionFrom;
    }

    public long getTo() {
        return to;
    }

    public void setTo(final long subscriptionTo) {
        this.to = subscriptionTo;
    }

    public boolean isActiveNow() {
        return TO_DATE_NOT_SET == to;
    }

    public boolean isActiveForDate(final long date) {
        return date >= from && (isActiveNow() || date <= to);
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