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

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants.ShowSorting;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.FirstRunFragment.OnFirstRunDismissedListener;
import com.battlelancer.seriesguide.ui.dialogs.ChangesDialogFragment;
import com.battlelancer.seriesguide.util.CompatActionBarNavHandler;
import com.battlelancer.seriesguide.util.CompatActionBarNavListener;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.UpdateTask;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides the apps main screen, displaying a list of shows and their next
 * episodes.
 */
public class ShowsActivity extends BaseTopActivity implements CompatActionBarNavListener,
        OnFirstRunDismissedListener {

    private static final String TAG = "Shows";

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_INCOMPLETE = 104;

    // Background Task States
    private static final String STATE_ART_IN_PROGRESS = "seriesguide.art.inprogress";

    private static final String STATE_ART_PATHS = "seriesguide.art.paths";

    private static final String STATE_ART_INDEX = "seriesguide.art.index";

    private Bundle mSavedState;

    private FetchPosterTask mArtTask;

    private boolean mIsLoaderStartAllowed;

    private Fragment mFragment;

    /**
     * Google Analytics helper method for easy event tracking.
     */
    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent(TAG, "Click", label, (long) 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shows);

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        updatePreferences(prefs);

        // setup fragments
        if (!FirstRunFragment.hasSeenFirstRunFragment(this)) {
            mFragment = FirstRunFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.shows_fragment, mFragment)
                    .commit();
        } else if (savedInstanceState == null) {
            mFragment = ShowsFragment.newInstance();
            getSupportFragmentManager().beginTransaction().replace(R.id.shows_fragment, mFragment)
                    .commit();
        }

        // set up action bar
        setUpActionBar(prefs);

    }

    private void setUpActionBar(SharedPreferences prefs) {
        mIsLoaderStartAllowed = false;

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setIcon(R.drawable.ic_action_menu);

        /* setup navigation */
        CompatActionBarNavHandler handler = new CompatActionBarNavHandler(this);
        if (getResources().getBoolean(R.bool.isLargeTablet)) {
            /* use tabs */
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            final String[] categories = getResources().getStringArray(R.array.showfilter_list);
            for (String category : categories) {
                actionBar.addTab(actionBar.newTab().setText(category).setTabListener(handler));
            }
        } else {
            /* use list (spinner) (! use different layouts for ABS) */
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            ArrayAdapter<CharSequence> mActionBarList = ArrayAdapter.createFromResource(this,
                    R.array.showfilter_list, R.layout.sherlock_spinner_item);
            mActionBarList.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
            actionBar.setListNavigationCallbacks(mActionBarList, handler);
        }

        // try to restore previously set show filter
        int navSelection = prefs.getInt(SeriesGuidePreferences.KEY_SHOWFILTER, 0);
        if (navSelection < 0 || navSelection > 3) {
            navSelection = 0;
        }
        if (getSupportActionBar().getSelectedNavigationIndex() != navSelection) {
            getSupportActionBar().setSelectedNavigationItem(navSelection);
        }

        // prevent the onNavigationItemSelected listener from reacting
        mIsLoaderStartAllowed = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.updateLatestEpisodes(this);
        if (mSavedState != null) {
            restoreLocalState(mSavedState);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        onCancelTasks();
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
                EasyTracker.getTracker().trackEvent(TAG, "Task Lifecycle", "Art Task Restored",
                        (long) 0);
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

            EasyTracker.getTracker().trackEvent(TAG, "Task Lifecycle", "Art Task Saved", (long) 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.seriesguide_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        MenuItem sortMenu = menu.findItem(R.id.menu_showsortby);
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            final CharSequence[] items = getResources().getStringArray(R.array.shsorting);
            ShowSorting sorting = ShowSorting
                    .fromValue(prefs.getString(
                            SeriesGuidePreferences.KEY_SHOW_SORT_ORDER,
                            ShowSorting.FAVORITES_FIRST.value()));
            sortMenu.setTitle(
                    getString(R.string.sort) + ": " + items[sorting.index()]);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_update) {
            performUpdateTask(false, null);
            fireTrackerEvent("Update");
            return true;
        } else if (itemId == R.id.menu_updateart) {
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
            fireTrackerEvent("Fetch missing posters");
            return true;
        } else if (itemId == R.id.menu_fullupdate) {
            performUpdateTask(true, null);
            fireTrackerEvent("Full Update");
            return true;
        } else if (itemId == R.id.menu_showsortby) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            ShowSorting sorting = ShowSorting
                    .fromValue(prefs.getString(
                            SeriesGuidePreferences.KEY_SHOW_SORT_ORDER,
                            ShowSorting.FAVORITES_FIRST.value()));
            ShowsFragment.showSortDialog(getSupportFragmentManager(), sorting);
            fireTrackerEvent("Sort shows");
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

    protected void performUpdateTask(boolean isFullUpdate, String showId) {
        int messageId;
        UpdateTask task;
        if (isFullUpdate) {
            messageId = R.string.update_full;
            task = (UpdateTask) new UpdateTask(true, this);
        } else {
            if (showId == null) {
                // (delta) update all shows
                messageId = R.string.update_delta;
                task = (UpdateTask) new UpdateTask(false, this);
            } else {
                // update a single show
                messageId = R.string.update_single;
                task = (UpdateTask) new UpdateTask(new String[] {
                        showId
                }, 0, "", this);
            }
        }
        TaskManager.getInstance(this).tryUpdateTask(task, true, messageId);
    }

    private class FetchPosterTask extends AsyncTask<Void, Void, Integer> {
        final AtomicInteger mFetchCount = new AtomicInteger();

        ArrayList<String> mPaths;

        private View mProgressOverlay;

        protected FetchPosterTask() {
        }

        protected FetchPosterTask(ArrayList<String> paths, int index) {
            mPaths = paths;
            mFetchCount.set(index);
        }

        @Override
        protected void onPreExecute() {
            // see if we already inflated the progress overlay
            mProgressOverlay = findViewById(R.id.overlay_update);
            if (mProgressOverlay == null) {
                mProgressOverlay = ((ViewStub) findViewById(R.id.stub_update)).inflate();
            }
            showOverlay(mProgressOverlay);
            // setup the progress overlay
            TextView mUpdateStatus = (TextView) mProgressOverlay
                    .findViewById(R.id.textViewUpdateStatus);
            mUpdateStatus.setText("");

            ProgressBar updateProgress = (ProgressBar) mProgressOverlay
                    .findViewById(R.id.ProgressBarShowListDet);
            updateProgress.setIndeterminate(true);

            View cancelButton = mProgressOverlay.findViewById(R.id.overlayCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onCancelTasks();
                }
            });
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // fetch all available poster paths
            if (mPaths == null) {
                Cursor shows = getContentResolver().query(Shows.CONTENT_URI, new String[] {
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
                    EasyTracker.getTracker().trackEvent(TAG, "Fetch missing posters", "Success",
                            (long) 0);

                    Toast.makeText(getApplicationContext(), getString(R.string.update_success),
                            Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_INCOMPLETE:
                    EasyTracker.getTracker().trackEvent(TAG, "Fetch missing posters", "Incomplete",
                            (long) 0);

                    Toast.makeText(getApplicationContext(), getString(R.string.arttask_incomplete),
                            Toast.LENGTH_LONG).show();
                    break;
            }

            hideOverlay(mProgressOverlay);
        }

        @Override
        protected void onCancelled() {
            hideOverlay(mProgressOverlay);
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

            EasyTracker.getTracker().trackEvent(TAG, "Task Lifecycle", "Art Task Canceled",
                    (long) 0);
        }
    }

    public void showOverlay(View overlay) {
        overlay.startAnimation(AnimationUtils
                .loadAnimation(getApplicationContext(), R.anim.fade_in));
        overlay.setVisibility(View.VISIBLE);
    }

    public void hideOverlay(View overlay) {
        overlay.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.fade_out));
        overlay.setVisibility(View.GONE);
    }

    /**
     * Called once on activity creation to load initial settings and display
     * one-time information dialogs.
     */
    private void updatePreferences(SharedPreferences prefs) {
        // between-version upgrade code
        final int lastVersion = prefs.getInt(SeriesGuidePreferences.KEY_VERSION, -1);
        try {
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
                    // clear trakt credetials
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

                // display extensive change dialog on beta channel
                if (getPackageName().contains("beta")) {
                    ChangesDialogFragment.show(getSupportFragmentManager());
                } else {
                    Toast.makeText(this, R.string.updated, Toast.LENGTH_LONG).show();
                }

                // set this as lastVersion
                editor.putInt(SeriesGuidePreferences.KEY_VERSION, currentVersion);

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
    public void onCategorySelected(int itemPosition) {
        // only react if everything is set up
        if (!mIsLoaderStartAllowed) {
            return;
        } else {
            // pass filter to show fragment
            ShowsFragment fragment;
            try {
                fragment = (ShowsFragment) mFragment;
                if (fragment != null) {
                    fragment.onFilterChanged(itemPosition);
                }
            } catch (ClassCastException e) {
            }

            // save the selected filter back to settings
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putInt(SeriesGuidePreferences.KEY_SHOWFILTER, itemPosition);
            editor.commit();
        }
    }

    @Override
    public void onFirstRunDismissed() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mFragment = ShowsFragment.newInstance();
        ft.replace(R.id.shows_fragment, mFragment).commit();
    }
}
