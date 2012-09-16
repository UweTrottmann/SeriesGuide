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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
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
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.dialogs.ChangesDialogFragment;
import com.battlelancer.seriesguide.util.CompatActionBarNavHandler;
import com.battlelancer.seriesguide.util.CompatActionBarNavListener;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.MultiPagerAdapter;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.UpdateTask;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.viewpagerindicator.TabPageIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides the apps main screen, displaying a list of shows and their next
 * episodes.
 */
public class ShowsActivity extends BaseActivity implements CompatActionBarNavListener {

    private static final String TAG = "Shows";

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_INCOMPLETE = 104;

    // Background Task States
    private static final String STATE_ART_IN_PROGRESS = "seriesguide.art.inprogress";

    private static final String STATE_ART_PATHS = "seriesguide.art.paths";

    private static final String STATE_ART_INDEX = "seriesguide.art.index";

    private static final int VER_TRAKT_SEC_CHANGES = 131;

    private static final int VER_SUMMERTIME_FIX = 155;

    private static final int VER_HIGHRES_THUMBS = 177;

    private static final int LIST_NAV_ITEM_POSITION = 4;

    private Bundle mSavedState;

    private FetchPosterTask mArtTask;

    private ViewPager mPager;

    private boolean mIsLoaderStartAllowed;

    private TabPageIndicator mIndicator;

    private ShowsPagerAdapter mShowsAdapter;

    private ListsPagerAdapter mListsAdapter;

    /**
     * Google Analytics helper method for easy event tracking.
     * 
     * @param label
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

        // set up adapters
        mShowsAdapter = new ShowsPagerAdapter(getSupportFragmentManager());
        mListsAdapter = new ListsPagerAdapter(getSupportFragmentManager(), this);

        // try to restore previously set show filter
        int navSelection = prefs.getInt(SeriesGuidePreferences.KEY_SHOWFILTER, 0);

        // set up action bar
        setUpActionBar(prefs, navSelection);

        // set up view pager
        mPager = (ViewPager) findViewById(R.id.pager);
        onChangePagerAdapter(navSelection);

        mIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        onDisplayTitleIndicator(navSelection);

        // FIXME force the options menu to be shown
        invalidateOptionsMenu();

        // TODO First run fragment
        // if (!FirstRunFragment.hasSeenFirstRunFragment(this)) {
        // mFragment = FirstRunFragment.newInstance();
        //
        // getSupportFragmentManager().beginTransaction().replace(R.id.shows_fragment,
        // mFragment)
        // .commit();
        // } else {
        // if (savedInstanceState == null) {
        // showShowsFragment();
        // }
        // }
    }

    private void setUpActionBar(final SharedPreferences prefs, int navSelection) {
        mIsLoaderStartAllowed = false;

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

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
            ArrayAdapter<CharSequence> mActionBarList = ArrayAdapter.createFromResource(
                    this, R.array.showfilter_list, R.layout.sherlock_spinner_item);
            mActionBarList.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
            actionBar.setListNavigationCallbacks(mActionBarList, handler);
        }

        actionBar.setSelectedNavigationItem(navSelection);

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
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_quickcheckin: {
                startActivity(new Intent(this, CheckinActivity.class));
                return true;
            }
            case R.id.menu_search:
                fireTrackerEvent("Search");

                onSearchRequested();
                return true;
            case R.id.menu_update:
                fireTrackerEvent("Update");

                performUpdateTask(false, null);
                return true;
            case R.id.menu_upcoming:
                startActivity(new Intent(this, UpcomingRecentActivity.class));
                return true;
            case R.id.menu_new_show:
                startActivity(new Intent(this, AddActivity.class));
                return true;
            case R.id.menu_updateart:
                fireTrackerEvent("Fetch missing posters");

                if (isArtTaskRunning()) {
                    return true;
                }

                // already fail if there is no external storage
                if (!AndroidUtils.isExtStorageAvailable()) {
                    Toast.makeText(this, getString(R.string.arttask_nosdcard), Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(this, getString(R.string.arttask_start), Toast.LENGTH_LONG)
                            .show();
                    mArtTask = (FetchPosterTask) new FetchPosterTask().execute();
                }
                return true;
            case R.id.menu_preferences:
                startActivity(new Intent(this, SeriesGuidePreferences.class));

                return true;
            case R.id.menu_fullupdate:
                fireTrackerEvent("Full Update");

                performUpdateTask(true, null);
                return true;
            case R.id.menu_feedback: {
                fireTrackerEvent("Feedback");

                final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
                intent.setType("plain/text");
                intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] {
                        SeriesGuidePreferences.SUPPORT_MAIL
                });
                intent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        "SeriesGuide " + Utils.getVersion(this) + " Feedback");
                intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                startActivity(Intent.createChooser(intent, "Send mail..."));

                return true;
            }
            case R.id.menu_help: {
                fireTrackerEvent("Help");

                Intent myIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse(SeriesGuidePreferences.HELP_URL));

                startActivity(myIntent);

                return true;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
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

                if (lastVersion < VER_TRAKT_SEC_CHANGES) {
                    // clear trakt credetials
                    editor.putString(SeriesGuidePreferences.KEY_TRAKTPWD, null);
                    editor.putString(SeriesGuidePreferences.KEY_SECURE, null);
                }
                if (lastVersion < VER_SUMMERTIME_FIX) {
                    scheduleAllShowsUpdate();
                }
                if (getResources().getBoolean(R.bool.isLargeTablet)
                        && lastVersion < VER_HIGHRES_THUMBS) {
                    // clear image cache
                    ImageProvider.getInstance(this).clearCache();
                    ImageProvider.getInstance(this).clearExternalStorageCache();
                    scheduleAllShowsUpdate();
                }

                // BETA warning dialog switch
                ChangesDialogFragment.show(getSupportFragmentManager());

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

    public static class ShowsPagerAdapter extends MultiPagerAdapter {

        public ShowsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return ShowsFragment.newInstance();
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "";
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof ShowsFragment) {
                return POSITION_UNCHANGED;
            } else {
                return POSITION_NONE;
            }
        }
    }

    /**
     * Returns {@link ListsFragment}s for every list in the database, makes sure
     * there is always at least one.
     */
    public static class ListsPagerAdapter extends MultiPagerAdapter {

