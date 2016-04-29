
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

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.ListsPagerAdapter;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.ListsDistillationSettings;
import com.battlelancer.seriesguide.ui.dialogs.AddListDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListManageDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsReorderDialogFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import de.greenrobot.event.EventBus;

import static com.battlelancer.seriesguide.settings.ListsDistillationSettings.ListsSortOrder;

/**
 * Hosts a view pager to display and manage lists of shows, seasons and episodes.
 */
public class ListsActivity extends BaseTopActivity {

    public static class ListsChangedEvent {
    }

    public static final String TAG = "Lists";
    public static final int LISTS_REORDER_LOADER_ID = 1;

    private ListsPagerAdapter mListsAdapter;
    private ViewPager mPager;
    private SlidingTabLayout mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs_drawer);
        setupActionBar();
        setupNavDrawer();

        setupViews();
        setupSyncProgressBar(R.id.progressBarTabs);
    }

    private void setupViews() {
        mListsAdapter = new ListsPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.viewPagerTabs);
        mPager.setAdapter(mListsAdapter);

        mTabs = (SlidingTabLayout) findViewById(R.id.tabLayoutTabs);
        mTabs.setCustomTabView(R.layout.tabstrip_item_allcaps, R.id.textViewTabStripItem);
        mTabs.setSelectedIndicatorColors(ContextCompat.getColor(this, R.color.white));
        mTabs.setOnTabClickListener(new SlidingTabLayout.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                if (mPager.getCurrentItem() == position) {
                    showListManageDialog(position);
                }
            }
        });
        mTabs.setViewPager(mPager);
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(R.id.navigation_item_lists);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mListsAdapter.onCleanUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.lists_menu, menu);

        menu.findItem(R.id.menu_action_lists_sort_ignore_articles)
                .setChecked(DisplaySettings.isSortOrderIgnoringArticles(this));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_lists_add) {
            AddListDialogFragment.showAddListDialog(getSupportFragmentManager());
            Utils.trackAction(this, TAG, "Add list");
            return true;
        }
        if (itemId == R.id.menu_action_lists_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        }
        if (itemId == R.id.menu_action_lists_edit) {
            int selectedListIndex = mPager.getCurrentItem();
            showListManageDialog(selectedListIndex);
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_title) {
            if (ListsDistillationSettings.getSortOrderId(this)
                    == ListsSortOrder.TITLE_ALPHABETICAL_ID) {
                changeSortOrder(ListsSortOrder.TITLE_REVERSE_ALHPABETICAL_ID);
            } else {
                changeSortOrder(ListsSortOrder.TITLE_ALPHABETICAL_ID);
            }
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_episode) {
            if (ListsDistillationSettings.getSortOrderId(this)
                    == ListsSortOrder.NEWEST_EPISODE_FIRST_ID) {
                changeSortOrder(ListsSortOrder.OLDEST_EPISODE_FIRST_ID);
            } else {
                changeSortOrder(ListsSortOrder.NEWEST_EPISODE_FIRST_ID);
            }
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_ignore_articles) {
            toggleSortIgnoreArticles();
            return true;
        }
        if (itemId == R.id.menu_action_lists_reorder) {
            ListsReorderDialogFragment.show(getSupportFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showListManageDialog(int selectedListIndex) {
        String listId = mListsAdapter.getListId(selectedListIndex);
        ListManageDialogFragment.show(listId, getSupportFragmentManager());
    }

    @SuppressWarnings("UnusedParameters")
    public void onEventMainThread(ListsChangedEvent event) {
        onListsChanged();
    }

    private void onListsChanged() {
        // refresh list adapter
        mListsAdapter.onListsChanged();
        // update tabs
        mTabs.setViewPager(mPager);
    }

    private void changeSortOrder(int sortOrderId) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(ListsDistillationSettings.KEY_SORT_ORDER, sortOrderId)
                .apply();

        // post event, so all active list fragments can react
        EventBus.getDefault().post(new ListsDistillationSettings.ListsSortOrderChangedEvent());
    }

    private void toggleSortIgnoreArticles() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                        !DisplaySettings.isSortOrderIgnoringArticles(this))
                .apply();

        // refresh icon state
        supportInvalidateOptionsMenu();

        // refresh all list widgets
        ListWidgetProvider.notifyAllAppWidgetsViewDataChanged(this);

        // post event, so all active list fragments can react
        EventBus.getDefault().post(new ListsDistillationSettings.ListsSortOrderChangedEvent());
    }
}
