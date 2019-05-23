package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.CloudSetupActivity;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity;
import com.battlelancer.seriesguide.service.FeedbackBroadcastReceiver;
import com.battlelancer.seriesguide.jobs.FlagJob;
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.ui.stats.StatsActivity;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.DrawableTools;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.uwetrottmann.seriesguide.customtabs.CustomTabsHelper;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Adds onto {@link BaseActivity} by attaching a navigation drawer.
 */
public abstract class BaseNavDrawerActivity extends BaseActivity {

    /**
     * Posted sticky while a service task is running.
     */
    public static class ServiceActiveEvent {
        private final boolean shouldSendToHexagon;
        private final boolean shouldSendToTrakt;

        public ServiceActiveEvent(boolean shouldSendToHexagon, boolean shouldSendToTrakt) {
            this.shouldSendToHexagon = shouldSendToHexagon;
            this.shouldSendToTrakt = shouldSendToTrakt;
        }

        public boolean shouldDisplayMessage() {
            return shouldSendToHexagon || shouldSendToTrakt;
        }

        public String getStatusMessage(Context context) {
            StringBuilder statusText = new StringBuilder();
            if (shouldSendToHexagon) {
                statusText.append(context.getString(R.string.hexagon_api_queued));
            }
            if (shouldSendToTrakt) {
                if (statusText.length() > 0) {
                    statusText.append(" ");
                }
                statusText.append(context.getString(R.string.trakt_submitqueued));
            }
            return statusText.toString();
        }
    }

    /**
     * Posted once a service action has completed. It may not have been successful.
     */
    public static class ServiceCompletedEvent {

        @Nullable public final String confirmationText;
        public boolean isSuccessful;
        @Nullable public final FlagJob flagJob;

        public ServiceCompletedEvent(@Nullable String confirmationText, boolean isSuccessful,
                @Nullable FlagJob flagJob) {
            this.confirmationText = confirmationText;
            this.isSuccessful = isSuccessful;
            this.flagJob = flagJob;
        }
    }

    private static final int NAVDRAWER_CLOSE_DELAY = 250;
    private static final int NAV_ITEM_ACCOUNT_CLOUD_ID = -1;
    private static final int NAV_ITEM_ACCOUNT_TRAKT_ID = -2;

