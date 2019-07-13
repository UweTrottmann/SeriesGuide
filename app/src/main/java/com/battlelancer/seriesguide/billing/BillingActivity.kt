package com.battlelancer.seriesguide.billing

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
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
import timber.log.Timber

class BillingActivity : BaseActivity() {

    private lateinit var progressScreen: View
    private lateinit var contentContainer: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SkuDetailsAdapter
    private lateinit var buttonManageSubs: Button
    private lateinit var buttonPass: Button
    private lateinit var textViewHasUpgrade: View

    private var billingViewModel: BillingViewModel? = null
    private lateinit var manageSubscriptionUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_billing)
        setupActionBar()

        manageSubscriptionUrl = PLAY_MANAGE_SUBS_ALL

        setupViews()

        if (Utils.hasXpass(this)) {
            setWaitMode(false)
            updateViewStates(true)
        } else {
            setWaitMode(true)
            billingViewModel = ViewModelProviders.of(this)
                .get(BillingViewModel::class.java).also {
                    it.goldStatusLiveData.observe(this, Observer { goldStatus ->
                        setWaitMode(false)
                        updateViewStates(goldStatus != null && goldStatus.entitled)
                        manageSubscriptionUrl =
                            if (goldStatus?.isSub == true && goldStatus.sku != null) {
                                PLAY_MANAGE_SUBS_ONE + goldStatus.sku
                            } else {
                                PLAY_MANAGE_SUBS_ALL
                            }
                    })
                    it.subsSkuDetailsListLiveData.observe(this, Observer { skuDetails ->
                        adapter.setSkuDetailsList(skuDetails)
                    })
                }
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
                billingViewModel?.makePurchase(this@BillingActivity, item)
            }
        }
        recyclerView.adapter = adapter

        textViewHasUpgrade = findViewById(R.id.textViewBillingExisting)
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

    private fun logAndShowAlertDialog(errorResId: Int, message: String) {
        Timber.e(message)

        AlertDialog.Builder(this)
            .setMessage(getString(errorResId) + "\n\n" + message)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .show()
    }

    companion object {

        private const val SOME_STRING = "SURPTk9UQ0FSRUlGWU9VUElSQVRFVEhJUw=="
        private const val PLAY_MANAGE_SUBS_ALL =
            "https://play.google.com/store/account/subscriptions"
        private const val PLAY_MANAGE_SUBS_ONE =
            "$PLAY_MANAGE_SUBS_ALL?package=${BuildConfig.APPLICATION_ID}&sku="

        //    @Nullable
        //    public static String latestSubscriptionSkuOrNull(@NonNull Inventory inventory) {
        //        if (inventory.getPurchase(SKU_X_SUB_2017_08) != null) {
        //            return SKU_X_SUB_2017_08;
        //        }
        //        if (inventory.getPurchase(SKU_X_SUB_2016_05) != null) {
        //            return SKU_X_SUB_2016_05;
        //        }
        //        if (inventory.getPurchase(SKU_X_SUB_2014_02) != null) {
        //            return SKU_X_SUB_2014_02;
        //        }
        //        if (inventory.getPurchase(SKU_X_SUB_LEGACY) != null) {
        //            return SKU_X_SUB_LEGACY;
        //        }
        //        return null;
        //    }

        //    /**
        //     * Checks if the user is subscribed to X features or has the deprecated X upgrade (so he gets
        //     * the subscription for life). Also sets the current state through {@link
        //     * AdvancedSettings#setSupporterState(Context, boolean)}.
        //     */
        //    public static boolean checkForSubscription(@NonNull Context context,
        //            @NonNull Inventory inventory) {
        //        /*
        //         * Check for items we own. Notice that for each purchase, we check the
        //         * developer payload to see if it's correct! See
        //         * verifyDeveloperPayload().
        //         */
        //
        //        // Does the user have the deprecated X Upgrade in-app purchase? If so unlock all features.
        //        Purchase premiumPurchase = inventory.getPurchase(SKU_X);
        //        boolean hasXUpgrade = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
        //
        //        // Does the user have an active unlock all subscription?
        //        String subscriptionSkuOrNull = latestSubscriptionSkuOrNull(inventory);
        //        boolean isSubscribedToX = subscriptionSkuOrNull != null
        //                && verifyDeveloperPayload(inventory.getPurchase(subscriptionSkuOrNull));
        //
        //        if (hasXUpgrade) {
        //            Timber.d("User has X SUBSCRIPTION for life.");
        //        } else {
        //            Timber.d("User has %s", isSubscribedToX ? "X SUBSCRIPTION" : "NO X SUBSCRIPTION");
        //        }
        //
        //        // notify the user about a change in subscription state
        //        boolean isSubscribedOld = AdvancedSettings.getLastSupporterState(context);
        //        boolean isSubscribed = hasXUpgrade || isSubscribedToX;
        //        if (!isSubscribedOld && isSubscribed) {
        //            Toast.makeText(context, R.string.upgrade_success, Toast.LENGTH_SHORT).show();
        //        } else if (isSubscribedOld && !isSubscribed) {
        //            onExpiredNotification(context);
        //        }
        //
        //        // Save current state until we query again
        //        AdvancedSettings.setSupporterState(context, isSubscribed);
        //
        //        return isSubscribed;
        //    }

        /**
         * Displays a notification that the subscription has expired. Its action opens [ ].
         */
        fun onExpiredNotification(context: Context) {
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
