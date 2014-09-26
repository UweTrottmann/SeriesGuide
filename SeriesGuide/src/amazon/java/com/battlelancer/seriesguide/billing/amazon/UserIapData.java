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

import java.util.List;

/**
 * Holder object for the customer's InAppPurchase data.
 */
public class UserIapData {
    private List<SubscriptionRecord> subscriptionRecords;

    private boolean subsActive;
    private long subsFrom;
    private final String amazonUserId;
    private final String amazonMarketplace;

    /**
     * Replaces the current subscriptions and updates the current subscription status.
     */
    public void setSubscriptionRecords(final List<SubscriptionRecord> subscriptionRecords) {
        this.subscriptionRecords = subscriptionRecords;
        reloadSubscriptionStatus();
    }

    public String getAmazonUserId() {
        return amazonUserId;
    }

    public String getAmazonMarketplace() {
        return amazonMarketplace;
    }

    public boolean isSubsActiveCurrently() {
        return subsActive;
    }

    public long getCurrentSubsFrom() {
        return subsFrom;
    }

    public UserIapData(final String amazonUserId, final String amazonMarketplace) {
        this.amazonUserId = amazonUserId;
        this.amazonMarketplace = amazonMarketplace;
    }

    private void reloadSubscriptionStatus() {
        this.subsActive = false;
        this.subsFrom = 0;
        for (final SubscriptionRecord record : subscriptionRecords) {
            if (record.isActiveNow()) {
                this.subsActive = true;
                this.subsFrom = record.getFrom();
                return;
            }
        }
    }
}
