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

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
public abstract class BaseNavDrawerActivity extends BaseActivity
        implements AdapterView.OnItemClickListener {

    private static final String TAG_NAV_DRAWER = "Navigation Drawer";
    private static final int NAVDRAWER_CLOSE_DELAY = 250;

    public static final int MENU_ITEM_ACCOUNT = 0;
    // DIVIDER IN BETWEEN HERE                  1
    public static final int MENU_ITEM_SHOWS_POSITION = 2;
    public static final int MENU_ITEM_LISTS_POSITION = 3;
    public static final int MENU_ITEM_MOVIES_POSITION = 4;
    public static final int MENU_ITEM_STATS_POSITION = 5;
    // DIVIDER IN BETWEEN HERE                         6
    public static final int MENU_ITEM_SETTINGS_POSITION = 7;
    public static final int MENU_ITEM_HELP_POSITION = 8;
    public static final int MENU_ITEM_SUBSCRIBE_POSITION = 9; // not always shown

    private Handler mHandler;
    private Toolbar mActionBarToolbar;
    private DrawerLayout mDrawerLayout;
    private View mDrawerView;
    private ListView mDrawerList;
    private DrawerAdapter mDrawerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mDrawerAdapter.setSubscribeVisible(!Utils.hasAccessToX(this));
    }

    /**
     * Initializes the navigation drawer. Overriding activities should call this in their {@link
     * #onCreate(android.os.Bundle)} after {@link #setContentView(int)}.
     */
    public void setupNavDrawer() {
        mActionBarToolbar = (Toolbar) findViewById(R.id.sgToolbar);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerView = findViewById(R.id.drawer_view);

        mDrawerList = (ListView) findViewById(R.id.drawer_list);

        // setup menu adapter
        mDrawerAdapter = new DrawerAdapter(this);
        mDrawerAdapter.add(new DrawerItemAccount());

        mDrawerAdapter.add(new DrawerItemDivider());

        mDrawerAdapter.add(new DrawerItem(getString(R.string.shows),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableTv)));
        mDrawerAdapter.add(new DrawerItem(getString(R.string.lists),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableList)));
        mDrawerAdapter.add(new DrawerItem(getString(R.string.movies),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableMovie)));
        mDrawerAdapter.add(new DrawerItem(getString(R.string.statistics),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableStats)));

        mDrawerAdapter.add(new DrawerItemDivider());

        mDrawerAdapter.add(new DrawerItem(getString(R.string.preferences),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableSettings)));
        mDrawerAdapter.add(new DrawerItem(getString(R.string.help),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableHelp)));
        mDrawerAdapter.add(new DrawerItem(getString(R.string.action_upgrade),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableStar)));

        mDrawerList.setAdapter(mDrawerAdapter);
        mDrawerList.setOnItemClickListener(this);
    }

    @Override
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent launchIntent = null;

        mDrawerAdapter.getItem(position);
        switch (position) {
            case MENU_ITEM_ACCOUNT: {
                // SG Cloud connection overrides trakt
                if (HexagonTools.isSignedIn(this)) {
                    launchIntent = new Intent(this, CloudSetupActivity.class);
                } else {
                    launchIntent = new Intent(this, ConnectTraktActivity.class);
                }
                Utils.trackAction(this, TAG_NAV_DRAWER, "Account");
                break;
            }
            case MENU_ITEM_SHOWS_POSITION:
                if (this instanceof ShowsActivity) {
                    break;
                }
                launchIntent = new Intent(this, ShowsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Shows");
                break;
            case MENU_ITEM_LISTS_POSITION:
                if (this instanceof ListsActivity) {
                    break;
                }
                launchIntent = new Intent(this, ListsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Lists");
                break;
            case MENU_ITEM_MOVIES_POSITION:
                if (this instanceof MoviesActivity) {
                    break;
                }
                launchIntent = new Intent(this, MoviesActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Movies");
                break;
            case MENU_ITEM_STATS_POSITION:
                if (this instanceof StatsActivity) {
                    break;
                }
                launchIntent = new Intent(this, StatsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Statistics");
                break;
            case MENU_ITEM_SETTINGS_POSITION:
                launchIntent = new Intent(this, SeriesGuidePreferences.class);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Settings");
                break;
            case MENU_ITEM_HELP_POSITION:
                launchIntent = new Intent(this, HelpActivity.class);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Help");
                break;
            case MENU_ITEM_SUBSCRIBE_POSITION:
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
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToNavDrawerItem(finalLaunchIntent);
                }
            }, NAVDRAWER_CLOSE_DELAY);
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void goToNavDrawerItem(Intent intent) {
        startActivity(intent);
        overridePendingTransition(R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg);
    }

    /**
     * Returns true if the navigation drawer is open.
     */
    public boolean isNavDrawerOpen() {
        return mDrawerLayout.isDrawerOpen(mDrawerView);
    }

    public void setDrawerIndicatorEnabled() {
        mActionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
        mActionBarToolbar.setNavigationContentDescription(R.string.drawer_open);
    }

    /**
     * Highlights the given position in the drawer menu. Activities listed in the drawer should call
     * this in {@link #onStart()}.
     */
    public void setDrawerSelectedItem(int menuItemPosition) {
        mDrawerList.setItemChecked(menuItemPosition, true);
    }

    public void openNavDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    public void closeNavDrawer() {
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    public boolean toggleDrawer(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return false;
    }

    public static class DrawerItem {

        String mTitle;
        int mIconRes;

        public DrawerItem(String title, int iconRes) {
            mTitle = title;
            mIconRes = iconRes;
        }
    }

    public static class DrawerItemDivider extends DrawerItem {
        public DrawerItemDivider() {
            super(null, 0);
        }
    }

    private static class DrawerItemAccount extends DrawerItem {
        public DrawerItemAccount() {
            super(null, 0);
        }
    }

    public static class DrawerAdapter extends ArrayAdapter<DrawerItem> {

        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_DIVIDER = 1;
        private static final int VIEW_TYPE_ACCOUNT = 2;

        private final ColorStateList colorStateListIcon;

        private boolean isSubscribeVisible;

        public DrawerAdapter(Context context) {
            super(context, 0);

            colorStateListIcon = getContext().getResources()
                    .getColorStateList(Utils.resolveAttributeToResourceId(context.getTheme(),
                            R.attr.sgColorNavDrawerIcon));
        }

        public void setSubscribeVisible(boolean visible) {
            isSubscribeVisible = visible;
        }

        @Override
        public int getCount() {
            // assumes that subscribe is last item
            return isSubscribeVisible ? super.getCount() : super.getCount() - 1;
        }

        @Override
        public int getItemViewType(int position) {
            DrawerItem item = getItem(position);
            if (item instanceof DrawerItemDivider) {
                return VIEW_TYPE_DIVIDER;
            }
            if (item instanceof DrawerItemAccount) {
                return VIEW_TYPE_ACCOUNT;
            }
            return VIEW_TYPE_ITEM;
        }

        @Override
        public int getViewTypeCount() {
            return 3;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            DrawerItem item = getItem(position);
            return !(item instanceof DrawerItemDivider);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            DrawerItem item = getItem(position);

            int type = getItemViewType(position);
            // divider and account only appear once, so don't use a view holder
            if (type == VIEW_TYPE_DIVIDER) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.drawer_item_divider, parent, false);
                return convertView;
            }
            if (type == VIEW_TYPE_ACCOUNT) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.drawer_item_account, parent, false);
                TextView account = (TextView) convertView.findViewById(
                        R.id.textViewDrawerItemAccount);
                TextView user = (TextView) convertView.findViewById(
                        R.id.textViewDrawerItemUsername);

                if (HexagonTools.isSignedIn(getContext())) {
                    // connected to SG Cloud
                    account.setText(R.string.hexagon);
                    user.setText(HexagonSettings.getAccountName(getContext()));
                } else if (TraktCredentials.get(getContext()).hasCredentials()) {
                    // connected to trakt
                    account.setText(R.string.trakt);
                    user.setText(TraktCredentials.get(getContext()).getUsername());
                } else {
                    // connected to nothing
                    account.setText(R.string.trakt);
                    user.setText(R.string.connect_trakt);
                }
                return convertView;
            }

            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(
                        R.layout.drawer_item, parent, false);
                holder = new ViewHolder();
                holder.attach(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            // title
            holder.title.setText(item.mTitle);

            // compat tintable drawable for the icon
            Drawable icon = DrawableCompat.wrap(
                    getContext().getResources().getDrawable(item.mIconRes));
            DrawableCompat.setTintList(icon, colorStateListIcon);
            holder.icon.setImageDrawable(icon);

            return convertView;
        }
    }

    private static class ViewHolder {

        public TextView title;

        public ImageView icon;

        public void attach(View v) {
            icon = (ImageView) v.findViewById(R.id.menu_icon);
            title = (TextView) v.findViewById(R.id.menu_title);
        }
    }
}
