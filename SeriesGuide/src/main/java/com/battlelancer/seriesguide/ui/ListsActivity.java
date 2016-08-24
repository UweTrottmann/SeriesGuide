
package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import butterknife.BindView;
import butterknife.ButterKnife;
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

    @BindView(R.id.viewPagerTabs) ViewPager viewPager;
    @BindView(R.id.tabLayoutTabs) SlidingTabLayout tabs;
    private ListsPagerAdapter listsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs_drawer);
        setupActionBar();
        setupNavDrawer();

        setupViews(savedInstanceState);
        setupSyncProgressBar(R.id.progressBarTabs);
    }

    private void setupViews(Bundle savedInstanceState) {
        ButterKnife.bind(this);

        listsAdapter = new ListsPagerAdapter(getSupportFragmentManager(), this);

        viewPager.setAdapter(listsAdapter);

        tabs.setCustomTabView(R.layout.tabstrip_item_allcaps, R.id.textViewTabStripItem);
        tabs.setSelectedIndicatorColors(ContextCompat.getColor(this, R.color.white));
        tabs.setOnTabClickListener(new SlidingTabLayout.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                if (viewPager.getCurrentItem() == position) {
                    showListManageDialog(position);
                }
            }
        });
        tabs.setViewPager(viewPager);

        if (savedInstanceState == null) {
            viewPager.setCurrentItem(DisplaySettings.getLastListsTabPosition(this), false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(R.id.navigation_item_lists);
    }

    @Override
    protected void onPause() {
        super.onPause();

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(DisplaySettings.KEY_LAST_ACTIVE_LISTS_TAB, viewPager.getCurrentItem())
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        listsAdapter.onCleanUp();
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
            int selectedListIndex = viewPager.getCurrentItem();
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
        String listId = listsAdapter.getListId(selectedListIndex);
        ListManageDialogFragment.show(listId, getSupportFragmentManager());
    }

    @SuppressWarnings("UnusedParameters")
    public void onEventMainThread(ListsChangedEvent event) {
        onListsChanged();
    }

    private void onListsChanged() {
        // refresh list adapter
        listsAdapter.onListsChanged();
        // update tabs
        tabs.setViewPager(viewPager);
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
