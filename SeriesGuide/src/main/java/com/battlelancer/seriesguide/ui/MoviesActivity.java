package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;

/**
 * Movie section of the app, displays various movie tabs.
 */
public class MoviesActivity extends BaseTopActivity {

    public static final int SEARCH_LOADER_ID = 100;
    public static final int NOW_TRAKT_USER_LOADER_ID = 101;
    public static final int NOW_TRAKT_FRIENDS_LOADER_ID = 102;
    public static final int WATCHLIST_LOADER_ID = 103;
    public static final int COLLECTION_LOADER_ID = 104;

    private static final int TAB_COUNT_WITH_TRAKT = 4;

    @BindView(R.id.viewPagerTabs) ViewPager viewPager;
    @BindView(R.id.tabLayoutTabs) SlidingTabLayout tabs;
    private TabStripAdapter tabsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tabs_drawer);
        setupActionBar();
        setupNavDrawer();

        setupViews(savedInstanceState);
        setupSyncProgressBar(R.id.progressBarTabs);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (savedInstanceState != null) {
                postponeEnterTransition();
                viewPager.post(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        // Allow the adapters to repopulate during the next layout pass before starting the transition animation
                        startPostponedEnterTransition();
                    }
                });
            }
        }
    }

    private void setupViews(Bundle savedInstanceState) {
        ButterKnife.bind(this);
        // tabs
        tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this, viewPager, tabs);
        // search
        tabsAdapter.addTab(R.string.search, MoviesSearchFragment.class, null);
        // trakt-only tabs should only be visible if connected
        if (TraktCredentials.get(this).hasCredentials()) {
            // (what to watch) now
            tabsAdapter.addTab(R.string.now_tab, MoviesNowFragment.class, null);
        }
        // watchlist
        tabsAdapter.addTab(R.string.movies_watchlist, MoviesWatchListFragment.class, null);
        // collection
        tabsAdapter.addTab(R.string.movies_collection, MoviesCollectionFragment.class, null);

        tabsAdapter.notifyTabsChanged();
        if (savedInstanceState == null) {
            viewPager.setCurrentItem(DisplaySettings.getLastMoviesTabPosition(this), false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(R.id.navigation_item_movies);

        // add trakt-only tab if user just signed in
        maybeAddNowTab();
    }

    @Override
    protected void onResume() {
        super.onResume();

        supportInvalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(DisplaySettings.KEY_LAST_ACTIVE_MOVIES_TAB, viewPager.getCurrentItem())
                .apply();
    }

    private void maybeAddNowTab() {
        int currentTabCount = tabsAdapter.getCount();
        boolean shouldShowTraktTabs = TraktCredentials.get(this).hasCredentials();
        if (shouldShowTraktTabs && currentTabCount != TAB_COUNT_WITH_TRAKT) {
            tabsAdapter.addTab(R.string.now_tab, MoviesNowFragment.class, null);
            // update tabs
            tabsAdapter.notifyTabsChanged();
        }
    }

    @Override
    protected View getSnackbarParentView() {
        return findViewById(R.id.rootLayoutTabs);
    }
}
