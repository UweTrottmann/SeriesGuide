
package com.battlelancer.seriesguide.billing;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.NotificationSettings;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.billing.BillingViewModel;
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

public class BillingActivity extends BaseActivity {

    // The SKU product ids as set in the Developer Console
    public static final String SKU_X = "x_upgrade";

    public static final String SKU_X_SUB_2017_08 = "x_sub_2017_08";
    public static final String SKU_X_SUB_2016_05 = "x_sub_2016_05";
    public static final String SKU_X_SUB_2014_02 = "x_sub_2014_02";
    public static final String SKU_X_SUB_LEGACY = "x_subscription";
    public static final String SKU_X_SUB_NEW_PURCHASES = SKU_X_SUB_2017_08;

    private static final String SOME_STRING = "SURPTk9UQ0FSRUlGWU9VUElSQVRFVEhJUw==";
    private static final String PLAY_MANAGE_SUBS_ALL = "https://play.google.com/store/account/subscriptions";
    private static final String PLAY_MANAGE_SUBS_ONE =
            PLAY_MANAGE_SUBS_ALL + "?package=" + BuildConfig.APPLICATION_ID + "&sku=";

    private View progressScreen;
    private View contentContainer;
    private RecyclerView recyclerView;
    private SkuDetailsAdapter adapter;
    private Button buttonPass;
    private View textViewHasUpgrade;

