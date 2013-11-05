/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.billing.BillingActivity;
import com.battlelancer.seriesguide.billing.IabHelper;
import com.battlelancer.seriesguide.billing.IabResult;
import com.battlelancer.seriesguide.billing.Inventory;
import com.battlelancer.seriesguide.migration.MigrationActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.AppSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.sync.SyncUtils;
import com.battlelancer.seriesguide.ui.FirstRunFragment.OnFirstRunDismissedListener;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.BuildConfig;
import com.uwetrottmann.seriesguide.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides the apps main screen, displaying a list of shows and their next episodes.
 */
public class ShowsActivity extends BaseTopShowsActivity implements OnFirstRunDismissedListener {

    protected static final String TAG = "Shows";

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_INCOMPLETE = 104;

    // Background Task States
    private static final String STATE_ART_IN_PROGRESS = "seriesguide.art.inprogress";

    private static final String STATE_ART_PATHS = "seriesguide.art.paths";

    private static final String STATE_ART_INDEX = "seriesguide.art.index";

    private IabHelper mHelper;

    private Bundle mSavedState;

    private FetchPosterTask mArtTask;

    private Fragment mFragment;

    private ProgressBar mProgressBar;

    private Object mSyncObserverHandle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMenu().setContentView(R.layout.shows);

        // Set up a sync account if needed
        SyncUtils.createSyncAccount(this);

        onUpgrade();

        setUpActionBar();
        setupViews(savedInstanceState);

