/*
 * Copyright 2011 Uwe Trottmann
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.ActivitySettings;
import com.battlelancer.seriesguide.ui.UpcomingFragment.ActivityType;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

public class UpcomingRecentActivity extends BaseTopShowsActivity implements OnAddShowListener {
    private static final String TAG = "Activity";

    public interface InitBundle {
        String SELECTED_TAB = "selectedtab";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMenu().setContentView(R.layout.upcoming);

        // if coming from a notification, set last cleared time
        NotificationService.handleDeleteIntent(this, getIntent());

        setupActionBar();

        setupViews(savedInstanceState);
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.activity);
        actionBar.setIcon(R.drawable.ic_action_upcoming);
    }

    private void setupViews(Bundle savedInstanceState) {
        ViewPager pager = (ViewPager) findViewById(R.id.pagerUpcoming);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabsUpcoming);

        ActivityTabPageAdapter tabsAdapter = new ActivityTabPageAdapter(
                getSupportFragmentManager(), this, pager, tabs);
        // upcoming tab
        final Bundle argsUpcoming = new Bundle();
        argsUpcoming.putString(UpcomingFragment.InitBundle.TYPE, ActivityType.UPCOMING);
        argsUpcoming.putString(UpcomingFragment.InitBundle.ANALYTICS_TAG, "Upcoming");
        argsUpcoming.putInt(UpcomingFragment.InitBundle.LOADER_ID, 10);
        argsUpcoming.putInt(UpcomingFragment.InitBundle.EMPTY_STRING_ID, R.string.noupcoming);
        tabsAdapter.addTab(R.string.upcoming, UpcomingFragment.class, argsUpcoming);

        // recent tab
        final Bundle argsRecent = new Bundle();
        argsRecent.putString(UpcomingFragment.InitBundle.TYPE, ActivityType.RECENT);
        argsRecent.putString(UpcomingFragment.InitBundle.ANALYTICS_TAG, "Recent");
        argsRecent.putInt(UpcomingFragment.InitBundle.LOADER_ID, 20);
        argsRecent.putInt(UpcomingFragment.InitBundle.EMPTY_STRING_ID, R.string.norecent);
        tabsAdapter.addTab(R.string.recent, UpcomingFragment.class, argsRecent);

        // trakt friends tab
        final boolean isTraktSetup = ServiceUtils.isTraktCredentialsValid(this);
        if (isTraktSetup) {
            tabsAdapter.addTab(R.string.friends, TraktFriendsFragment.class, null);
        }

        // set starting tab
        int selection = 0;
        if (savedInstanceState != null) {
            selection = savedInstanceState.getInt("index");
        } else {
            Intent intent = getIntent();
            Bundle extras = intent.getExtras();
            if (extras != null) {
                selection = extras.getInt(InitBundle.SELECTED_TAB, 0);
            } else {
                // use saved selection
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                selection = prefs.getInt(SeriesGuidePreferences.KEY_ACTIVITYTAB, 0);
            }
        }
        // never select a non-existent tab
        if (selection > tabsAdapter.getCount() - 1) {
            selection = 0;
        }
        pager.setCurrentItem(selection);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_menu, menu);

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        // set menu items to current values
        menu.findItem(R.id.menu_onlyfavorites).setChecked(ActivitySettings.isOnlyFavorites(this));
        menu.findItem(R.id.menu_nospecials).setChecked(ActivitySettings.isHidingSpecials(this));
        menu.findItem(R.id.menu_nowatched).setChecked(
                prefs.getBoolean(SeriesGuidePreferences.KEY_NOWATCHED, false));
        menu.findItem(R.id.menu_infinite_scrolling).setChecked(
                ActivitySettings.isInfiniteScrolling(this));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_onlyfavorites) {
            storeBooleanPreference(item, ActivitySettings.KEY_ONLY_FAVORITES);
            fireTrackerEvent("Only favorite shows Toggle");
            return true;
        } else if (itemId == R.id.menu_nospecials) {
            storeBooleanPreference(item, ActivitySettings.KEY_HIDE_SPECIALS);
            fireTrackerEvent("Hide specials Toggle");
            return true;
        } else if (itemId == R.id.menu_nowatched) {
            storeBooleanPreference(item, SeriesGuidePreferences.KEY_NOWATCHED);
            fireTrackerEvent("Hide watched Toggle");
            return true;
        } else if (itemId == R.id.menu_infinite_scrolling) {
            storeBooleanPreference(item, ActivitySettings.KEY_INFINITE_SCROLLING);
            fireTrackerEvent("Infinite Scrolling Toggle");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    /**
     * Special {@link TabPagerIndicatorAdapter} which saves the currently
     * selected page to preferences, so we can restore it when the user comes
     * back later.
     */
    public static class ActivityTabPageAdapter extends TabStripAdapter implements
            OnPageChangeListener {
        private SharedPreferences mPrefs;

        public ActivityTabPageAdapter(FragmentManager fm, Context context, ViewPager pager,
                PagerSlidingTabStrip tabs) {
            super(fm, context, pager, tabs);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            tabs.setOnPageChangeListener(this);
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            // save selected tab index
            mPrefs.edit().putInt(SeriesGuidePreferences.KEY_ACTIVITYTAB, position).commit();
        }

    }

    /**
     * Provide a listener for the TraktFriendsFragment.
     */
    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance(this).performAddTask(show);
    }

    @Override
    protected void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Action Item", label, (long) 0);
    }

    private void storeBooleanPreference(MenuItem item, String key) {
        item.setChecked(!item.isChecked());
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        prefs.edit().putBoolean(key, item.isChecked()).commit();
    }
}
