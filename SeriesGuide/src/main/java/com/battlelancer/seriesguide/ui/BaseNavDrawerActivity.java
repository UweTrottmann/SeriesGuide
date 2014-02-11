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

import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.backend.CloudSetupActivity;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.R;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Adds onto {@link BaseActivity} by attaching a navigation drawer.
 */
public abstract class BaseNavDrawerActivity extends BaseActivity
        implements AdapterView.OnItemClickListener {

    private static final String TAG_NAV_DRAWER = "Navigation Drawer";

    private DrawerLayout mDrawerLayout;

    private ListView mDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    public static final int MENU_ITEM_SHOWS_POSITION = 0;

    public static final int MENU_ITEM_LISTS_POSITION = 1;

    public static final int MENU_ITEM_MOVIES_POSITION = 2;

    public static final int MENU_ITEM_STATS_POSITION = 3;

    public static final int MENU_ITEM_SEARCH_POSITION = 4;

    public static final int MENU_ITEM_CLOUD_POSITION = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // set a theme based on user preference
        setTheme(SeriesGuidePreferences.THEME);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // close a previously opened drawer
        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
        }
    }

    /**
     * Initializes the navigation drawer. Overriding activities should call this in their {@link
     * #onCreate(android.os.Bundle)} after {@link #setContentView(int)}.
     */
    public void setupNavDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // setup menu adapter
        DrawerAdapter drawerAdapter = new DrawerAdapter(this);
        drawerAdapter.add(new DrawerItem(getString(R.string.shows),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableTv)));
        drawerAdapter.add(new DrawerItem(getString(R.string.lists),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableList)));
        drawerAdapter.add(new DrawerItem(getString(R.string.movies),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableMovie)));
        drawerAdapter.add(new DrawerItem(getString(R.string.statistics),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableStats)));
        drawerAdapter.add(new DrawerItem(getString(R.string.search_hint),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableSearch)));
        drawerAdapter.add(new DrawerItem(getString(R.string.hexagon_short),
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableLab)));

        mDrawerList.setAdapter(drawerAdapter);
        mDrawerList.setOnItemClickListener(this);

        // setup drawer indicator
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,
                R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        // don't show the indicator by default
        mDrawerToggle.setDrawerIndicatorEnabled(false);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Intent launchIntent = null;

        switch (position) {
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
            case MENU_ITEM_SEARCH_POSITION:
                if (this instanceof SearchActivity) {
                    break;
                }
                launchIntent = new Intent(this, SearchActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                Utils.trackAction(this, TAG_NAV_DRAWER, "Search");
                break;
            case MENU_ITEM_CLOUD_POSITION:
                if (this instanceof CloudSetupActivity) {
                    break;
                }
                launchIntent = new Intent(this, CloudSetupActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
        }

        // already displaying correct screen
        if (launchIntent == null) {
            mDrawerLayout.closeDrawer(mDrawerList);
            return;
        }

        startActivity(launchIntent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Returns true if the navigation drawer is open.
     */
    protected boolean isDrawerOpen() {
        return mDrawerLayout.isDrawerOpen(mDrawerList);
    }

    public void setDrawerIndicatorEnabled(boolean isEnabled) {
        mDrawerToggle.setDrawerIndicatorEnabled(isEnabled);
    }

    /**
     * Highlights the given position in the drawer menu. Activities listed in the drawer should
     * call
     * this in {@link #onStart()}.
     */
    public void setDrawerSelectedItem(int menuItemPosition) {
        mDrawerList.setItemChecked(menuItemPosition, true);
    }

    public boolean toggleDrawer(MenuItem item) {
        if (item != null && item.getItemId() == android.R.id.home && mDrawerToggle
                .isDrawerIndicatorEnabled()) {
            if (mDrawerLayout.isDrawerVisible(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return false;
    }

    public class DrawerItem {

        String mTitle;

        int mIconRes;

        public DrawerItem(String title, int iconRes) {
            mTitle = title;
            mIconRes = iconRes;
        }
    }

    private class DrawerCategory {

        public DrawerCategory() {
        }
    }

    public class DrawerAdapter extends ArrayAdapter<Object> {

        public DrawerAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof DrawerItem ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position) instanceof DrawerItem;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = getItem(position);

            if (item instanceof DrawerItem) {
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

                DrawerItem menuItem = (DrawerItem) item;
                holder.icon.setImageResource(menuItem.mIconRes);
                holder.title.setText(menuItem.mTitle);
            } else {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(
                            R.layout.drawer_category, parent, false);
                }
            }

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