        private Context mContext;

        private Cursor mLists;

        public ListsPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mContext = context;

            mLists = mContext.getContentResolver().query(Lists.CONTENT_URI, new String[] {
                    Lists.LIST_ID, Lists.NAME
            }, null, null, null);

            // precreate first list
            if (mLists != null && mLists.getCount() == 0) {
                String listName = mContext.getString(R.string.first_list);

                ContentValues values = new ContentValues();
                values.put(Lists.LIST_ID, Lists.generateListId(listName));
                values.put(Lists.NAME, listName);
                mContext.getContentResolver().insert(Lists.CONTENT_URI, values);
            }
        }

        @Override
        public Fragment getItem(int position) {
            if (mLists == null) {
                return null;
            }

            mLists.moveToPosition(position);
            return ListsFragment.newInstance(mLists.getString(0));
        }

        @Override
        public int getCount() {
            if (mLists == null) {
                return 1;
            }

            return mLists.getCount();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (mLists == null) {
                return "";
            }

            mLists.moveToPosition(position);
            return mLists.getString(1);
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof ListsFragment) {
                return POSITION_UNCHANGED;
            } else {
                return POSITION_NONE;
            }
        }
    }

    @Override
    public void onCategorySelected(int itemPosition) {
        // only react if everything is set up
        if (!mIsLoaderStartAllowed) {
            return;
        } else {
            // show/hide title indicator
            onDisplayTitleIndicator(itemPosition);

            // attach correct adapter
            onChangePagerAdapter(itemPosition);

            // pass filter to show fragment
            ShowsFragment fragment;
            try {
                fragment = (ShowsFragment) getSupportFragmentManager().findFragmentByTag(
                        Utils.makeViewPagerFragmentName(mPager.getId(), 0));
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

    private void onDisplayTitleIndicator(int itemPosition) {
        if (itemPosition < LIST_NAV_ITEM_POSITION) {
            // displaying shows
            if (mIndicator.getVisibility() == View.VISIBLE) {
                mIndicator.startAnimation(AnimationUtils.loadAnimation(this,
                        android.R.anim.fade_out));
            }
            mIndicator.setVisibility(View.GONE);
        } else {
            // displaying lists
            if (mIndicator.getVisibility() != View.VISIBLE) {
                mIndicator.startAnimation(AnimationUtils.loadAnimation(this,
                        android.R.anim.fade_in));
            }
            mIndicator.setVisibility(View.VISIBLE);
        }
    }

    private void onChangePagerAdapter(int navItem) {
        /*
         * Prevent unnecessary fragment destruction by checking if the right
         * adapter is already attached.
         */
        if (navItem < LIST_NAV_ITEM_POSITION) {
            if (!(mPager.getAdapter() instanceof ShowsPagerAdapter)) {
                mPager.setAdapter(mShowsAdapter);
                mShowsAdapter.notifyDataSetChanged();
            }
        } else {
            if (!(mPager.getAdapter() instanceof ListsPagerAdapter)) {
                mPager.setAdapter(mListsAdapter);
                mListsAdapter.notifyDataSetChanged();
                if (mIndicator != null) {
                    mIndicator.notifyDataSetChanged();
                }
            }
        }
    }
}
