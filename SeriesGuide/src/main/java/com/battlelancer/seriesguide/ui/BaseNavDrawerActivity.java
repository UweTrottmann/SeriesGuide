/*
 * Copyright 2014 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.CloudSetupActivity;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.amazon.AmazonBillingActivity;
import com.battlelancer.seriesguide.settings.TraktCredentials;
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
    protected void onStart() {
        super.onStart();

        // update account type and signed in user
        if (HexagonTools.isSignedIn(this)) {
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
     * Initializes the navigation drawer. Overriding activities should call this in their {@link
     * #onCreate(android.os.Bundle)} after {@link #setContentView(int)}.
     */
    public void setupNavDrawer() {
        actionBarToolbar = (Toolbar) findViewById(R.id.sgToolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        navigationView = (NavigationView) findViewById(R.id.navigation);

        // setup nav drawer account header
        navigationView.findViewById(R.id.containerDrawerAccount).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onNavItemClick(NAV_ITEM_ACCOUNT_ID);
                    }
                });
        textViewHeaderAccountType = (TextView) navigationView.findViewById(
                R.id.textViewDrawerItemAccount);
        textViewHeaderUser = (TextView) navigationView.findViewById(
                R.id.textViewDrawerItemUsername);

        // setup nav drawer items
        navigationView.inflateMenu(SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light
                ? R.menu.menu_drawer_light : R.menu.menu_drawer);
        navigationView.setItemIconTintList(getResources().getColorStateList(
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.sgColorNavDrawerIcon)));
        navigationView.setItemTextColor(getResources().getColorStateList(
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.sgColorNavDrawerText)));
        navigationView.setItemBackgroundResource(Utils.resolveAttributeToResourceId(getTheme(),
                R.attr.sgActivatedItemBackgroundDrawer));
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        menuItem.setChecked(true);
                        onNavItemClick(menuItem.getItemId());
                        return true;
                    }
                });
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
                Utils.trackAction(this, TAG_NAV_DRAWER, "Shows");
                break;
            case R.id.navigation_item_lists:
                if (this instanceof ListsActivity) {
                    break;
                }
                launchIntent = new Intent(this, ListsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Lists");
                break;
            case R.id.navigation_item_movies:
                if (this instanceof MoviesActivity) {
                    break;
                }
                launchIntent = new Intent(this, MoviesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Movies");
                break;
            case R.id.navigation_item_stats:
                if (this instanceof StatsActivity) {
                    break;
                }
                launchIntent = new Intent(this, StatsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Statistics");
                break;
            case R.id.navigation_sub_item_settings:
                launchIntent = new Intent(this, SeriesGuidePreferences.class);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Settings");
                break;
            case R.id.navigation_sub_item_help:
                launchIntent = new Intent(this, HelpActivity.class);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Help");
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
        navigationView.getMenu().findItem(menuItemId).setChecked(true);
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