        // query in-app purchases (only if not already qualified)
        if (Utils.requiresPurchaseCheck(this)) {
            mHelper = new IabHelper(this, BillingActivity.getPublicKey(this));
            mHelper.enableDebugLogging(BuildConfig.DEBUG);

            Log.d(TAG, "Starting In-App Billing helper setup.");
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    Log.d(TAG, "Setup finished.");

                    if (!result.isSuccess()) {
                        // Oh noes, there was a problem. But do not go crazy.
                        disposeIabHelper();
                        return;
                    }

                    // Have we been disposed of in the meantime? If so, quit.
                    if (mHelper == null) {
                        return;
                    }

                    // Hooray, IAB is fully set up. Now, let's get an inventory
                    // of stuff we own.
                    Log.d(TAG, "Setup successful. Querying inventory.");
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                }
            });
        }
    }

    private void setUpActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    private void setupViews(Bundle savedInstanceState) {
        // setup fragments
        if (!FirstRunFragment.hasSeenFirstRunFragment(this)) {
            mFragment = FirstRunFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.shows_fragment, mFragment)
                    .commit();
        } else if (savedInstanceState == null) {
            onShowShowsFragment();
        } else {
            mFragment = getSupportFragmentManager().findFragmentById(R.id.shows_fragment);
        }

        // setup progress bar
        mProgressBar = (ProgressBar) findViewById(R.id.progressBarShows);
    }

    @Override
    protected void onStart() {
        super.onStart();

        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.updateLatestEpisodes(this);
        if (mSavedState != null) {
            restoreLocalState(mSavedState);
        }

        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onCancelTasks();
        disposeIabHelper();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        restoreLocalState(savedInstanceState);
        mSavedState = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveArtTask(outState);
        mSavedState = outState;
    }

    private void restoreLocalState(Bundle savedInstanceState) {
        restoreArtTask(savedInstanceState);
    }

    private void restoreArtTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_ART_IN_PROGRESS)) {
            ArrayList<String> paths = savedInstanceState.getStringArrayList(STATE_ART_PATHS);
            int index = savedInstanceState.getInt(STATE_ART_INDEX);

            if (paths != null) {
                mArtTask = (FetchPosterTask) new FetchPosterTask(paths, index).execute();
            }
        }
    }

    private void saveArtTask(Bundle outState) {
        final FetchPosterTask task = mArtTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            task.cancel(true);

            outState.putBoolean(STATE_ART_IN_PROGRESS, true);
            outState.putStringArrayList(STATE_ART_PATHS, task.mPaths);
            outState.putInt(STATE_ART_INDEX, task.mFetchCount.get());

            mArtTask = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.seriesguide_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // If the nav drawer is open, hide action items related to the content
        // view
        boolean isDrawerOpen = isMenuDrawerOpen();
        menu.findItem(R.id.menu_add_show).setVisible(!isDrawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add_show) {
            fireTrackerEvent("Add show");
            startActivity(new Intent(this, AddActivity.class));
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (itemId == R.id.menu_update) {
            SgSyncAdapter.requestSync(this, 0);
            fireTrackerEvent("Update (outdated)");

            return true;
        } else if (itemId == R.id.menu_fullupdate) {
            SgSyncAdapter.requestSync(this, -1);
            fireTrackerEvent("Update (all)");

            return true;
        } else if (itemId == R.id.menu_updateart) {
            fireTrackerEvent("Fetch posters");
            if (isArtTaskRunning()) {
                return true;
            }
            // already fail if there is no external storage
            if (!AndroidUtils.isExtStorageAvailable()) {
                Toast.makeText(this, getString(R.string.arttask_nosdcard), Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(this, getString(R.string.arttask_start), Toast.LENGTH_LONG).show();
                mArtTask = (FetchPosterTask) new FetchPosterTask().execute();
            }
            return true;
        } else if (itemId == R.id.menu_search) {
            fireTrackerEvent("Search");
            onSearchRequested();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        // always navigate back to the home activity
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // do nothing as we are already on top
            return true;
        }
        return false;
    }

    private class FetchPosterTask extends AsyncTask<Void, Void, Integer> {

        final AtomicInteger mFetchCount = new AtomicInteger();

        ArrayList<String> mPaths;

        protected FetchPosterTask() {
        }

        protected FetchPosterTask(ArrayList<String> paths, int index) {
            mPaths = paths;
            mFetchCount.set(index);
        }

        @Override
        protected void onPreExecute() {
            setProgressVisibility(true);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // fetch all available poster paths
            if (mPaths == null) {
                Cursor shows = getContentResolver().query(Shows.CONTENT_URI, new String[]{
                        Shows.POSTER
                }, null, null, null);

                // finish fast if there is no image to download
                if (shows.getCount() == 0) {
                    shows.close();
                    return UPDATE_SUCCESS;
                }

                mPaths = new ArrayList<String>();
                while (shows.moveToNext()) {
                    String imagePath = shows.getString(shows.getColumnIndexOrThrow(Shows.POSTER));
                    if (imagePath.length() != 0) {
                        mPaths.add(imagePath);
                    }
                }
                shows.close();
            }

            int resultCode = UPDATE_SUCCESS;
            final List<String> list = mPaths;
            final int count = list.size();
            final AtomicInteger fetchCount = mFetchCount;

            // try to fetch image for each path
            for (int i = fetchCount.get(); i < count; i++) {
                if (isCancelled()) {
                    // code doesn't matter as onPostExecute will not be called
                    return UPDATE_INCOMPLETE;
                }

                if (!TheTVDB.fetchArt(list.get(i), true, ShowsActivity.this)) {
                    resultCode = UPDATE_INCOMPLETE;
                }

                fetchCount.incrementAndGet();
            }

            getContentResolver().notifyChange(Shows.CONTENT_URI, null);

            return resultCode;
        }

        @Override
        protected void onPostExecute(Integer resultCode) {
            switch (resultCode) {
                case UPDATE_SUCCESS:
                    Toast.makeText(getApplicationContext(), getString(R.string.done),
                            Toast.LENGTH_SHORT).show();

                    EasyTracker.getInstance(getApplicationContext()).send(
                            MapBuilder.createEvent(TAG, "Poster Task", "Success", null).build()
                    );
                    break;
                case UPDATE_INCOMPLETE:
                    Toast.makeText(getApplicationContext(), getString(R.string.arttask_incomplete),
                            Toast.LENGTH_LONG).show();

                    EasyTracker.getInstance(getApplicationContext()).send(
                            MapBuilder.createEvent(TAG, "Poster Task", "Incomplete", null).build()
                    );
                    break;
            }

            setProgressVisibility(false);
        }

        @Override
        protected void onCancelled() {
            setProgressVisibility(false);
        }
    }

    private boolean isArtTaskRunning() {
        if (mArtTask != null && mArtTask.getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(this, getString(R.string.update_inprogress), Toast.LENGTH_LONG).show();
            return true;
        } else {
            return false;
        }
    }

    public void onCancelTasks() {
        if (mArtTask != null && mArtTask.getStatus() == AsyncTask.Status.RUNNING) {
            mArtTask.cancel(true);
            mArtTask = null;
        }
    }

    /**
     * Called once on activity creation to load initial settings and display one-time information
     * dialogs.
     */
    private void onUpgrade() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // between-version upgrade code
        try {
            final int lastVersion = AppSettings.getLastVersionCode(this);
            final int currentVersion = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA).versionCode;
            if (currentVersion > lastVersion) {
                Editor editor = prefs.edit();

                int VER_TRAKT_SEC_CHANGES;
                int VER_SUMMERTIME_FIX;
                int VER_HIGHRES_THUMBS;
                if (getPackageName().contains("beta")) {
                    VER_TRAKT_SEC_CHANGES = 131;
                    VER_SUMMERTIME_FIX = 155;
                    VER_HIGHRES_THUMBS = 177;
                } else if (getPackageName().contains("x")) {
                    VER_TRAKT_SEC_CHANGES = 129;
                    VER_SUMMERTIME_FIX = 136;
                    VER_HIGHRES_THUMBS = 141;
                } else {
                    VER_TRAKT_SEC_CHANGES = 129;
                    VER_SUMMERTIME_FIX = 136;
                    VER_HIGHRES_THUMBS = 140;
                }

                if (lastVersion < VER_TRAKT_SEC_CHANGES) {
                    // clear trakt credentials
                    editor.putString(SeriesGuidePreferences.KEY_TRAKTPWD, null);
                    editor.putString(SeriesGuidePreferences.KEY_SECURE, null);
                }
                if (lastVersion < VER_SUMMERTIME_FIX) {
                    scheduleAllShowsUpdate();
                }
                if (lastVersion < VER_HIGHRES_THUMBS
                        && getResources().getBoolean(R.bool.isLargeTablet)) {
                    // clear image cache
                    ImageProvider.getInstance(this).clearCache();
                    ImageProvider.getInstance(this).clearExternalStorageCache();
                    scheduleAllShowsUpdate();
                }

                // update notification
                Toast.makeText(this, R.string.updated, Toast.LENGTH_LONG).show();

                // set this as lastVersion
                editor.putInt(AppSettings.KEY_VERSION, currentVersion);

                editor.commit();
            }

        } catch (NameNotFoundException e) {
            // this should never happen
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
        onShowShowsFragment();
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }

    private void onShowShowsFragment() {
        mFragment = ShowsFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.shows_fragment, mFragment)
                .commit();
    }

    /**
     * Shows or hides a custom indeterminate progress indicator inside this activity layout.
     */
    public void setProgressVisibility(boolean isVisible) {
        mProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    // Listener that's called when we finish querying the items and
    // subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener
            = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");

            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null) {
                return;
            }

            if (result.isFailure()) {
                // ignore failures (maybe not, requires testing)
                disposeIabHelper();
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            BillingActivity.checkForSubscription(ShowsActivity.this, inventory);

            Log.d(TAG, "Inventory query finished.");
            disposeIabHelper();
        }
    };

    private void disposeIabHelper() {
        if (mHelper != null) {
            Log.d(TAG, "Disposing of IabHelper.");
            mHelper.dispose();
        }
        mHelper = null;
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
                    Account account = SyncUtils.getSyncAccount(ShowsActivity.this);
                    if (account == null) {
                        // GetAccount() returned an invalid value. This
                        // shouldn't happen.
                        setProgressVisibility(false);
                        return;
                    }

                    // Test the ContentResolver to see if the sync adapter is
                    // active or pending.
                    // Set the state of the refresh button accordingly.
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, SeriesGuideApplication.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, SeriesGuideApplication.CONTENT_AUTHORITY);
                    setProgressVisibility(syncActive || syncPending);
                }
            });
        }
    };
}
