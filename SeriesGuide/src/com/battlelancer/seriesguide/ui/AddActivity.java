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

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.slidingmenu.lib.SlidingMenu;
import com.uwetrottmann.seriesguide.R;
import com.viewpagerindicator.TabPageIndicator;

/**
 * Hosts various fragments in a {@link ViewPager} which allow adding shows to
 * the database.
 */
public class AddActivity extends BaseActivity implements OnAddShowListener {

    private AddPagerAdapter mAdapter;

    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The TvdbAddFragment uses a progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.addactivity_pager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);
        setSupportProgressBarIndeterminateVisibility(false);
        
        getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

        mAdapter = new AddPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    public static class AddPagerAdapter extends FragmentPagerAdapter {

        private static final int DEFAULT_TABCOUNT = 2;
        private static final int TRAKT_CONNECTED_TABCOUNT = 5;

        public static final int TRENDING_TAB_POSITION = 0;
        public static final int RECOMMENDED_TAB_POSITION = 1;
        public static final int LIBRARY_TAB_POSITION = 2;
        public static final int WATCHLIST_TAB_POSITION = 3;

        public static final int SEARCH_TAB_DEFAULT_POSITION = 1;
        public static final int SEARCH_TAB_CONNECTED_POSITION = 4;

        private Context mContext;

        public AddPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            int count = getCount();
            if ((count == DEFAULT_TABCOUNT && position == SEARCH_TAB_DEFAULT_POSITION)
                    || (count == TRAKT_CONNECTED_TABCOUNT && position == SEARCH_TAB_CONNECTED_POSITION)) {
                return TvdbAddFragment.newInstance();
            } else {
                return TraktAddFragment.newInstance(position);
            }
        }

        @Override
        public int getCount() {
            final boolean isValidCredentials = Utils.isTraktCredentialsValid(mContext);
            if (isValidCredentials) {
                // show trakt recommended and libraried shows, too
                return TRAKT_CONNECTED_TABCOUNT;
            } else {
                // show search results and trakt trending shows
                return DEFAULT_TABCOUNT;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (getCount() == TRAKT_CONNECTED_TABCOUNT) {
                switch (position) {
                    case TRENDING_TAB_POSITION:
                        return mContext.getString(R.string.trending).toUpperCase();
                    case RECOMMENDED_TAB_POSITION:
                        return mContext.getString(R.string.recommended).toUpperCase();
                    case LIBRARY_TAB_POSITION:
                        return mContext.getString(R.string.library).toUpperCase();
                    case WATCHLIST_TAB_POSITION:
                        return mContext.getString(R.string.watchlist).toUpperCase();
                    case SEARCH_TAB_CONNECTED_POSITION:
                        return mContext.getString(R.string.search_button).toUpperCase();
                }
            } else {
                switch (position) {
                    case TRENDING_TAB_POSITION:
                        return mContext.getString(R.string.trending).toUpperCase();
                    case SEARCH_TAB_DEFAULT_POSITION:
                        return mContext.getString(R.string.search_button).toUpperCase();
                }
            }
            return "";
        }

    }

    @Override
    public void onAddShow(SearchResult show) {
        // clear the search field (if it is shown)
        EditText searchbox = (EditText) findViewById(R.id.searchbox);
        if (searchbox != null) {
            searchbox.setText("");
        }

        TaskManager.getInstance(this).performAddTask(show);
    }
}