    private Handler handler;
    private Toolbar actionBarToolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView textViewHeaderUserCloud;
    private TextView textViewHeaderUserTrakt;
    private Snackbar snackbarProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
    }

    @Override
    protected void setCustomTheme() {
        if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light) {
            setTheme(R.style.Theme_SeriesGuide_Light_Drawer);
        } else if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue) {
            setTheme(R.style.Theme_SeriesGuide_DarkBlue_Drawer);
        } else {
            setTheme(R.style.Theme_SeriesGuide_Drawer);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        ServiceActiveEvent event = EventBus.getDefault().getStickyEvent(ServiceActiveEvent.class);
        handleServiceActiveEvent(event);

        if (Utils.hasAccessToX(this) && HexagonSettings.shouldValidateAccount(this)) {
            onShowCloudAccountWarning();
        }

        // update signed-in accounts
        if (HexagonSettings.isEnabled(this)) {
            textViewHeaderUserCloud.setText(HexagonSettings.getAccountName(this));
        } else {
            textViewHeaderUserCloud.setText(R.string.hexagon_signin);
        }
        if (TraktCredentials.get(this).hasCredentials()) {
            textViewHeaderUserTrakt.setText(TraktCredentials.get(this).getUsername());
        } else {
            textViewHeaderUserTrakt.setText(R.string.connect_trakt);
        }
    }

    /**
     * Implementers may choose to show a warning that Cloud is not signed in.
     */
    protected void onShowCloudAccountWarning() {
        // do nothing
    }

    /**
     * Initializes the navigation drawer. Overriding activities should call this in their {@link
     * #onCreate(android.os.Bundle)} after {@link #setContentView(int)}.
     */
    public void setupNavDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        actionBarToolbar = drawerLayout.findViewById(R.id.sgToolbar);

        navigationView = drawerLayout.findViewById(R.id.navigation);

        // setup nav drawer account header
        View headerView = navigationView.getHeaderView(0);
        headerView.findViewById(R.id.containerDrawerAccountCloud).setOnClickListener(
                accountClickListener);
        headerView.findViewById(R.id.containerDrawerAccountTrakt).setOnClickListener(
                accountClickListener);
        textViewHeaderUserCloud = headerView.findViewById(R.id.textViewDrawerUserCloud);
        textViewHeaderUserTrakt = headerView.findViewById(R.id.textViewDrawerUserTrakt);
        maybeShowNoMoreUpdatesNotice(headerView);

        // setup nav drawer items
        navigationView.inflateMenu(R.menu.menu_drawer);
        if (!BuildConfig.DEBUG) {
            navigationView.getMenu().removeItem(R.id.navigation_sub_item_debug);
        }
        navigationView.setItemIconTintList(ContextCompat.getColorStateList(this,
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.sgColorNavDrawerIcon)));
        navigationView.setItemTextColor(ContextCompat.getColorStateList(this,
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.sgColorNavDrawerText)));
        navigationView.setItemBackgroundResource(Utils.resolveAttributeToResourceId(getTheme(),
                R.attr.sgActivatedItemBackgroundDrawer));
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    onNavItemClick(menuItem.getItemId());
                    return false;
                });
    }

    /**
     * Shows a no updates info text in the drawer if the device is running a version of Android
     * that will not be supported by a future version of this app.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void maybeShowNoMoreUpdatesNotice(View headerView) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            headerView.findViewById(R.id.textViewDrawerNoMoreUpdates).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
            return;
        }
        super.onBackPressed();
    }

    private void onNavItemClick(int itemId) {
        Intent launchIntent = null;

        switch (itemId) {
            case NAV_ITEM_ACCOUNT_CLOUD_ID: {
                launchIntent = new Intent(this, CloudSetupActivity.class);
                break;
            }
            case NAV_ITEM_ACCOUNT_TRAKT_ID: {
                launchIntent = new Intent(this, ConnectTraktActivity.class);
                break;
            }
            case R.id.navigation_item_shows:
                if (this instanceof ShowsActivity) {
                    break;
                }
                launchIntent = new Intent(this, ShowsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_item_lists:
                if (this instanceof ListsActivity) {
                    break;
                }
                launchIntent = new Intent(this, ListsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_item_movies:
                if (this instanceof MoviesActivity) {
                    break;
                }
                launchIntent = new Intent(this, MoviesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_item_stats:
                if (this instanceof StatsActivity) {
                    break;
                }
                launchIntent = new Intent(this, StatsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case R.id.navigation_sub_item_settings:
                launchIntent = new Intent(this, SeriesGuidePreferences.class);
                break;
            case R.id.navigation_sub_item_help:
                // if we cant find a package name, it means there is no browser that supports
                // Chrome Custom Tabs installed. So, we fallback to the webview activity.
                String packageName = CustomTabsHelper.getPackageNameToUse(this);
                if (packageName == null) {
                    launchIntent = new Intent(this, HelpActivity.class);
                } else {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    builder.setShowTitle(true);
                    builder.setToolbarColor(getResources().getColor(
                            Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));
                    Bitmap icon = DrawableTools
                            .getBitmapFromVectorDrawable(this, R.drawable.ic_checkin_black_24dp);
                    builder.setActionButton(icon,
                            getString(R.string.feedback),
                            PendingIntent.getBroadcast(getApplicationContext(), 0,
                                    new Intent(getApplicationContext(),
                                            FeedbackBroadcastReceiver.class), 0),
                            true);
                    CustomTabsIntent customTabsIntent = builder.build();
                    customTabsIntent.intent.setPackage(packageName);
                    customTabsIntent.intent.setData(Uri.parse(getString(R.string.help_url)));
                    launchIntent = customTabsIntent.intent;
                }
                break;
            case R.id.navigation_sub_item_unlock:
                if (Utils.isAmazonVersion()) {
                    launchIntent = new Intent(this, AmazonBillingActivity.class);
                } else {
                    launchIntent = new Intent(this, BillingActivity.class);
                }
                break;
            case R.id.navigation_sub_item_debug:
                if (BuildConfig.DEBUG) {
                    DialogTools.safeShow(new DebugViewFragment(), getSupportFragmentManager(),
                            "debugViewDialog");
                }
                break;
        }

        // already displaying correct screen
        if (launchIntent != null) {
            final Intent finalLaunchIntent = launchIntent;
            handler.postDelayed(() -> goToNavDrawerItem(finalLaunchIntent), NAVDRAWER_CLOSE_DELAY);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void goToNavDrawerItem(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg);
    }

    /**
     * Returns true if the navigation drawer is open.
     */
    public boolean isNavDrawerOpen() {
        return drawerLayout.isDrawerOpen(navigationView);
    }

    public void setDrawerIndicatorEnabled() {
        actionBarToolbar.setNavigationIcon(R.drawable.ic_menu_24dp);
        actionBarToolbar.setNavigationContentDescription(R.string.drawer_open);
    }

    /**
     * Highlights the given position in the drawer menu. Activities listed in the drawer should call
     * this in {@link #onStart()}.
     */
    public void setDrawerSelectedItem(@IdRes int menuItemId) {
        navigationView.setCheckedItem(menuItemId);
    }

    public void openNavDrawer() {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public void closeNavDrawer() {
        drawerLayout.closeDrawer(GravityCompat.START);
    }

    public boolean toggleDrawer(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerVisible(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return false;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEpisodeTask(ServiceActiveEvent event) {
        handleServiceActiveEvent(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEpisodeTask(ServiceCompletedEvent event) {
        if (event.confirmationText != null) {
            // show a confirmation/error text
            Snackbar snackbarCompleted = Snackbar
                    .make(getSnackbarParentView(), event.confirmationText,
                            event.isSuccessful ? Snackbar.LENGTH_SHORT : Snackbar.LENGTH_LONG);
            // replaces any previous snackbar, including the indefinite progress one
            snackbarCompleted.show();
        } else {
            handleServiceActiveEvent(null);
        }
    }

    /**
     * Return a view to pass to {@link Snackbar#make(View, CharSequence, int) Snackbar.make},
     * ideally a {@link CoordinatorLayout CoordinatorLayout}.
     */
    protected View getSnackbarParentView() {
        return findViewById(android.R.id.content);
    }

    private void handleServiceActiveEvent(@Nullable ServiceActiveEvent event) {
        if (event != null && event.shouldDisplayMessage()) {
            if (snackbarProgress == null) {
                snackbarProgress = Snackbar.make(getSnackbarParentView(),
                        event.getStatusMessage(this), Snackbar.LENGTH_INDEFINITE);
            } else {
                snackbarProgress.setText(event.getStatusMessage(this));
                snackbarProgress.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE);
            }
            snackbarProgress.show();
        } else if (snackbarProgress != null) {
            snackbarProgress.dismiss();
        }
    }

    private View.OnClickListener accountClickListener = v -> {
        if (v.getId() == R.id.containerDrawerAccountCloud) {
            onNavItemClick(NAV_ITEM_ACCOUNT_CLOUD_ID);
        } else if (v.getId() == R.id.containerDrawerAccountTrakt) {
            onNavItemClick(NAV_ITEM_ACCOUNT_TRAKT_ID);
        }
    };
}
