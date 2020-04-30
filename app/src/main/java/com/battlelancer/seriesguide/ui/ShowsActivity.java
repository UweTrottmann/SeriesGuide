package com.battlelancer.seriesguide.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.api.Intents;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.sync.AccountUtils;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.ui.episodes.EpisodeTools;
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity;
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.ui.shows.CalendarFragment2;
import com.battlelancer.seriesguide.ui.shows.ShowsActivityViewModel;
import com.battlelancer.seriesguide.ui.shows.ShowsFragment;
import com.battlelancer.seriesguide.ui.shows.ShowsNowFragment;
import com.battlelancer.seriesguide.util.ActivityTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.uwetrottmann.seriesguide.billing.BillingViewModel;
import com.uwetrottmann.seriesguide.widgets.SlidingTabLayout;

/**
 * Provides the apps main screen, displaying a list of shows and their next episodes.
 */
public class ShowsActivity extends BaseTopActivity implements
        AddShowDialogFragment.OnAddShowListener {

    public static final int SHOWS_LOADER_ID = 100;
    public static final int UPCOMING_LOADER_ID = 101;
    public static final int RECENT_LOADER_ID = 102;
    public static final int ADD_SHOW_LOADER_ID = 103;
    public static final int NOW_RECENTLY_LOADER_ID = 104;
    public static final int NOW_TRAKT_USER_LOADER_ID = 106;
    public static final int NOW_TRAKT_FRIENDS_LOADER_ID = 107;

    private ShowsTabPageAdapter tabsAdapter;
    private ViewPager viewPager;

    private ShowsActivityViewModel viewModel;

    @SuppressWarnings("unused")
    public interface InitBundle {

        String SELECTED_TAB = "selectedtab";

        int INDEX_TAB_SHOWS = 0;
        int INDEX_TAB_NOW = 1;
        int INDEX_TAB_UPCOMING = 2;
        int INDEX_TAB_RECENT = 3;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shows);
        setupActionBar();
        setupBottomNavigation(R.id.navigation_item_shows);

        // Set up a sync account if needed
        if (!AccountUtils.isAccountExists(this)) {
            AccountUtils.createAccount(this);
        }

        onUpgrade();

        // may launch from a notification, then set last cleared time
        NotificationService.handleDeleteIntent(this, getIntent());

        // handle implicit intents from other apps
        if (handleViewIntents(getIntent())) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(ShowsActivityViewModel.class);

        // setup all the views!
        setupViews();
        setupSyncProgressBar(R.id.progressBarTabs);
        setInitialTab(getIntent().getExtras());

        // query for in-app purchases
        if (Utils.isAmazonVersion()) {
            // setup Amazon IAP
            AmazonIapManager.setup(this);
        } else {
            // setup Google IAP
            checkGooglePlayPurchase();
        }
    }

    /**
     * Handles further behavior, if this activity was launched through one of the {@link Intents}
     * action filters defined in the manifest.
     *
     * @return true if a show or episode is viewed directly and this activity should finish.
     */
    private boolean handleViewIntents(Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return false;
        }

        Intent viewIntent = null;

        // view an episode
        if (Intents.ACTION_VIEW_EPISODE.equals(action)) {
            int episodeTvdbId = intent.getIntExtra(Intents.EXTRA_EPISODE_TVDBID, 0);
            if (episodeTvdbId > 0 && EpisodeTools.isEpisodeExists(this, episodeTvdbId)) {
                // episode exists, display it
                viewIntent = new Intent(this, EpisodesActivity.class)
                        .putExtra(EpisodesActivity.EXTRA_EPISODE_TVDBID, episodeTvdbId);
            } else {
                // no such episode, offer to add show
                int showTvdbId = intent.getIntExtra(Intents.EXTRA_SHOW_TVDBID, 0);
                if (showTvdbId > 0) {
                    AddShowDialogFragment.show(this, getSupportFragmentManager(),
                            showTvdbId);
                }
            }
        }
        // view a show
        else if (Intents.ACTION_VIEW_SHOW.equals(action)) {
            int showTvdbId = intent.getIntExtra(Intents.EXTRA_SHOW_TVDBID, 0);
            if (showTvdbId <= 0) {
                return false;
            }
            if (DBUtils.isShowExists(this, showTvdbId)) {
                // show exists, display it
                viewIntent = OverviewActivity.intentShow(this, showTvdbId);
            } else {
                // no such show, offer to add it
                AddShowDialogFragment.show(this, getSupportFragmentManager(),
                        showTvdbId);
            }
        }

        if (viewIntent != null) {
            startActivity(viewIntent);
            return true;
        }

        return false;
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        setTitle(R.string.shows);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.shows);
        }
    }

    private void setupViews() {
        // setup floating action button for adding shows
        FloatingActionButton buttonAddShow = findViewById(R.id.buttonShowsAdd);
        buttonAddShow.setOnClickListener(
                v -> startActivity(new Intent(ShowsActivity.this, SearchActivity.class)
                        .putExtra(SearchActivity.EXTRA_DEFAULT_TAB,
                                SearchActivity.TAB_POSITION_SEARCH)));

        viewPager = findViewById(R.id.viewPagerTabs);
        SlidingTabLayout tabs = findViewById(R.id.tabLayoutTabs);
        tabs.setOnTabClickListener(position -> {
            if (viewPager.getCurrentItem() == position) {
                scrollSelectedTabToTop();
            }
        });
        tabsAdapter = new ShowsTabPageAdapter(getSupportFragmentManager(), this, viewPager,
                tabs, buttonAddShow);

        // shows tab
        tabsAdapter.addTab(R.string.shows, ShowsFragment.class, null);

        // history tab
        tabsAdapter.addTab(R.string.user_stream, ShowsNowFragment.class, null);

        // upcoming tab
        final Bundle argsUpcoming = new Bundle();
        argsUpcoming.putInt(CalendarFragment2.ARG_CALENDAR_TYPE, CalendarFragment2.CalendarType.UPCOMING.getId());
        tabsAdapter.addTab(R.string.upcoming, CalendarFragment2.class, argsUpcoming);

        // recent tab
        final Bundle argsRecent = new Bundle();
        argsRecent.putInt(CalendarFragment2.ARG_CALENDAR_TYPE, CalendarFragment2.CalendarType.RECENT.getId());
        tabsAdapter.addTab(R.string.recent, CalendarFragment2.class, argsRecent);

        // display new tabs
        tabsAdapter.notifyTabsChanged();
    }

    private void scrollSelectedTabToTop() {
        viewModel.scrollTabToTop(viewPager.getCurrentItem());
    }

    @Override
    protected void onSelectedCurrentNavItem() {
        scrollSelectedTabToTop();
    }

    /**
     * Tries to restore the current tab from given intent extras. If that fails, uses the last known
     * selected tab. If that fails also, defaults to the first tab.
     */
    private void setInitialTab(Bundle intentExtras) {
        int selection;
        if (intentExtras != null) {
            selection = intentExtras.getInt(InitBundle.SELECTED_TAB,
                    DisplaySettings.getLastShowsTabPosition(this));
        } else {
            // use last saved selection
            selection = DisplaySettings.getLastShowsTabPosition(this);
        }

        // never select a non-existent tab
        if (selection > tabsAdapter.getCount() - 1) {
            selection = 0;
        }

        viewPager.setCurrentItem(selection);
    }

    private void checkGooglePlayPurchase() {
        if (Utils.hasXpass(this)) {
            return;
        }
        // Automatically starts checking all access status.
        // Ends connection if activity is finished (and was not ended elsewhere already).
        BillingViewModel billingViewModel = new ViewModelProvider(this).get(BillingViewModel.class);
        billingViewModel.getEntitlementRevokedEvent()
                .observe(this, aVoid -> BillingActivity.showExpiredNotification(this));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // handle intents that just want to view a specific show/episode
        if (!handleViewIntents(intent)) {
            // if no special intent, restore the last selected tab
            setInitialTab(intent.getExtras());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // prefs might have changed, update menus
        supportInvalidateOptionsMenu();

        if (Utils.isAmazonVersion()) {
            // update Amazon IAP
            AmazonIapManager.get().activate();
            AmazonIapManager.get().requestUserDataAndPurchaseUpdates();
            AmazonIapManager.get().validateSupporterState(this);
        }

        // update next episodes
        TaskManager.getInstance().tryNextEpisodeUpdateTask(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Utils.isAmazonVersion()) {
            // pause Amazon IAP
            AmazonIapManager.get().deactivate();
        }

        // save selected tab index
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putInt(DisplaySettings.KEY_LAST_ACTIVE_SHOWS_TAB, viewPager.getCurrentItem())
                .apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.seriesguide_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (itemId == R.id.menu_update) {
            SgSyncAdapter.requestSyncDeltaImmediate(this, true);
            return true;
        } else if (itemId == R.id.menu_fullupdate) {
            SgSyncAdapter.requestSyncFullImmediate(this, true);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // prevent navigating to top activity as this is the top activity
        return keyCode == KeyEvent.KEYCODE_BACK;
    }

    /**
     * Called if the user adds a show from a trakt stream fragment.
     */
    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance().performAddTask(this, show);
    }

    /**
     * Runs any upgrades necessary if coming from earlier versions.
     */
    private void onUpgrade() {
        final int lastVersion = AppSettings.getLastVersionCode(this);
        final int currentVersion = BuildConfig.VERSION_CODE;

        if (lastVersion < currentVersion) {
            // Let the user know the app has updated.
            Snackbar.make(getSnackbarParentView(), R.string.updated, Snackbar.LENGTH_LONG)
                    .setAction(
                            R.string.updated_details,
                            v -> Utils.launchWebsite(
                                    ShowsActivity.this,
                                    getString(R.string.url_release_notes)
                            )
                    )
                    .show();

            // Run some required tasks after updating to certain versions.
            // NOTE: see version codes for upgrade description.
            if (lastVersion < SgApp.RELEASE_VERSION_12_BETA5) {
                // flag all episodes as outdated
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Episodes.LAST_UPDATED, 0);
                getContentResolver().update(SeriesGuideContract.Episodes.CONTENT_URI, values, null,
                        null);
                // sync is triggered in last condition
                // (if we are in here we will definitely hit the ones below)
            }
            if (lastVersion < SgApp.RELEASE_VERSION_16_BETA1) {
                Utils.clearLegacyExternalFileCache(this);
            }
            if (lastVersion < SgApp.RELEASE_VERSION_23_BETA4) {
                // make next trakt sync download watched movies
                TraktSettings.resetMoviesLastWatchedAt(this);
            }
            if (lastVersion < SgApp.RELEASE_VERSION_26_BETA3) {
                // flag all shows outdated so delta sync will pick up, if full sync gets aborted
                scheduleAllShowsUpdate();
                // force a sync
                SgSyncAdapter.requestSyncFullImmediate(this, true);
            }
            if (lastVersion < SgApp.RELEASE_VERSION_34_BETA4) {
                ActivityTools.populateShowsLastWatchedTime(this);
            }
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            if (lastVersion < SgApp.RELEASE_VERSION_36_BETA2) {
                // used account name to determine sign-in state before switch to Google Sign-In
                if (!TextUtils.isEmpty(HexagonSettings.getAccountName(this))) {
                    // tell users to sign in again
                    editor.putBoolean(HexagonSettings.KEY_SHOULD_VALIDATE_ACCOUNT, true);
                }
            }
            if (lastVersion < SgApp.RELEASE_VERSION_40_BETA4) {
                ExtensionManager.get(this).setDefaultEnabledExtensions(this);
            }
            if (lastVersion < SgApp.RELEASE_VERSION_40_BETA6) {
                // cancel old widget alarm using implicit intent
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    PendingIntent pi = PendingIntent.getBroadcast(this,
                            ListWidgetProvider.REQUEST_CODE,
                            new Intent(ListWidgetProvider.ACTION_DATA_CHANGED), 0);
                    am.cancel(pi);
                }
                // new alarm is set automatically as upgrading causes app widgets to update
            }
            if (lastVersion != SgApp.RELEASE_VERSION_50_1
                    && lastVersion < SgApp.RELEASE_VERSION_51_BETA4) {
                // Movies were not added in all cases when syncing, so ensure they are now.
                TraktSettings.resetMoviesLastWatchedAt(this);
                HexagonSettings.resetSyncState(this);
            }

            // set this as lastVersion
            editor.putInt(AppSettings.KEY_VERSION, currentVersion);
            editor.apply();
        }
    }

    private void scheduleAllShowsUpdate() {
        // force update of all shows
        ContentValues values = new ContentValues();
        values.put(Shows.LASTUPDATED, 0);
        getContentResolver().update(Shows.CONTENT_URI, values, null, null);
    }

    @Override
    protected View getSnackbarParentView() {
        return findViewById(R.id.rootLayoutShows);
    }

    /**
     * Special {@link TabStripAdapter} which saves the currently selected page to preferences, so we
     * can restore it when the user comes back later.
     */
    public static class ShowsTabPageAdapter extends TabStripAdapter
            implements ViewPager.OnPageChangeListener {

        private final FloatingActionButton floatingActionButton;

        public ShowsTabPageAdapter(FragmentManager fm, Context context, ViewPager pager,
                SlidingTabLayout tabs, FloatingActionButton floatingActionButton) {
            super(fm, context, pager, tabs);
            this.floatingActionButton = floatingActionButton;
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
            // only display add show button on Shows tab
            if (position == InitBundle.INDEX_TAB_SHOWS) {
                floatingActionButton.show();
            } else {
                floatingActionButton.hide();
            }
        }
    }
}
