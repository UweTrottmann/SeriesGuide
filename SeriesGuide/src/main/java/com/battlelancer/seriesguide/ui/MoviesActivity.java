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

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;

/**
 * Movie section of the app, displays various movie tabs.
 */
public class MoviesActivity extends BaseTopActivity {

    public static final int SEARCH_LOADER_ID = 100;
    public static final int NOW_TRAKT_USER_LOADER_ID = 101;
    public static final int NOW_TRAKT_FRIENDS_LOADER_ID = 102;
    public static final int WATCHLIST_LOADER_ID = 103;
    public static final int COLLECTION_LOADER_ID = 104;

    private static final String TAG = "Movies";
    private static final int TAB_COUNT_WITH_TRAKT = 4;

    private TabStripAdapter tabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs_drawer);
        setupActionBar();
        setupNavDrawer();

        setupViews();
        setupSyncProgressBar(R.id.progressBarTabs);
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.movies));
        }
    }

    private void setupViews() {
        // tabs
        tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this,
                (ViewPager) findViewById(R.id.viewPagerTabs),
                (SlidingTabLayout) findViewById(R.id.tabLayoutTabs));
        // search
        tabsAdapter.addTab(R.string.search, MoviesSearchFragment.class, null);
        // trakt-only tabs should only be visible if connected
        if (TraktCredentials.get(this).hasCredentials()) {
            // (what to watch) now
            tabsAdapter.addTab(R.string.now_tab, MoviesNowFragment.class, null);
        }
        // watchlist
        tabsAdapter.addTab(R.string.movies_watchlist, MoviesWatchListFragment.class, null);
        // collection
        tabsAdapter.addTab(R.string.movies_collection, MoviesCollectionFragment.class, null);

        tabsAdapter.notifyTabsChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_MOVIES_POSITION);

        // add trakt-only tab if user just signed in
        maybeAddNowTab();
    }

    @Override
    protected void onResume() {
        super.onResume();

        supportInvalidateOptionsMenu();
    }

    private void maybeAddNowTab() {
        int currentTabCount = tabsAdapter.getCount();
        boolean shouldShowTraktTabs = TraktCredentials.get(this).hasCredentials();
        if (shouldShowTraktTabs && currentTabCount != TAB_COUNT_WITH_TRAKT) {
            tabsAdapter.addTab(R.string.now_tab, MoviesNowFragment.class, null);
            // update tabs
            tabsAdapter.notifyTabsChanged();
        }
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
