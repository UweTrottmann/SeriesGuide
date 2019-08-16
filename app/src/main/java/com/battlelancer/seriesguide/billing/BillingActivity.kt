package com.battlelancer.seriesguide.billing

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.view.isGone
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.ui.BaseActivity
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.ui.ShowsActivity
import com.battlelancer.seriesguide.util.Utils
import com.uwetrottmann.seriesguide.billing.BillingViewModel
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails

class BillingActivity : BaseActivity() {

    private lateinit var progressScreen: View
    private lateinit var contentContainer: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkuDetailsAdapter
    private lateinit var buttonManageSubs: Button
    private lateinit var buttonPass: Button
    private lateinit var textViewHasUpgrade: View
    private lateinit var textViewBillingError: TextView

    private lateinit var billingViewModel: BillingViewModel
    private lateinit var manageSubscriptionUrl: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing)
        setupActionBar()

        manageSubscriptionUrl = PLAY_MANAGE_SUBS_ALL

        setupViews()

        // Always get subscription SKU info.
        // Users might want to support even if unlock app is installed.
        billingViewModel = ViewModelProviders.of(this)
            .get(BillingViewModel::class.java).also {
                it.subsSkuDetailsListLiveData.observe(this, Observer { skuDetails ->
                    adapter.setSkuDetailsList(skuDetails)
                })
            }
        billingViewModel.errorEvent.observe(this, Observer { message ->
            message?.let {
                textViewBillingError.apply {
                    text = "${getString(R.string.subscription_unavailable)} ($message)"
                    isGone = false
                }
            }
        })
        // Only use subscription state if unlock app is not installed.
        if (Utils.hasXpass(this)) {
            setWaitMode(false)
            updateViewStates(true)
        } else {
            setWaitMode(true)
            billingViewModel.goldStatusLiveData.observe(this, Observer { goldStatus ->
                setWaitMode(false)
                updateViewStates(goldStatus != null && goldStatus.entitled)
                manageSubscriptionUrl =
                    if (goldStatus?.isSub == true && goldStatus.sku != null) {
                        PLAY_MANAGE_SUBS_ONE + goldStatus.sku
                    } else {
                        PLAY_MANAGE_SUBS_ALL
                    }
            })
        }
    }

    override fun setupActionBar() {
        super.setupActionBar()
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewBilling)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = object : SkuDetailsAdapter() {
            override fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
                billingViewModel.makePurchase(this@BillingActivity, item)
            }
        }
        recyclerView.adapter = adapter

        textViewHasUpgrade = findViewById(R.id.textViewBillingExisting)
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
        buttonPass.setOnClickListener {
            Utils.launchWebsite(
                this@BillingActivity, getString(R.string.url_x_pass)
            )
        }

        progressScreen = findViewById(R.id.progressBarBilling)
        contentContainer = findViewById(R.id.containerBilling)

        findViewById<View>(R.id.textViewBillingMoreInfo).setOnClickListener {
            Utils.launchWebsite(
                this@BillingActivity, getString(R.string.url_whypay)
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                super.onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStart() {
        super.onStart()

        // Check if user has installed key app.
        if (Utils.hasXpass(this)) {
            updateViewStates(true)
        }
    }

    private fun updateViewStates(hasUpgrade: Boolean) {
        // Only enable key app button if the user does not have all access yet.
        buttonPass.isEnabled = !hasUpgrade
        textViewHasUpgrade.isGone = !hasUpgrade
    }

    /**
     * Disables the purchase button and hides the subscribed message.
     */
    private fun enableFallBackMode() {
        buttonPass.isEnabled = true
        textViewHasUpgrade.visibility = View.GONE
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
         * Displays a notification that the subscription has expired. Its action opens this activity.
         */
        @JvmStatic
        fun showExpiredNotification(context: Context) {
            val nb = NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_ERRORS)
            NotificationSettings.setDefaultsForChannelErrors(context, nb)

            // set required attributes
            nb.setSmallIcon(R.drawable.ic_notification)
            nb.setContentTitle(context.getString(R.string.subscription_expired))
            nb.setContentText(context.getString(R.string.subscription_expired_details))
            nb.setTicker(context.getString(R.string.subscription_expired_details))
            nb.setAutoCancel(true)

            // build task stack
            val notificationIntent = Intent(context, BillingActivity::class.java)
            val contentIntent = TaskStackBuilder
                .create(context)
                .addNextIntent(Intent(context, ShowsActivity::class.java))
                .addNextIntent(Intent(context, SeriesGuidePreferences::class.java))
                .addNextIntent(notificationIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
            nb.setContentIntent(contentIntent)

            // build the notification
            val notification = nb.build()

            // show the notification
            val nm = context
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
            nm?.notify(SgApp.NOTIFICATION_SUBSCRIPTION_ID, notification)
        }
    }
}
