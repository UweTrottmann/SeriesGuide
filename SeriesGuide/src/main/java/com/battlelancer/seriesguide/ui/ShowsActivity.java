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

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncStatusObserver;
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
import android.view.View;
import android.view.animation.AnimationUtils;
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
import com.battlelancer.seriesguide.settings.ActivitySettings;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.sync.AccountUtils;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.FirstRunFragment.OnFirstRunDismissedListener;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.ui.streams.UserEpisodeStreamFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.RemoveShowWorkerFragment;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import de.greenrobot.event.EventBus;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
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
    public static final int FRIENDS_LOADER_ID = 103;
    public static final int USER_LOADER_ID = 104;
    public static final int ADD_SHOW_LOADER_ID = 105;

    private static final int TAB_COUNT_WITH_TRAKT = 4;

    private IabHelper mBillingHelper;

    private SmoothProgressBar mProgressBar;

    private Object mSyncObserverHandle;

    private ShowsTabPageAdapter mTabsAdapter;

    private ViewPager mViewPager;

    private ProgressDialog mProgressDialog;

    public interface InitBundle {

        String SELECTED_TAB = "selectedtab";

        int INDEX_TAB_SHOWS = 0;
        int INDEX_TAB_UPCOMING = 1;
        int INDEX_TAB_RECENT = 2;
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
        if (handleViewIntents()) {
            finish();
            return;
        }

        // setup all the views!
        setupViews();
        setInitialTab(savedInstanceState, getIntent().getExtras());

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
                    SearchResult show = new SearchResult();
                    show.tvdbid = showTvdbId;
                    AddShowDialogFragment.showAddDialog(show, getSupportFragmentManager());
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
                SearchResult show = new SearchResult();
                show.tvdbid = showTvdbId;
                AddShowDialogFragment.showAddDialog(show, getSupportFragmentManager());
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
        mViewPager = (ViewPager) findViewById(R.id.pagerShows);

        mTabsAdapter = new ShowsTabPageAdapter(getSupportFragmentManager(),
                this, mViewPager, (SlidingTabLayout) findViewById(R.id.tabsShows));

        // shows tab (or first run fragment)
        if (!FirstRunFragment.hasSeenFirstRunFragment(this)) {
            mTabsAdapter.addTab(R.string.shows, FirstRunFragment.class, null);
        } else {
            mTabsAdapter.addTab(R.string.shows, ShowsFragment.class, null);
        }

        // upcoming tab
        final Bundle argsUpcoming = new Bundle();
        argsUpcoming.putString(ActivityFragment.InitBundle.TYPE,
                ActivityFragment.ActivityType.UPCOMING);
        argsUpcoming.putString(ActivityFragment.InitBundle.ANALYTICS_TAG, "Upcoming");
        argsUpcoming.putInt(ActivityFragment.InitBundle.LOADER_ID, UPCOMING_LOADER_ID);
        argsUpcoming.putInt(ActivityFragment.InitBundle.EMPTY_STRING_ID, R.string.noupcoming);
        mTabsAdapter.addTab(R.string.upcoming, ActivityFragment.class, argsUpcoming);

        // recent tab
        final Bundle argsRecent = new Bundle();
        argsRecent
                .putString(ActivityFragment.InitBundle.TYPE, ActivityFragment.ActivityType.RECENT);
        argsRecent.putString(ActivityFragment.InitBundle.ANALYTICS_TAG, "Recent");
        argsRecent.putInt(ActivityFragment.InitBundle.LOADER_ID, RECENT_LOADER_ID);
        argsRecent.putInt(ActivityFragment.InitBundle.EMPTY_STRING_ID, R.string.norecent);
        mTabsAdapter.addTab(R.string.recent, ActivityFragment.class, argsRecent);

        // trakt tabs only visible if connected
        if (TraktCredentials.get(this).hasCredentials()) {
            mTabsAdapter.addTab(R.string.user_stream, UserEpisodeStreamFragment.class, null);
        }

        // display new tabs
        mTabsAdapter.notifyTabsChanged();

        // progress bar
        mProgressBar = (SmoothProgressBar) findViewById(R.id.progressBarShows);
        mProgressBar.setVisibility(View.GONE);
    }

    /**
     * Tries to restore the current tab from the given state, if that fails from the given intent
     * extras. If that fails as well, uses the last known selected tab.
     */
    private void setInitialTab(Bundle savedInstanceState, Bundle intentExtras) {
        int selection;
        if (intentExtras != null) {
            selection = intentExtras.getInt(InitBundle.SELECTED_TAB,
                    ActivitySettings.getDefaultActivityTabPosition(this));
        } else {
            // use last saved selection
            selection = ActivitySettings.getDefaultActivityTabPosition(this);
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
        mBillingHelper = new IabHelper(this, BillingActivity.getPublicKey());
        mBillingHelper.enableDebugLogging(BuildConfig.DEBUG);
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

        // add trakt tabs if user just signed in
        maybeAddTraktTabs();

        // check for running show removal worker
        Fragment f = getSupportFragmentManager().findFragmentByTag(RemoveShowWorkerFragment.TAG);
        if (f != null && !((RemoveShowWorkerFragment) f).isTaskFinished()) {
            showProgressDialog();
        }
        // now listen to events
        EventBus.getDefault().register(this);
    }

    private void maybeAddTraktTabs() {
        int currentTabCount = mTabsAdapter.getCount();
        boolean shouldShowTraktTabs = TraktCredentials.get(this).hasCredentials();

        if (shouldShowTraktTabs && currentTabCount != TAB_COUNT_WITH_TRAKT) {
            mTabsAdapter.addTab(R.string.user_stream, UserEpisodeStreamFragment.class, null);
            // update tabs
            mTabsAdapter.notifyTabsChanged();
        }
    }

    @Override
    public void registerEventBus() {
        // do nothing, we handle that ourselves in onStart
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setInitialTab(null, intent.getExtras());
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
            AmazonIapManager.get().validateSubscription(this);
        }

        // update next episodes
        TaskManager.getInstance(this).tryNextEpisodeUpdateTask();

        // watch for sync state changes
        mSyncStatusObserver.onStatusChanged(0);
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (Utils.isAmazonVersion()) {
            // pause Amazon IAP
            AmazonIapManager.get().deactivate();
        }

        // stop listening to sync state changes
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
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

    /**
     * Shows or hides a custom indeterminate progress indicator inside this activity layout.
     */
    public void setProgressVisibility(boolean isVisible) {
        if (mProgressBar.getVisibility() == (isVisible ? View.VISIBLE : View.GONE)) {
            // already in desired state, avoid replaying animation
            return;
        }
        mProgressBar.startAnimation(AnimationUtils.loadAnimation(mProgressBar.getContext(),
                isVisible ? R.anim.fade_in : R.anim.fade_out));
        mProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
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
     * Create a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If a sync is active or pending, a progress bar is
     * shown.
     */
    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked with the sync adapter status changes. */
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(new Runnable() {
                /**
                 * The SyncAdapter runs on a background thread. To update the
                 * UI, onStatusChanged() runs on the UI thread.
                 */
                @Override
                public void run() {
                    Account account = AccountUtils.getAccount(ShowsActivity.this);
                    if (account == null) {
                        // no account setup
                        setProgressVisibility(false);
                        return;
                    }

                    // Test the ContentResolver to see if the sync adapter is active.
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, SeriesGuideApplication.CONTENT_AUTHORITY);
                    setProgressVisibility(syncActive);
                }
            });
        }
    };

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
            mPrefs.edit().putInt(ActivitySettings.KEY_ACTIVITYTAB, position).apply();
        }
    }
}