    @Nullable private BillingViewModel billingViewModel;
    private String manageSubscriptionUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_billing);
        setupActionBar();

        manageSubscriptionUrl = PLAY_MANAGE_SUBS_ALL;

        setupViews();

        if (Utils.hasXpass(this)) {
            setWaitMode(false);
            updateViewStates(true);
        } else {
            setWaitMode(true);
            billingViewModel = ViewModelProviders.of(this).get(BillingViewModel.class);
            billingViewModel.getGoldStatusLiveData().observe(this, goldStatus -> {
                setWaitMode(false);
                updateViewStates(goldStatus != null && goldStatus.getEntitled());
            });
            billingViewModel.getSubsSkuDetailsListLiveData().observe(this,
                    augmentedSkuDetails -> adapter.setSkuDetailsList(augmentedSkuDetails));
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerViewBilling);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SkuDetailsAdapter() {
            @Override
            public void onSkuDetailsClicked(@NotNull AugmentedSkuDetails item) {
                if (billingViewModel != null) {
                    billingViewModel.makePurchase(BillingActivity.this, item);
                }
            }
        };
        recyclerView.setAdapter(adapter);

        Button buttonManageSubs = findViewById(R.id.buttonBillingManageSubscription);
        buttonManageSubs.setOnClickListener(
                v -> Utils.launchWebsite(v.getContext(), manageSubscriptionUrl));

        buttonPass = findViewById(R.id.buttonBillingGetPass);
        buttonPass.setOnClickListener(
                v -> Utils.launchWebsite(BillingActivity.this, getString(R.string.url_x_pass)
                ));

        textViewHasUpgrade = findViewById(R.id.textViewBillingExisting);

        progressScreen = findViewById(R.id.progressBarBilling);
        contentContainer = findViewById(R.id.containerBilling);

        findViewById(R.id.textViewBillingMoreInfo).setOnClickListener(
                v -> Utils.launchWebsite(BillingActivity.this, getString(R.string.url_whypay)
                ));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user has installed key app.
        if (Utils.hasXpass(this)) {
            updateViewStates(true);
        }
    }

    @Nullable
    public static String latestSubscriptionSkuOrNull(@NonNull Inventory inventory) {
        if (inventory.getPurchase(SKU_X_SUB_2017_08) != null) {
            return SKU_X_SUB_2017_08;
        }
        if (inventory.getPurchase(SKU_X_SUB_2016_05) != null) {
            return SKU_X_SUB_2016_05;
        }
        if (inventory.getPurchase(SKU_X_SUB_2014_02) != null) {
            return SKU_X_SUB_2014_02;
        }
        if (inventory.getPurchase(SKU_X_SUB_LEGACY) != null) {
            return SKU_X_SUB_LEGACY;
        }
        return null;
    }

    /**
     * Checks if the user is subscribed to X features or has the deprecated X upgrade (so he gets
     * the subscription for life). Also sets the current state through {@link
     * AdvancedSettings#setSupporterState(Context, boolean)}.
     */
    public static boolean checkForSubscription(@NonNull Context context,
            @NonNull Inventory inventory) {
        /*
         * Check for items we own. Notice that for each purchase, we check the
         * developer payload to see if it's correct! See
         * verifyDeveloperPayload().
         */

        // Does the user have the deprecated X Upgrade in-app purchase? If so unlock all features.
        Purchase premiumPurchase = inventory.getPurchase(SKU_X);
        boolean hasXUpgrade = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));

        // Does the user have an active unlock all subscription?
        String subscriptionSkuOrNull = latestSubscriptionSkuOrNull(inventory);
        boolean isSubscribedToX = subscriptionSkuOrNull != null
                && verifyDeveloperPayload(inventory.getPurchase(subscriptionSkuOrNull));

        if (hasXUpgrade) {
            Timber.d("User has X SUBSCRIPTION for life.");
        } else {
            Timber.d("User has %s", isSubscribedToX ? "X SUBSCRIPTION" : "NO X SUBSCRIPTION");
        }

        // notify the user about a change in subscription state
        boolean isSubscribedOld = AdvancedSettings.getLastSupporterState(context);
        boolean isSubscribed = hasXUpgrade || isSubscribedToX;
        if (!isSubscribedOld && isSubscribed) {
            Toast.makeText(context, R.string.upgrade_success, Toast.LENGTH_SHORT).show();
        } else if (isSubscribedOld && !isSubscribed) {
            onExpiredNotification(context);
        }

        // Save current state until we query again
        AdvancedSettings.setSupporterState(context, isSubscribed);

        return isSubscribed;
    }

    /**
     * Displays a notification that the subscription has expired. Its action opens {@link
     * BillingActivity}.
     */
    public static void onExpiredNotification(Context context) {
        NotificationCompat.Builder nb =
                new NotificationCompat.Builder(context, SgApp.NOTIFICATION_CHANNEL_ERRORS);
        NotificationSettings.setDefaultsForChannelErrors(context, nb);

        // set required attributes
        nb.setSmallIcon(R.drawable.ic_notification);
        nb.setContentTitle(context.getString(R.string.subscription_expired));
        nb.setContentText(context.getString(R.string.subscription_expired_details));
        nb.setTicker(context.getString(R.string.subscription_expired_details));
        nb.setAutoCancel(true);

        // build task stack
        Intent notificationIntent = new Intent(context, BillingActivity.class);
        PendingIntent contentIntent = TaskStackBuilder
                .create(context)
                .addNextIntent(new Intent(context, ShowsActivity.class))
                .addNextIntent(new Intent(context, SeriesGuidePreferences.class))
                .addNextIntent(notificationIntent)
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        nb.setContentIntent(contentIntent);

        // build the notification
        Notification notification = nb.build();

        // show the notification
        final NotificationManager nm = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(SgApp.NOTIFICATION_SUBSCRIPTION_ID, notification);
        }
    }

    /**
     * Verifies the developer payload of a purchase.
     */
    public static boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * Not doing anything sophisticated here,
         * this is open source anyhow.
         */

        return SOME_STRING.equals(payload);
    }

    private void updateViewStates(boolean hasUpgrade) {
        // Only enable key app button if the user does not have all access yet.
        buttonPass.setEnabled(!hasUpgrade);
        textViewHasUpgrade.setVisibility(hasUpgrade ? View.VISIBLE : View.GONE);
    }

    /**
     * Disables the purchase button and hides the subscribed message.
     */
    private void enableFallBackMode() {
        buttonPass.setEnabled(true);
        textViewHasUpgrade.setVisibility(View.GONE);
    }

    private void setWaitMode(boolean isActive) {
        progressScreen.setVisibility(isActive ? View.VISIBLE : View.GONE);
        contentContainer.setVisibility(isActive ? View.GONE : View.VISIBLE);
    }

    private void logAndShowAlertDialog(int errorResId, String message) {
        Timber.e(message);

        new AlertDialog.Builder(this)
                .setMessage(getString(errorResId) + "\n\n" + message)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }
}
