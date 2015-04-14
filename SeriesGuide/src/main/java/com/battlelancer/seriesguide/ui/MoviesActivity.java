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
import com.battlelancer.seriesguide.ui.streams.UserMovieStreamFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;

/**
 * Users can search for a movie, display detailed information and then check in with trakt or
 * GetGlue.
 */
public class MoviesActivity extends BaseTopActivity {

    public static final int SEARCH_LOADER_ID = 100;
    public static final int WATCHLIST_LOADER_ID = 101;
    public static final int COLLECTION_LOADER_ID = 102;
    public static final int FRIENDS_LOADER_ID = 103;
    public static final int USER_LOADER_ID = 104;

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
        actionBar.setTitle(getString(R.string.movies));
    }

    private void setupViews() {
        // tabs
        tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this,
                (ViewPager) findViewById(R.id.viewPagerTabs),
                (SlidingTabLayout) findViewById(R.id.tabLayoutTabs));
        // search
        tabsAdapter.addTab(R.string.search, MoviesSearchFragment.class, null);
        // watchlist
        tabsAdapter.addTab(R.string.movies_watchlist, MoviesWatchListFragment.class, null);
        // collection
        tabsAdapter.addTab(R.string.movies_collection, MoviesCollectionFragment.class, null);

        // trakt tabs only visible if connected
        if (TraktCredentials.get(this).hasCredentials()) {
            tabsAdapter.addTab(R.string.user_stream, UserMovieStreamFragment.class, null);
        }

        tabsAdapter.notifyTabsChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_MOVIES_POSITION);

        // add trakt tabs if user just signed in
        maybeAddTraktTabs();
    }

    @Override
    protected void onResume() {
        super.onResume();

        supportInvalidateOptionsMenu();
    }

    private void maybeAddTraktTabs() {
        int currentTabCount = tabsAdapter.getCount();
        boolean shouldShowTraktTabs = TraktCredentials.get(this).hasCredentials();

        if (shouldShowTraktTabs && currentTabCount != TAB_COUNT_WITH_TRAKT) {
            tabsAdapter.addTab(R.string.user_stream, UserMovieStreamFragment.class, null);
            // update tabs
            tabsAdapter.notifyTabsChanged();
        }
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
