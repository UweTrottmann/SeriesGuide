/*
 * Copyright 2012 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;
import com.astuetz.PagerSlidingTabStrip;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

/**
 * Users can search for a movie, display detailed information and then check in
 * with trakt or GetGlue.
 */
public class MoviesActivity extends BaseTopActivity {

    private static final String TAG = "Movies";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // MovieSearchFragment needs a progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setSupportProgressBarIndeterminateVisibility(false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.movies);
        setupNavDrawer();

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.movies));
        actionBar.setIcon(R.drawable.ic_action_movie);
    }

    private void setupViews() {
        ViewPager pager = (ViewPager) findViewById(R.id.pagerMovies);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabsMovies);

        TabStripAdapter tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this, pager,
                tabs);
        // only show the trakt watchlist with valid credentials
        if (TraktSettings.hasTraktCredentials(this)) {
            tabsAdapter.addTab(R.string.movies_watchlist, MoviesWatchListFragment.class, null);
        }
        // movie search
        tabsAdapter.addTab(R.string.search, MovieSearchFragment.class, null);
        tabsAdapter.notifyTabsChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_MOVIES_POSITION);
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }

}
