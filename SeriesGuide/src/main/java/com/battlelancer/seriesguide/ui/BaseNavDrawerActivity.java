package com.battlelancer.seriesguide.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.CloudSetupActivity;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity;
import com.battlelancer.seriesguide.customtabs.CustomTabsHelper;
import com.battlelancer.seriesguide.customtabs.FeedbackBroadcastReceiver;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktOAuthSettings;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Adds onto {@link BaseActivity} by attaching a navigation drawer.
 */
public abstract class BaseNavDrawerActivity extends BaseActivity {

    private static final String TAG_NAV_DRAWER = "Navigation Drawer";
    private static final int NAVDRAWER_CLOSE_DELAY = 250;
    private static final int NAV_ITEM_ACCOUNT_ID = 0;

    private Handler handler;
    private Toolbar actionBarToolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView textViewHeaderAccountType;
    private TextView textViewHeaderUser;

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

        boolean isSignedIntoCloud = HexagonTools.isSignedIn(this);
        if (!isSignedIntoCloud && HexagonSettings.getAccountName(this) != null) {
            // if not signed into hexagon, but still have an account name:
            // check if the required persmission is missing
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                    != PackageManager.PERMISSION_GRANTED) {
                onShowCloudPermissionWarning();
            }
        }

        // update account type and signed in user
        if (isSignedIntoCloud) {
            // connected to SG Cloud
            textViewHeaderAccountType.setText(R.string.hexagon);
            textViewHeaderUser.setText(HexagonSettings.getAccountName(this));
        } else if (TraktCredentials.get(this).hasCredentials()) {
            // connected to trakt
            textViewHeaderAccountType.setText(R.string.trakt);
            textViewHeaderUser.setText(TraktCredentials.get(this).getUsername());
        } else {
            // connected to nothing
            textViewHeaderAccountType.setText(R.string.trakt);
            textViewHeaderUser.setText(R.string.connect_trakt);
        }

        // if user is already a supporter, hide unlock action
        MenuItem menuItem = navigationView.getMenu().findItem(R.id.navigation_sub_item_unlock);
        menuItem.setEnabled(!Utils.hasAccessToX(this));
        menuItem.setVisible(!Utils.hasAccessToX(this));
    }

    /**
     * Implementers may choose to show a warning that Cloud is not signed in due to missing
     * permissions.
     */
    protected void onShowCloudPermissionWarning() {
        // do nothing
    }

    /**
     * Initializes the navigation drawer. Overriding activities should call this in their {@link
     * #onCreate(android.os.Bundle)} after {@link #setContentView(int)}.
     */
    public void setupNavDrawer() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        actionBarToolbar = (Toolbar) drawerLayout.findViewById(R.id.sgToolbar);

        navigationView = (NavigationView) drawerLayout.findViewById(R.id.navigation);

        // setup nav drawer account header
        View headerView = navigationView.getHeaderView(0);
        headerView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onNavItemClick(NAV_ITEM_ACCOUNT_ID);
                    }
                });
        textViewHeaderAccountType = (TextView) headerView.findViewById(
                R.id.textViewDrawerItemAccount);
        textViewHeaderUser = (TextView) headerView.findViewById(R.id.textViewDrawerItemUsername);

        // setup nav drawer items
        navigationView.inflateMenu(SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light
                ? R.menu.menu_drawer_light : R.menu.menu_drawer);
        navigationView.setItemIconTintList(ContextCompat.getColorStateList(this,
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.sgColorNavDrawerIcon)));
        navigationView.setItemTextColor(ContextCompat.getColorStateList(this,
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.sgColorNavDrawerText)));
        navigationView.setItemBackgroundResource(Utils.resolveAttributeToResourceId(getTheme(),
                R.attr.sgActivatedItemBackgroundDrawer));
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        onNavItemClick(menuItem.getItemId());
                        return false;
                    }
                });

        if (BuildConfig.DEBUG) {
            // add debug drawer
            View debugViews = getLayoutInflater().inflate(R.layout.debug_drawer, drawerLayout,
                    true);
            debugViews.findViewById(R.id.debug_buttonClearTraktRefreshToken).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TraktOAuthSettings.storeRefreshData(getApplicationContext(),
                                    "", 3600 /* 1 hour */);
                        }
                    });
            debugViews.findViewById(R.id.debug_buttonInvalidateTraktAccessToken).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TraktCredentials.get(getApplicationContext())
                                    .storeAccessToken("invalid-token");
                        }
                    });
            debugViews.findViewById(R.id.debug_buttoInvalidateTraktRefreshToken).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            TraktOAuthSettings.storeRefreshData(getApplicationContext(),
                                    "invalid-token", 3600 /* 1 hour */);
                        }
                    });
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
            case NAV_ITEM_ACCOUNT_ID: {
                // SG Cloud connection overrides trakt
                if (HexagonTools.isSignedIn(this)) {
                    launchIntent = new Intent(this, CloudSetupActivity.class);
                } else {
                    launchIntent = new Intent(this, ConnectTraktActivity.class);
                }
                Utils.trackAction(this, TAG_NAV_DRAWER, "Account");
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
                    //noinspection deprecation
                    builder.setToolbarColor(getResources().getColor(
                            Utils.resolveAttributeToResourceId(getTheme(), R.attr.colorPrimary)));
                    builder.setActionButton(
                            BitmapFactory.decodeResource(getResources(),
                                    R.drawable.ic_action_checkin),
                            getString(R.string.feedback),
                            PendingIntent.getBroadcast(getApplicationContext(), 0,
                                    new Intent(getApplicationContext(),
                                            FeedbackBroadcastReceiver.class), 0));
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
                Utils.trackAction(this, TAG_NAV_DRAWER, "Unlock");
                break;
        }

        // already displaying correct screen
        if (launchIntent != null) {
            final Intent finalLaunchIntent = launchIntent;
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToNavDrawerItem(finalLaunchIntent);
                }
            }, NAVDRAWER_CLOSE_DELAY);
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
        actionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
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
}
