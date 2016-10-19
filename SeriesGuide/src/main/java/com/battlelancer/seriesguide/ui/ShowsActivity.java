package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.api.Intents;
import com.battlelancer.seriesguide.billing.IabHelper;
import com.battlelancer.seriesguide.billing.amazon.AmazonIapManager;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.service.NotificationService;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.sync.AccountUtils;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.RemoveShowWorkerFragment;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Provides the apps main screen, displaying a list of shows and their next episodes.
 */
public class ShowsActivity extends BaseTopActivity implements
        AddShowDialogFragment.OnAddShowListener {

    protected static final String TAG = "Shows";

    public static final int SHOWS_LOADER_ID = 100;
    public static final int UPCOMING_LOADER_ID = 101;
    public static final int RECENT_LOADER_ID = 102;
    public static final int ADD_SHOW_LOADER_ID = 103;
    public static final int NOW_RECENTLY_LOADER_ID = 104;
    public static final int NOW_TODAY_LOADER_ID = 105;
    public static final int NOW_TRAKT_USER_LOADER_ID = 106;
    public static final int NOW_TRAKT_FRIENDS_LOADER_ID = 107;

    private IabHelper billingHelper;

    private ShowsTabPageAdapter tabsAdapter;
    private ViewPager viewPager;
    private ProgressDialog mProgressDialog;

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
        setupNavDrawer();

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
                        .putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeTvdbId);
            } else {
                // no such episode, offer to add show
                int showTvdbId = intent.getIntExtra(Intents.EXTRA_SHOW_TVDBID, 0);
                if (showTvdbId > 0) {
                    AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
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
                viewIntent = new Intent(this, OverviewActivity.class)
                        .putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, showTvdbId);
            } else {
                // no such show, offer to add it
                AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
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
        FloatingActionButton buttonAddShow = ButterKnife.findById(this, R.id.buttonShowsAdd);
        buttonAddShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShowsActivity.this, SearchActivity.class).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.SEARCH_TAB_POSITION));
            }
        });

        viewPager = (ViewPager) findViewById(R.id.viewPagerTabs);
        tabsAdapter = new ShowsTabPageAdapter(getSupportFragmentManager(), this, viewPager,
                (SlidingTabLayout) findViewById(R.id.tabLayoutTabs), buttonAddShow);

        // shows tab
        tabsAdapter.addTab(R.string.shows, ShowsFragment.class, null);

        // now tab
        tabsAdapter.addTab(R.string.now_tab, ShowsNowFragment.class, null);

        // upcoming tab
        final Bundle argsUpcoming = new Bundle();
        argsUpcoming.putString(CalendarFragment.InitBundle.TYPE,
                CalendarFragment.CalendarType.UPCOMING);
        argsUpcoming.putString(CalendarFragment.InitBundle.ANALYTICS_TAG, "Upcoming");
        argsUpcoming.putInt(CalendarFragment.InitBundle.LOADER_ID, UPCOMING_LOADER_ID);
        argsUpcoming.putInt(CalendarFragment.InitBundle.EMPTY_STRING_ID, R.string.noupcoming);
        tabsAdapter.addTab(R.string.upcoming, CalendarFragment.class, argsUpcoming);

        // recent tab
        final Bundle argsRecent = new Bundle();
        argsRecent
                .putString(CalendarFragment.InitBundle.TYPE, CalendarFragment.CalendarType.RECENT);
        argsRecent.putString(CalendarFragment.InitBundle.ANALYTICS_TAG, "Recent");
        argsRecent.putInt(CalendarFragment.InitBundle.LOADER_ID, RECENT_LOADER_ID);
        argsRecent.putInt(CalendarFragment.InitBundle.EMPTY_STRING_ID, R.string.norecent);
        tabsAdapter.addTab(R.string.recent, CalendarFragment.class, argsRecent);

        // display new tabs
        tabsAdapter.notifyTabsChanged();
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
        billingHelper = new IabHelper(this);
        billingHelper.startSetupAndQueryInventory();
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(R.id.navigation_item_shows);

        // check for running show removal worker
        Fragment f = getSupportFragmentManager().findFragmentByTag(RemoveShowWorkerFragment.TAG);
        if (f != null && !((RemoveShowWorkerFragment) f).isTaskFinished()) {
            showProgressDialog();
        }
        // now listen to events
        EventBus.getDefault().register(this);
    }

    @Override
    public void registerEventBus() {
        // do nothing, we handle that ourselves in onStart
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
        TaskManager.getInstance(this).tryNextEpisodeUpdateTask();
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
    protected void onStop() {
        super.onStop();

        // now prevent dialog from restoring itself (we would loose ref to it)
        hideProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingHelper != null) {
            billingHelper.dispose();
            billingHelper = null;
        }
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
            SgSyncAdapter.requestSyncImmediate(this, SgSyncAdapter.SyncType.DELTA, 0, true);
            Utils.trackAction(this, TAG, "Update (outdated)");

            return true;
        } else if (itemId == R.id.menu_fullupdate) {
            SgSyncAdapter.requestSyncImmediate(this, SgSyncAdapter.SyncType.FULL, 0, true);
            Utils.trackAction(this, TAG, "Update (all)");

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
        TaskManager.getInstance(this).performAddTask(SgApp.from(this), show);
    }

    /**
     * Called from {@link com.battlelancer.seriesguide.util.RemoveShowWorkerFragment}.
     */
    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RemoveShowWorkerFragment.OnRemovingShowEvent event) {
        showProgressDialog();
    }

    /**
     * Called from {@link com.battlelancer.seriesguide.util.RemoveShowWorkerFragment}.
     */
    @SuppressWarnings("UnusedParameters")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(RemoveShowWorkerFragment.OnShowRemovedEvent event) {
        hideProgressDialog();
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setCancelable(false);
        }
        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mProgressDialog = null;
    }

    /**
     * Runs any upgrades necessary if coming from earlier versions.
     */
    @SuppressLint("CommitPrefEdits")
    private void onUpgrade() {
        final int lastVersion = AppSettings.getLastVersionCode(this);
        final int currentVersion = BuildConfig.VERSION_CODE;

        if (lastVersion < currentVersion) {
            // user feedback about update
            Toast.makeText(getApplicationContext(), R.string.updated, Toast.LENGTH_LONG).show();

            /**
             * Run some required tasks after updating to certain versions.
             *
             * NOTE: see version codes for upgrade description.
             */
            if (lastVersion < SgApp.RELEASE_VERSION_12_BETA5) {
                // flag all episodes as outdated
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Episodes.LAST_EDITED, 0);
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
                TraktSettings.resetMoviesLastActivity(this);
            }
            if (lastVersion < SgApp.RELEASE_VERSION_26_BETA3) {
                // flag all shows outdated so delta sync will pick up, if full sync gets aborted
                scheduleAllShowsUpdate();
                // force a sync
                SgSyncAdapter.requestSyncImmediate(this, SgSyncAdapter.SyncType.FULL, 0, true);
            }

            // set this as lastVersion
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
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
