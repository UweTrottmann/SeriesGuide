package com.battlelancer.seriesguide.billing.amazon

import android.app.Activity
import android.content.Context

/**
 * No-op dummy of Amazon IAP manager.
 */
class AmazonIapManager(@Suppress("UNUSED_PARAMETER") context: Context) : AmazonIapManagerInterface {

    override fun register() {
        // no op
    }

    override fun requestProductData() {
        // no op
    }

    override fun requestUserDataAndPurchaseUpdates() {
        // no op
    }

    override fun activate() {
        // no op
    }

    override fun deactivate() {
        // no op
    }

    override fun validateSupporterState(activity: Activity) {
        // no op
    }

}
