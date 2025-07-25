// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing.amazon

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.amazon.device.iap.PurchasingService
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapAvailabilityEvent
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapMessageEvent
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager.AmazonIapProductEvent
import com.battlelancer.seriesguide.databinding.ActivityAmazonBillingBinding
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Offers a single subscription using the Amazon in-app purchasing library.
 *
 * To debug: install on device with Amazon App Store and download App Tester app, put
 * `amazon.sdktester.json` onto `sdcard`.
 * Run command `adb shell setprop debug.amazon.sandboxmode debug` and launch app tester, then app.
 * Debug version of app works.
 *
 * To disable sandbox mode run `adb shell setprop debug.amazon.sandboxmode none`.
 *
 * 2022-05-19: subscription not recognized because sku in receipt is null, entitlement works.
 * Confirmed that subscription works and is recognized with production version.
 *
 * https://developer.amazon.com/de/docs/in-app-purchasing/iap-app-tester-user-guide.html
 */
class AmazonBillingActivity : BaseActivity() {

    private lateinit var binding: ActivityAmazonBillingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAmazonBillingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeUtils.configureForEdgeToEdge(binding.root)
        setupActionBar()
        setupViews()
        AmazonHelper.create(this)
        AmazonHelper.iapManager.register()
    }

    override fun setupActionBar() {
        super.setupActionBar()
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        ThemeUtils.applyBottomPaddingForNavigationBar(binding.scrollViewAmazonBilling)

        binding.buttonAmazonBillingSubscribe.isEnabled = false
        binding.buttonAmazonBillingSubscribe.setOnClickListener { subscribe() }

        ViewTools.openUriOnClick(
            binding.textViewAmazonBillingMoreInfo,
            getString(R.string.url_billing_info_and_help)
        )
        binding.progressBarAmazonBilling.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()

        // no need to get product data every time we were hidden, so do it in onStart
        AmazonHelper.iapManager.requestProductData()
    }

    override fun onResume() {
        super.onResume()
        AmazonHelper.iapManager.activate()
        AmazonHelper.iapManager.requestUserDataAndPurchaseUpdates()
    }

    override fun onPause() {
        super.onPause()
        AmazonHelper.iapManager.deactivate()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return false
    }

    private fun subscribe() {
        val requestId = PurchasingService.purchase(
            AmazonSku.SERIESGUIDE_SUB_YEARLY.sku
        )
        Timber.d("subscribe: requestId (%s)", requestId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: AmazonIapMessageEvent) {
        Toast.makeText(this, event.messageResId, Toast.LENGTH_LONG).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: AmazonIapAvailabilityEvent) {
        binding.progressBarAmazonBilling.visibility = View.GONE

        // enable or disable purchase buttons based on what can be purchased
        binding.buttonAmazonBillingSubscribe.isEnabled =
            event.subscriptionAvailable && !event.userHasActivePurchase

        // status text
        if (!event.subscriptionAvailable) {
            // neither purchase available, probably not signed in
            binding.textViewAmazonBillingExisting.setText(R.string.subscription_not_signed_in)
        } else {
            // subscription available
            binding.textViewAmazonBillingExisting.text =
                if (event.userHasActivePurchase) getString(R.string.upgrade_success) else null
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: AmazonIapProductEvent) {
        val product = event.product
        // display the actual price like "1.23 C"
        var price = product.price
        if (price == null) {
            price = "--"
        }
        if (AmazonSku.SERIESGUIDE_SUB_YEARLY.sku == product.sku) {
            val priceString = getString(R.string.billing_duration_format, price)
            val trialInfo = getString(R.string.billing_sub_description)
            val finalPriceString = "$priceString\n$trialInfo"
            binding.textViewAmazonBillingSubPrice.text = finalPriceString
        }
    }
}