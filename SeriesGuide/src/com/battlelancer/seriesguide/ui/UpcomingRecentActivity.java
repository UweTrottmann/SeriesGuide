
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.ui.UpcomingFragment.InitBundle;
import com.battlelancer.seriesguide.ui.UpcomingFragment.UpcomingQuery;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TaskManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;

public class UpcomingRecentActivity extends BaseActivity implements OnAddShowListener {
    ViewPager mViewPager;

    TabsAdapter mTabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upcoming_multipane);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab upcomingTab = actionBar.newTab().setText(R.string.upcoming);
        ActionBar.Tab recentTab = actionBar.newTab().setText(R.string.recent);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, actionBar, mViewPager);
        // upcoming tab
        final Bundle argsUpcoming = new Bundle();
        argsUpcoming.putString(InitBundle.QUERY, UpcomingQuery.QUERY_UPCOMING);
        argsUpcoming.putString(InitBundle.SORTORDER, UpcomingQuery.SORTING_UPCOMING);
        argsUpcoming.putString(InitBundle.ANALYTICS_TAG, "/Upcoming");
        argsUpcoming.putInt(InitBundle.LOADER_ID, 10);
        argsUpcoming.putInt(InitBundle.EMPTY_STRING_ID, R.string.noupcoming);
        mTabsAdapter.addTab(upcomingTab, UpcomingFragment.class, argsUpcoming);

        // recent tab
        final Bundle argsRecent = new Bundle();
        argsRecent.putString(InitBundle.QUERY, UpcomingQuery.QUERY_RECENT);
        argsRecent.putString(InitBundle.SORTORDER, UpcomingQuery.SORTING_RECENT);
        argsRecent.putString(InitBundle.ANALYTICS_TAG, "/Recent");
        argsRecent.putInt(InitBundle.LOADER_ID, 20);
        argsRecent.putInt(InitBundle.EMPTY_STRING_ID, R.string.norecent);
        mTabsAdapter.addTab(recentTab, UpcomingFragment.class, argsRecent);

        // trakt friends tab
        final boolean isTraktSetup = ShareUtils.isTraktCredentialsValid(this);
        if (isTraktSetup) {
            ActionBar.Tab friendsTab = actionBar.newTab().setText(R.string.friends);
            mTabsAdapter.addTab(friendsTab, TraktFriendsFragment.class, null);
        }

        if (savedInstanceState != null) {
            actionBar.setSelectedNavigationItem(savedInstanceState.getInt("index"));
        }
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
        boolean isOnlyFavorites = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLYFAVORITES, false);

        MenuItem item = menu.findItem(R.id.menu_onlyfavorites);
        item.setChecked(isOnlyFavorites);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_onlyfavorites: {
                item.setChecked(!item.isChecked());
                SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(getApplicationContext());
                prefs.edit().putBoolean(SeriesGuidePreferences.KEY_ONLYFAVORITES, item.isChecked())
                        .commit();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost. It relies on a
     * trick. Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show. This is not sufficient for switching
     * between pages. So instead we make the content part of the tab host 0dp
     * high (it is not shown) and the TabsAdapter supplies its own dummy view to
     * show as the tab content. It listens to changes in tabs, and takes care of
     * switch to the correct paged in the ViewPager whenever the selected tab
     * changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter implements
            ViewPager.OnPageChangeListener, ActionBar.TabListener {
        private final Context mContext;

        private final ActionBar mActionBar;

        private final ViewPager mViewPager;

        private final ArrayList<String> mTabs = new ArrayList<String>();

        private final ArrayList<Bundle> mArgs = new ArrayList<Bundle>();

        public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mContext = activity;
            mActionBar = actionBar;
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args) {
            mTabs.add(clss.getName());
            mArgs.add(args);
            mActionBar.addTab(tab.setTabListener(this));
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            return Fragment.instantiate(mContext, mTabs.get(position), mArgs.get(position));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }

    /**
     * Provide a listener for the TraktFriendsFragment.
     */
    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance(this).performAddTask(show);
    }
}
