package com.battlelancer.seriesguide.billing.amazon;

import android.app.Activity;
import android.content.Context;

/**
 * No-op dummy of Amazon IAP manager.
 */
public class AmazonIapManager {

    public static void setup(Context context) {
        // no op
    }

    public static AmazonIapManager get() {
        return new AmazonIapManager();
    }

    public void requestUserDataAndPurchaseUpdates() {
        // no op
    }

    public void activate() {
        // no op
    }

    public void deactivate() {
        // no op
    }

    public void validateSubscription(Activity activity) {
        // no op
    }
}
