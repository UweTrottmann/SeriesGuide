
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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ListsPagerAdapter;
import com.battlelancer.seriesguide.interfaces.OnListsChangedListener;
import com.battlelancer.seriesguide.ui.dialogs.AddListDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListManageDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;

/**
 * Hosts a view pager to display and manage lists of shows, seasons and episodes.
 */
public class ListsActivity extends BaseTopActivity implements OnListsChangedListener {

    public static final String TAG = "Lists";

    private ListsPagerAdapter mListsAdapter;

    private ViewPager mPager;

    private SlidingTabLayout mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lists);
        setupNavDrawer();

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getActionBar();
        actionBar.setIcon(R.drawable.ic_action_list);
        actionBar.setTitle(R.string.lists);
    }

    private void setupViews() {
        mListsAdapter = new ListsPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pagerLists);
        mPager.setAdapter(mListsAdapter);

        mTabs = (SlidingTabLayout) findViewById(R.id.tabsLists);
        mTabs.setCustomTabView(R.layout.tabstrip_item_allcaps, R.id.textViewTabStripItem);
        mTabs.setSelectedIndicatorColors(getResources().getColor(
                Utils.resolveAttributeToResourceId(getTheme(), R.attr.sgColorAccent)));
        mTabs.setBottomBorderColor(getResources().getColor(
                Utils.resolveAttributeToResourceId(getTheme(),
                        R.attr.sgColorTabStripUnderline)
        ));
        mTabs.setOnTabClickListener(new SlidingTabLayout.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                if (mPager.getCurrentItem() == position) {
                    String listId = mListsAdapter.getListId(position);
                    ListManageDialogFragment.showListManageDialog(listId,
                            getSupportFragmentManager());
                }
            }
        });
        mTabs.setViewPager(mPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_LISTS_POSITION);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListsAdapter.onCleanUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.lists_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean isDrawerOpen = isDrawerOpen();
        menu.findItem(R.id.menu_action_lists_add).setVisible(!isDrawerOpen);
        menu.findItem(R.id.menu_action_lists_search).setVisible(!isDrawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_lists_add) {
            AddListDialogFragment.showAddListDialog(getSupportFragmentManager());
            fireTrackerEvent("Add list");
            return true;
        }
        if (itemId == R.id.menu_action_lists_search) {
            startActivity(new Intent(this, SearchActivity.class));
            fireTrackerEvent("Search");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListsChanged() {
        // refresh list adapter
        mListsAdapter.onListsChanged();
        // update tabs
        mTabs.setViewPager(mPager);
    }

    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
