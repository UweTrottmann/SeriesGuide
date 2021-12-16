package com.battlelancer.seriesguide.billing.amazon

import android.app.Activity

interface AmazonIapManagerInterface {

    /**
     * Sets up Amazon IAP.
     *
     * Ensure to call this in `onCreate` of any activity before making calls to any other methods.
     */
    fun register()

    fun requestProductData()

    fun requestUserDataAndPurchaseUpdates()

    fun activate()

    fun deactivate()

    fun validateSupporterState(activity: Activity)

}