package com.battlelancer.seriesguide.ui;

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
import org.greenrobot.eventbus.EventBus;

/**
 * Movie section of the app, displays various movie tabs.
 */
public class MoviesActivity extends BaseTopActivity {

    public class MoviesTabClickEvent {
        public final int position;
        public final boolean showingNowTab;

        public MoviesTabClickEvent(int position, boolean showingNowTab) {
            this.position = position;
            this.showingNowTab = showingNowTab;
        }
    }

    public static final int SEARCH_LOADER_ID = 100;
    public static final int NOW_TRAKT_USER_LOADER_ID = 101;
    public static final int NOW_TRAKT_FRIENDS_LOADER_ID = 102;
    public static final int WATCHLIST_LOADER_ID = 103;
    public static final int COLLECTION_LOADER_ID = 104;

    public static final int TAB_POSITION_SEARCH = 0;
    public static final int TAB_POSITION_WATCHLIST_DEFAULT = 1;
    public static final int TAB_POSITION_COLLECTION_DEFAULT = 2;
    public static final int TAB_POSITION_NOW = 1;
    public static final int TAB_POSITION_WATCHLIST_WITH_NOW = 2;
    public static final int TAB_POSITION_COLLECTION_WITH_NOW = 3;
    private static final int TAB_COUNT_WITH_TRAKT = 4;

    @BindView(R.id.viewPagerTabs) ViewPager viewPager;
    @BindView(R.id.tabLayoutTabs) SlidingTabLayout tabs;
    private TabStripAdapter tabsAdapter;
    private boolean showNowTab;

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

        // tabs
        showNowTab = TraktCredentials.get(this).hasCredentials();
        tabs.setOnTabClickListener(new SlidingTabLayout.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                if (viewPager.getCurrentItem() == position) {
                    EventBus.getDefault().post(new MoviesTabClickEvent(position, showNowTab));
                }
            }
        });
        tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this, viewPager, tabs);
        // search
        tabsAdapter.addTab(R.string.search, MoviesSearchFragment.class, null);
        // trakt-only tabs should only be visible if connected
        if (showNowTab) {
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
        showNowTab = TraktCredentials.get(this).hasCredentials();
        if (showNowTab && currentTabCount != TAB_COUNT_WITH_TRAKT) {
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
