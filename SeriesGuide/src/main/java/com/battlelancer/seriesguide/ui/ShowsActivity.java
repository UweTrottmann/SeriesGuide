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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.api.Intents;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.IabHelper;
import com.battlelancer.seriesguide.billing.IabResult;
import com.battlelancer.seriesguide.billing.Inventory;
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
import com.battlelancer.seriesguide.ui.FirstRunFragment.OnFirstRunDismissedListener;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.RemoveShowWorkerFragment;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import de.greenrobot.event.EventBus;
import timber.log.Timber;

/**
 * Provides the apps main screen, displaying a list of shows and their next episodes.
 */
public class ShowsActivity extends BaseTopActivity implements
        AddShowDialogFragment.OnAddShowListener, OnFirstRunDismissedListener {

    protected static final String TAG = "Shows";

    public static final int SHOWS_LOADER_ID = 100;
    public static final int UPCOMING_LOADER_ID = 101;
    public static final int RECENT_LOADER_ID = 102;
    public static final int ADD_SHOW_LOADER_ID = 103;
    public static final int NOW_RECENTLY_LOADER_ID = 104;
    public static final int NOW_TODAY_LOADER_ID = 105;
    public static final int NOW_TRAKT_USER_LOADER_ID = 106;
    public static final int NOW_TRAKT_FRIENDS_LOADER_ID = 107;

    private IabHelper mBillingHelper;

    private ShowsTabPageAdapter mTabsAdapter;

    private ViewPager mViewPager;

    private ProgressDialog mProgressDialog;

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
        setContentView(R.layout.activity_tabs_drawer);
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
        if (handleViewIntents()) {
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
    private boolean handleViewIntents() {
        String action = getIntent().getAction();
        if (TextUtils.isEmpty(action)) {
            return false;
        }

        Intent intent = null;

        // view an episode
        if (Intents.ACTION_VIEW_EPISODE.equals(action)) {
            int episodeTvdbId = getIntent().getIntExtra(Intents.EXTRA_EPISODE_TVDBID, 0);
            if (episodeTvdbId > 0 && EpisodeTools.isEpisodeExists(this, episodeTvdbId)) {
                // episode exists, display it
                intent = new Intent(this, EpisodesActivity.class)
                        .putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeTvdbId);
            } else {
                // no such episode, offer to add show
                int showTvdbId = getIntent().getIntExtra(Intents.EXTRA_SHOW_TVDBID, 0);
                if (showTvdbId > 0) {
                    AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
                }
            }
        }
        // view a show
        else if (Intents.ACTION_VIEW_SHOW.equals(action)) {
            int showTvdbId = getIntent().getIntExtra(Intents.EXTRA_SHOW_TVDBID, 0);
            if (showTvdbId <= 0) {
                return false;
            }
            if (DBUtils.isShowExists(this, showTvdbId)) {
                // show exists, display it
                intent = new Intent(this, OverviewActivity.class)
                        .putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, showTvdbId);
            } else {
                // no such show, offer to add it
                AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
            }
        }

        if (intent != null) {
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.shows);
    }

    private void setupViews() {
        mViewPager = (ViewPager) findViewById(R.id.viewPagerTabs);

        mTabsAdapter = new ShowsTabPageAdapter(getSupportFragmentManager(),
                this, mViewPager, (SlidingTabLayout) findViewById(R.id.tabLayoutTabs));

        // shows tab (or first run fragment)
        if (!FirstRunFragment.hasSeenFirstRunFragment(this)) {
            mTabsAdapter.addTab(R.string.shows, FirstRunFragment.class, null);
        } else {
            mTabsAdapter.addTab(R.string.shows, ShowsFragment.class, null);
        }

        // now tab
        mTabsAdapter.addTab(R.string.now_tab, ShowsNowFragment.class, null);

        // upcoming tab
        final Bundle argsUpcoming = new Bundle();
        argsUpcoming.putString(CalendarFragment.InitBundle.TYPE,
                CalendarFragment.CalendarType.UPCOMING);
        argsUpcoming.putString(CalendarFragment.InitBundle.ANALYTICS_TAG, "Upcoming");
        argsUpcoming.putInt(CalendarFragment.InitBundle.LOADER_ID, UPCOMING_LOADER_ID);
        argsUpcoming.putInt(CalendarFragment.InitBundle.EMPTY_STRING_ID, R.string.noupcoming);
        mTabsAdapter.addTab(R.string.upcoming, CalendarFragment.class, argsUpcoming);

        // recent tab
        final Bundle argsRecent = new Bundle();
        argsRecent
                .putString(CalendarFragment.InitBundle.TYPE, CalendarFragment.CalendarType.RECENT);
        argsRecent.putString(CalendarFragment.InitBundle.ANALYTICS_TAG, "Recent");
        argsRecent.putInt(CalendarFragment.InitBundle.LOADER_ID, RECENT_LOADER_ID);
        argsRecent.putInt(CalendarFragment.InitBundle.EMPTY_STRING_ID, R.string.norecent);
        mTabsAdapter.addTab(R.string.recent, CalendarFragment.class, argsRecent);

        // display new tabs
        mTabsAdapter.notifyTabsChanged();
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
        if (selection > mTabsAdapter.getCount() - 1) {
            selection = 0;
        }

        mViewPager.setCurrentItem(selection);
    }

    private void checkGooglePlayPurchase() {
        if (Utils.hasXpass(this)) {
            return;
        }
        mBillingHelper = new IabHelper(this);
        mBillingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (mBillingHelper == null) {
                    // disposed
                    return;
                }

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem. Try again next time.
                    disposeIabHelper();
                    return;
                }

                Timber.d("onIabSetupFinished: Successful. Querying inventory.");
                mBillingHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_SHOWS_POSITION);
        if (!AppSettings.hasSeenNavDrawer(this)) {
            // introduce the nav drawer
            openNavDrawer();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putBoolean(AppSettings.KEY_HAS_SEEN_NAV_DRAWER, true)
                    .apply();
        }

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

        setInitialTab(intent.getExtras());
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
        disposeIabHelper();
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
            fireTrackerEvent("Search");
            return true;
        } else if (itemId == R.id.menu_update) {
            SgSyncAdapter.requestSyncImmediate(this, SgSyncAdapter.SyncType.DELTA, 0, true);
            fireTrackerEvent("Update (outdated)");

            return true;
        } else if (itemId == R.id.menu_fullupdate) {
            SgSyncAdapter.requestSyncImmediate(this, SgSyncAdapter.SyncType.FULL, 0, true);
            fireTrackerEvent("Update (all)");

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
        TaskManager.getInstance(this).performAddTask(show);
    }

    /**
     * Called from {@link com.battlelancer.seriesguide.util.RemoveShowWorkerFragment}.
     */
    public void onEventMainThread(RemoveShowWorkerFragment.OnRemovingShowEvent event) {
        showProgressDialog();
    }

    /**
     * Called from {@link com.battlelancer.seriesguide.util.RemoveShowWorkerFragment}.
     */
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
            if (lastVersion < SeriesGuideApplication.RELEASE_VERSION_12_BETA5) {
                // flag all episodes as outdated
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Episodes.LAST_EDITED, 0);
                getContentResolver().update(SeriesGuideContract.Episodes.CONTENT_URI, values, null,
                        null);
                // sync is triggered in last condition
                // (if we are in here we will definitely hit the ones below)
            }
            if (lastVersion < SeriesGuideApplication.RELEASE_VERSION_16_BETA1) {
                Utils.clearLegacyExternalFileCache(this);
            }
            if (lastVersion < SeriesGuideApplication.RELEASE_VERSION_21) {
                // flag all shows outdated so delta sync will pick up, if full sync was aborted
                scheduleAllShowsUpdate();
                // force a sync
                SgSyncAdapter.requestSyncImmediate(this, SgSyncAdapter.SyncType.FULL, 0, true);
            }
            if (lastVersion < SeriesGuideApplication.RELEASE_VERSION_23_BETA4) {
                // make next trakt sync download watched movies
                TraktSettings.resetMoviesLastActivity(this);
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

    @Override
    public void onFirstRunDismissed() {
        // replace the first run fragment with a show fragment
        mTabsAdapter.updateTab(R.string.shows, ShowsFragment.class, null, 0);
        mTabsAdapter.notifyTabsChanged();
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }

    // Listener that's called when we finish querying the items and
    // subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener
            = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (mBillingHelper == null) {
                // disposed
                return;
            }

            if (result.isFailure()) {
                // do not care about failure, will try again next time
                disposeIabHelper();
                return;
            }

            BillingActivity.checkForSubscription(ShowsActivity.this, inventory);
            disposeIabHelper();
        }
    };

    private void disposeIabHelper() {
        Timber.i("Disposing of IabHelper.");
        if (mBillingHelper != null) {
            mBillingHelper.dispose();
        }
        mBillingHelper = null;
    }

    /**
     * Special {@link TabStripAdapter} which saves the currently selected page to preferences, so we
     * can restore it when the user comes back later.
     */
    public static class ShowsTabPageAdapter extends TabStripAdapter
            implements ViewPager.OnPageChangeListener {

        private SharedPreferences mPrefs;

        public ShowsTabPageAdapter(FragmentManager fm, Context context, ViewPager pager,
                SlidingTabLayout tabs) {
            super(fm, context, pager, tabs);
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
            tabs.setOnPageChangeListener(this);
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof FirstRunFragment) {
                return POSITION_NONE;
            } else {
                return super.getItemPosition(object);
            }
        }

        @Override
        public void onPageScrollStateChanged(int arg0) {
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
        }

        @Override
        public void onPageSelected(int position) {
            // save selected tab index
            mPrefs.edit().putInt(DisplaySettings.KEY_LAST_ACTIVE_SHOWS_TAB, position).apply();
        }
    }
}
