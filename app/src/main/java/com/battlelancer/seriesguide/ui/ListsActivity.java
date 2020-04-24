
package com.battlelancer.seriesguide.ui;

import static com.battlelancer.seriesguide.ui.lists.ListsDistillationSettings.ListsSortOrder;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.lists.AddListDialogFragment;
import com.battlelancer.seriesguide.ui.lists.ListManageDialogFragment;
import com.battlelancer.seriesguide.ui.lists.ListsActivityViewModel;
import com.battlelancer.seriesguide.ui.lists.ListsDistillationSettings;
import com.battlelancer.seriesguide.ui.lists.ListsPagerAdapter;
import com.battlelancer.seriesguide.ui.lists.ListsReorderDialogFragment;
import com.battlelancer.seriesguide.ui.lists.ListsTools;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Hosts a view pager to display and manage lists of shows, seasons and episodes.
 */
public class ListsActivity extends BaseTopActivity {

    public static class ListsChangedEvent {
    }

    public static final int LISTS_LOADER_ID = 1;
    public static final int LISTS_REORDER_LOADER_ID = 2;

    @BindView(R.id.viewPagerTabs) ViewPager viewPager;
    @BindView(R.id.tabLayoutTabs) SlidingTabLayout tabs;
    private ListsPagerAdapter listsAdapter;

    private ListsActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs_drawer);
        setupActionBar();
        setupBottomNavigation(R.id.navigation_item_lists);

        viewModel = new ViewModelProvider(this).get(ListsActivityViewModel.class);

        setupViews(savedInstanceState);
        setupSyncProgressBar(R.id.progressBarTabs);

        LoaderManager.getInstance(this)
                .initLoader(LISTS_LOADER_ID, null, listsLoaderCallbacks);
    }

    private void setupViews(Bundle savedInstanceState) {
        ButterKnife.bind(this);

        listsAdapter = new ListsPagerAdapter(getSupportFragmentManager());

        viewPager.setAdapter(listsAdapter);

        tabs.setCustomTabView(R.layout.tabstrip_item_allcaps, R.id.textViewTabStripItem);
        tabs.setSelectedIndicatorColors(
                ThemeUtils.getColorFromAttribute(tabs.getContext(), R.attr.colorOnPrimarySurface));
        tabs.setOnTabClickListener(position -> {
            if (viewPager.getCurrentItem() == position) {
                showListManageDialog(position);
            }
        });
        tabs.setViewPager(viewPager);

        if (savedInstanceState == null) {
            viewPager.setCurrentItem(DisplaySettings.getLastListsTabPosition(this), false);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(DisplaySettings.KEY_LAST_ACTIVE_LISTS_TAB, viewPager.getCurrentItem())
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.lists_menu, menu);

        // set current sort order and check box states
        int sortOrderId = ListsDistillationSettings.getSortOrderId(this);
        MenuItem sortTitleItem = menu.findItem(R.id.menu_action_lists_sort_title);
        sortTitleItem.setTitle(R.string.action_shows_sort_title);
        MenuItem sortLatestItem = menu.findItem(R.id.menu_action_lists_sort_latest_episode);
        sortLatestItem.setTitle(R.string.action_shows_sort_latest_episode);
        MenuItem sortOldestItem = menu.findItem(R.id.menu_action_lists_sort_oldest_episode);
        sortOldestItem.setTitle(R.string.action_shows_sort_oldest_episode);
        MenuItem lastWatchedItem = menu.findItem(R.id.menu_action_lists_sort_last_watched);
        lastWatchedItem.setTitle(R.string.action_shows_sort_last_watched);
        MenuItem remainingItem = menu.findItem(R.id.menu_action_lists_sort_remaining);
        remainingItem.setTitle(R.string.action_shows_sort_remaining);
        if (sortOrderId == ListsSortOrder.TITLE_ALPHABETICAL_ID) {
            ViewTools.setMenuItemActiveString(sortTitleItem);
        } else if (sortOrderId == ListsSortOrder.LATEST_EPISODE_ID) {
            ViewTools.setMenuItemActiveString(sortLatestItem);
        } else if (sortOrderId == ListsSortOrder.OLDEST_EPISODE_ID) {
            ViewTools.setMenuItemActiveString(sortOldestItem);
        } else if (sortOrderId == ListsSortOrder.LAST_WATCHED_ID) {
            ViewTools.setMenuItemActiveString(lastWatchedItem);
        } else if (sortOrderId == ListsSortOrder.LEAST_REMAINING_EPISODES_ID) {
            ViewTools.setMenuItemActiveString(remainingItem);
        }

        menu.findItem(R.id.menu_action_lists_sort_ignore_articles)
                .setChecked(DisplaySettings.isSortOrderIgnoringArticles(this));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_lists_add) {
            AddListDialogFragment.show(getSupportFragmentManager());
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
            changeSortOrder(ListsSortOrder.TITLE_ALPHABETICAL_ID);
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_latest_episode) {
            changeSortOrder(ListsSortOrder.LATEST_EPISODE_ID);
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_oldest_episode) {
            changeSortOrder(ListsSortOrder.OLDEST_EPISODE_ID);
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_last_watched) {
            changeSortOrder(ListsSortOrder.LAST_WATCHED_ID);
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_remaining) {
            changeSortOrder(ListsSortOrder.LEAST_REMAINING_EPISODES_ID);
            return true;
        }
        if (itemId == R.id.menu_action_lists_sort_ignore_articles) {
            toggleSortIgnoreArticles();
            return true;
        }
        if (itemId == R.id.menu_action_lists_reorder) {
            DialogTools.safeShow(new ListsReorderDialogFragment(), getSupportFragmentManager(),
                    "listsReorderDialog");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showListManageDialog(int selectedListIndex) {
        String listId = listsAdapter.getListId(selectedListIndex);
        if (!TextUtils.isEmpty(listId)) {
            ListManageDialogFragment.show(listId, getSupportFragmentManager());
        }
    }

    @Override
    protected void onSelectedCurrentNavItem() {
        viewModel.scrollTabToTop(viewPager.getCurrentItem());
    }

    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ListsChangedEvent event) {
        LoaderManager.getInstance(this)
                .restartLoader(LISTS_LOADER_ID, null, listsLoaderCallbacks);
    }

    private void changeSortOrder(int sortOrderId) {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(ListsDistillationSettings.KEY_SORT_ORDER, sortOrderId)
                .apply();

        // refresh icon state
        supportInvalidateOptionsMenu();

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
        ListWidgetProvider.notifyDataChanged(this);

        // post event, so all active list fragments can react
        EventBus.getDefault().post(new ListsDistillationSettings.ListsSortOrderChangedEvent());
    }

    @Override
    protected View getSnackbarParentView() {
        return findViewById(R.id.rootLayoutTabs);
    }

    private LoaderManager.LoaderCallbacks<Cursor> listsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            // load lists, order by order number, then name
            return new CursorLoader(ListsActivity.this,
                    SeriesGuideContract.Lists.CONTENT_URI,
                    ListsPagerAdapter.ListsQuery.PROJECTION, null, null,
                    SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME);
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
            // precreate first list
            if (data != null && data.getCount() == 0) {
                String listName = getString(R.string.first_list);
                ListsTools.addList(ListsActivity.this, listName);
            }
            listsAdapter.swapCursor(data);
            // update tabs
            tabs.setViewPager(viewPager);
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Cursor> loader) {
            listsAdapter.swapCursor(null);
        }
    };
}
