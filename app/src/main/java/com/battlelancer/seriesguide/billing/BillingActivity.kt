// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.WebTools
import com.uwetrottmann.seriesguide.billing.BillingViewModel
import com.uwetrottmann.seriesguide.billing.BillingViewModelFactory
import com.uwetrottmann.seriesguide.billing.SafeAugmentedProductDetails
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BillingActivity : BaseActivity() {

    private lateinit var progressScreen: View
    private lateinit var contentContainer: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkuDetailsAdapter
    private lateinit var buttonManageSubs: Button
    private lateinit var buttonOtherWaysToSupport: Button
    private lateinit var textViewHasUpgrade: View
    private lateinit var textViewBillingUnlockDetected: View
    private lateinit var textViewBillingError: TextView

    private lateinit var billingViewModel: BillingViewModel
    private lateinit var manageSubscriptionUrl: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing)
        ThemeUtils.configureForEdgeToEdge(findViewById(R.id.rootLayoutBilling))
        setupActionBar()

        manageSubscriptionUrl = PLAY_MANAGE_SUBS_ALL

        setupViews()

        // Always get subscription SKU info.
        // Users might want to support even if unlock app is installed.
        billingViewModel =
            ViewModelProvider(
                this,
                BillingViewModelFactory(application, SgApp.coroutineScope)
            )[BillingViewModel::class.java].also { model ->
                lifecycleScope.launch {
                    // Only update while views are shown.
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        model.availableProducts.collectLatest {
                            adapter.setProductDetailsList(it)
                        }
                    }
                }
            }
        billingViewModel.errorEvent.observe(this) { error ->
            error?.debugMessage?.let {
                textViewBillingError.apply {
                    text = "${getString(R.string.subscription_unavailable)} ($it)"
                    isVisible = true
                }
            }
            // Only display the other ways to support button if Play Billing is not available
            if (error?.isUnavailable == true) {
                buttonOtherWaysToSupport.isVisible = true
            }
        }
        // Only use subscription state if unlock app is not installed.
        if (BillingTools.hasUnlockKey(this)) {
            setWaitMode(false)
            updateViewStates(hasUpgrade = true, unlockAppDetected = true)
        } else {
            setWaitMode(true)
            billingViewModel.subStatusLiveData.observe(this) { goldStatus ->
                setWaitMode(false)
                updateViewStates(goldStatus != null && goldStatus.entitled, false)
                manageSubscriptionUrl =
                    if (goldStatus?.isSub == true && goldStatus.sku != null) {
                        PLAY_MANAGE_SUBS_ONE + goldStatus.sku
                    } else {
                        PLAY_MANAGE_SUBS_ALL
                    }
            }
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        supportActionBar?.apply {
            setHomeAsUpIndicator(R.drawable.ic_clear_24dp)
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupViews() {
        progressScreen = findViewById(R.id.progressBarBilling)
        contentContainer = findViewById(R.id.containerBilling)
        ThemeUtils.applyBottomPaddingForNavigationBar(contentContainer)

        recyclerView = findViewById(R.id.recyclerViewBilling)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = object : SkuDetailsAdapter() {
            override fun onSkuDetailsClicked(item: SafeAugmentedProductDetails) {
                billingViewModel.makePurchase(this@BillingActivity, item)
            }
        }
        recyclerView.adapter = adapter

        textViewHasUpgrade = findViewById(R.id.textViewBillingExisting)
        textViewBillingUnlockDetected = findViewById(R.id.textViewBillingUnlockDetected)
        textViewBillingError = findViewById<TextView>(R.id.textViewBillingError).apply {
            isGone = true
        }
        buttonManageSubs = findViewById<Button>(R.id.buttonBillingManageSubscription).also {
            it.setOnClickListener { v ->
                // URL may change depending on active sub, so get it on demand.
                WebTools.openInApp(v.context, manageSubscriptionUrl)
            }
        }

        buttonOtherWaysToSupport = findViewById<Button>(R.id.buttonBillingMoreOptions).also {
            it.setOnClickListener {
                WebTools.openInApp(this, getString(R.string.url_support_the_dev))
            }
            it.isGone = true
        }
        findViewById<View>(R.id.buttonBillingMoreInfo).setOnClickListener {
            WebTools.openInCustomTab(this, getString(R.string.url_billing_info_and_help))
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if user has installed key app.
        if (BillingTools.hasUnlockKey(this)) {
            updateViewStates(hasUpgrade = true, unlockAppDetected = true)
        }
    }

    private fun updateViewStates(hasUpgrade: Boolean, unlockAppDetected: Boolean) {
        textViewHasUpgrade.isGone = !hasUpgrade
        textViewBillingUnlockDetected.isGone = !unlockAppDetected
    }

    private fun setWaitMode(isActive: Boolean) {
        progressScreen.isGone = !isActive
        contentContainer.isGone = isActive
    }

    companion object {

        private const val PLAY_MANAGE_SUBS_ALL =
            "https://play.google.com/store/account/subscriptions"
        private const val PLAY_MANAGE_SUBS_ONE =
            "$PLAY_MANAGE_SUBS_ALL?package=${BuildConfig.APPLICATION_ID}&sku="

        fun intent(context: Context) = Intent(context, BillingActivity::class.java)
    }
}
