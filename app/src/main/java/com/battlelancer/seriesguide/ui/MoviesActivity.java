package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.ui.movies.MoviesActivityViewModel;
import com.battlelancer.seriesguide.ui.movies.MoviesCollectionFragment;
import com.battlelancer.seriesguide.ui.movies.MoviesDiscoverFragment;
import com.battlelancer.seriesguide.ui.movies.MoviesNowFragment;
import com.battlelancer.seriesguide.ui.movies.MoviesSearchActivity;
import com.battlelancer.seriesguide.ui.movies.MoviesWatchListFragment;
import com.battlelancer.seriesguide.ui.movies.MoviesWatchedFragment;
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout;

/**
 * Movie section of the app, displays various movie tabs.
 */
public class MoviesActivity extends BaseTopActivity {

    public static final int SEARCH_LOADER_ID = 100;
    public static final int NOW_TRAKT_USER_LOADER_ID = 101;
    public static final int NOW_TRAKT_FRIENDS_LOADER_ID = 102;
    public static final int WATCHLIST_LOADER_ID = 103;
    public static final int COLLECTION_LOADER_ID = 104;

    public static final int TAB_POSITION_DISCOVER = 0;
    public static final int TAB_POSITION_WATCHLIST_DEFAULT = 1;
    public static final int TAB_POSITION_COLLECTION_DEFAULT = 2;
    public static final int TAB_POSITION_WATCHED_DEFAULT = 2;
    public static final int TAB_POSITION_NOW = 1;
    public static final int TAB_POSITION_WATCHLIST_WITH_NOW = 2;
    public static final int TAB_POSITION_COLLECTION_WITH_NOW = 3;
    public static final int TAB_POSITION_WATCHED_WITH_NOW = 4;
    private static final int TAB_COUNT_WITH_TRAKT = 5;

    @BindView(R.id.viewPagerTabs) ViewPager viewPager;
    @BindView(R.id.tabLayoutTabs) SlidingTabLayout tabs;
    private TabStripAdapter tabsAdapter;
    private boolean showNowTab;

    private MoviesActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs_drawer);
        setupActionBar();
        setupBottomNavigation(R.id.navigation_item_movies);

        viewModel = new ViewModelProvider(this).get(MoviesActivityViewModel.class);

        setupViews(savedInstanceState);
        setupSyncProgressBar(R.id.progressBarTabs);

        if (savedInstanceState != null) {
            postponeEnterTransition();
            // Allow the adapters to repopulate during the next layout pass
            // before starting the transition animation
            viewPager.post(this::startPostponedEnterTransition);
        }
    }

    private void setupViews(Bundle savedInstanceState) {
        ButterKnife.bind(this);

        // tabs
        showNowTab = TraktCredentials.get(this).hasCredentials();
        tabs.setOnTabClickListener(position -> {
            if (viewPager.getCurrentItem() == position) {
                scrollSelectedTabToTop();
            }
        });
        tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this, viewPager, tabs);
        // discover
        tabsAdapter.addTab(R.string.title_discover, MoviesDiscoverFragment.class, null);
        // trakt-only tabs should only be visible if connected
        if (showNowTab) {
            // history tab
            tabsAdapter.addTab(R.string.user_stream, MoviesNowFragment.class, null);
        }
        // watchlist
        tabsAdapter.addTab(R.string.movies_watchlist, MoviesWatchListFragment.class, null);
        // collection
        tabsAdapter.addTab(R.string.movies_collection, MoviesCollectionFragment.class, null);
        // watched
        tabsAdapter.addTab(R.string.movies_watched, MoviesWatchedFragment.class, null);

        tabsAdapter.notifyTabsChanged();
        if (savedInstanceState == null) {
            viewPager.setCurrentItem(DisplaySettings.getLastMoviesTabPosition(this), false);
        }
    }

    private void scrollSelectedTabToTop() {
        viewModel.scrollTabToTop(viewPager.getCurrentItem(), showNowTab);
    }

    @Override
    protected void onSelectedCurrentNavItem() {
        scrollSelectedTabToTop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Broken: tabs will update, but view pager does not use correct fragments.
        // add trakt-only tab if user just signed in
//        maybeAddNowTab();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(DisplaySettings.KEY_LAST_ACTIVE_MOVIES_TAB, viewPager.getCurrentItem())
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.movies_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_movies_search) {
            startActivity(new Intent(this, MoviesSearchActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void maybeAddNowTab() {
        int currentTabCount = tabsAdapter.getCount();
        showNowTab = TraktCredentials.get(this).hasCredentials();
        if (showNowTab && currentTabCount != TAB_COUNT_WITH_TRAKT) {
            tabsAdapter.addTab(
                    R.string.user_stream, MoviesNowFragment.class, null, TAB_POSITION_NOW
            );
            // update tabs
            tabsAdapter.notifyTabsChanged();
        }
    }

    @Override
    protected View getSnackbarParentView() {
        return findViewById(R.id.rootLayoutTabs);
    }
}
