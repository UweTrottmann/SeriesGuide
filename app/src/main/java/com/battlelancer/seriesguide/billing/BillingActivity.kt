package com.battlelancer.seriesguide.billing

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isGone
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.google.android.material.snackbar.Snackbar
import com.uwetrottmann.seriesguide.billing.BillingViewModel
import com.uwetrottmann.seriesguide.billing.BillingViewModelFactory
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails

class BillingActivity : BaseActivity() {

    private lateinit var progressScreen: View
    private lateinit var contentContainer: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkuDetailsAdapter
    private lateinit var buttonManageSubs: Button
    private lateinit var buttonPass: Button
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
            ViewModelProvider(this, BillingViewModelFactory(application, SgApp.coroutineScope))
                .get(BillingViewModel::class.java).also {
                    it.subsSkuDetailsListLiveData.observe(this) { skuDetails ->
                        adapter.setSkuDetailsList(skuDetails)
                    }
                }
        billingViewModel.errorEvent.observe(this) { message ->
            message?.let {
                textViewBillingError.apply {
                    text = "${getString(R.string.subscription_unavailable)} ($message)"
                    isGone = false
                }
            }
        }
        // Only use subscription state if unlock app is not installed.
        if (Utils.hasXpass(this)) {
            setWaitMode(false)
            updateViewStates(hasUpgrade = true, unlockAppDetected = true)
        } else {
            setWaitMode(true)
            billingViewModel.goldStatusLiveData.observe(this) { goldStatus ->
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
            override fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
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
                Utils.launchWebsite(
                    v.context,
                    manageSubscriptionUrl
                )
            }
        }

        buttonPass = findViewById(R.id.buttonBillingGetPass)
        ViewTools.openUriOnClick(buttonPass, getString(R.string.url_x_pass))

        findViewById<View>(R.id.textViewBillingMoreInfo).setOnClickListener {
            Utils.launchWebsite(
                this@BillingActivity, getString(R.string.url_whypay)
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if user has installed key app.
        if (Utils.hasXpass(this)) {
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

        /**
         * Displays a [Snackbar] that the subscription has expired. Its action opens this activity.
         */
        @JvmStatic
        fun showExpiredNotification(activity: Activity, parentView: View) {
            val snackbar = Snackbar.make(
                parentView,
                R.string.subscription_expired_details,
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.setAction(R.string.billing_action_manage_subscriptions) {
                activity.startActivity(Intent(activity, BillingActivity::class.java))
            }
            snackbar.show()
        }
    }
}
