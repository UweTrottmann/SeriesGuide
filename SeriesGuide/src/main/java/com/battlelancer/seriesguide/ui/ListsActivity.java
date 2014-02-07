
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

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.PagerSlidingTabStrip;
import com.astuetz.PagerSlidingTabStrip.OnTabClickListener;
import com.battlelancer.seriesguide.adapters.ListsPagerAdapter;
import com.battlelancer.seriesguide.interfaces.OnListsChangedListener;
import com.battlelancer.seriesguide.ui.dialogs.AddListDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListManageDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.R;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

/**
 * Hosts a view pager to display and manage lists of shows, seasons and episodes.
 */
public class ListsActivity extends BaseTopShowsActivity implements OnListsChangedListener {

    public static final String TAG = "Lists";

    private ListsPagerAdapter mListsAdapter;

    private ViewPager mPager;

    private PagerSlidingTabStrip mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lists);
        setupNavDrawer();

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(Utils.resolveAttributeToResourceId(getTheme(), R.attr.drawableList));
        actionBar.setTitle(R.string.lists);
    }

    private void setupViews() {
        mListsAdapter = new ListsPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pagerLists);
        mPager.setAdapter(mListsAdapter);

        mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabsLists);
        mTabs.setViewPager(mPager);
        mTabs.setOnTabClickListener(new OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                if (mPager.getCurrentItem() == position) {
                    String listId = mListsAdapter.getListId(position);
                    ListManageDialogFragment.showListManageDialog(listId,
                            getSupportFragmentManager());
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_LISTS_POSITION);
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListsAdapter.onCleanUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.SeriesGuideThemeLight;
        getSupportMenuInflater()
                .inflate(isLightTheme ? R.menu.lists_menu_light : R.menu.lists_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        boolean isDrawerOpen = isDrawerOpen();
        menu.findItem(R.id.menu_list_add).setVisible(!isDrawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_list_add) {
            fireTrackerEvent("Add list");
            AddListDialogFragment.showAddListDialog(getSupportFragmentManager());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListsChanged() {
        // refresh list adapter
        mListsAdapter.onListsChanged();
        // update indicator and view pager
        mTabs.notifyDataSetChanged();
    }

    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
