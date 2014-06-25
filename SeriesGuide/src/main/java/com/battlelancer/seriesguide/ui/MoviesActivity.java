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

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Window;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.streams.FriendsMovieStreamFragment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // MovieSearchFragment needs a progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.movies);
        setupNavDrawer();

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setTitle(getString(R.string.movies));
        actionBar.setIcon(R.drawable.ic_action_movie);
    }

    private void setupViews() {
        TabStripAdapter tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this,
                (ViewPager) findViewById(R.id.pagerMovies),
                (SlidingTabLayout) findViewById(R.id.tabsMovies));
        // search
        tabsAdapter.addTab(R.string.search, MoviesSearchFragment.class, null);
        // watchlist
        tabsAdapter.addTab(R.string.movies_watchlist, MoviesWatchListFragment.class, null);
        // collection
        tabsAdapter.addTab(R.string.movies_collection, MoviesCollectionFragment.class, null);

        // trakt tabs only visible if connected
        if (TraktCredentials.get(this).hasCredentials()) {
            tabsAdapter.addTab(R.string.friends, FriendsMovieStreamFragment.class, null);
            tabsAdapter.addTab(R.string.user_stream, UserMovieStreamFragment.class, null);
        }

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
