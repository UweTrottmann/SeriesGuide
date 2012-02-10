
package com.battlelancer.seriesguide.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EulaHelper;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.UpdateTask;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;

public class ShowsActivity extends BaseActivity implements AbsListView.OnScrollListener,
        LoaderManager.LoaderCallbacks<Cursor>, ActionBar.OnNavigationListener {

    private boolean mBusy;

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_INCOMPLETE = 104;

    private static final int CONTEXT_DELETE_ID = 200;

    private static final int CONTEXT_UPDATESHOW_ID = 201;

    private static final int CONTEXT_MARKNEXT = 203;

    private static final int CONTEXT_FAVORITE = 204;

    private static final int CONTEXT_UNFAVORITE = 205;

    private static final int CONTEXT_HIDE = 206;

    private static final int CONTEXT_UNHIDE = 207;

    private static final int CONFIRM_DELETE_DIALOG = 304;

    private static final int SORT_DIALOG = 306;

    private static final int LOADER_ID = 900;

    // Background Task States
    private static final String STATE_ART_IN_PROGRESS = "seriesguide.art.inprogress";

    private static final String STATE_ART_PATHS = "seriesguide.art.paths";

    private static final String STATE_ART_INDEX = "seriesguide.art.index";

    // Show Filter Ids
    private static final int SHOWFILTER_ALL = 0;

    private static final int SHOWFILTER_FAVORITES = 1;

    private static final int SHOWFILTER_UNSEENEPISODES = 2;

    private static final int SHOWFILTER_HIDDEN = 3;

    private static final String FILTER_ID = "filterid";

    private static final int VER_TRAKT_SEC_CHANGES = 131;

    private Bundle mSavedState;

    private FetchPosterTask mArtTask;

    private SlowAdapter mAdapter;

    private Constants.ShowSorting mSorting;

    private long mToDeleteId;

    private boolean mIsPreventLoaderRestart;

    /**
     * Google Analytics helper method for easy event tracking.
     * 
     * @param label
     */
    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(this).trackEvent("Shows", "Click", label, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shows);

        if (!EulaHelper.hasAcceptedEula(this)) {
            EulaHelper.showEula(false, this);
        }

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        // setup action bar filter list (! use different layouts for ABS)
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        ArrayAdapter<CharSequence> mActionBarList = ArrayAdapter.createFromResource(this,
                R.array.showfilter_list, android.R.layout.simple_dropdown_item_1line);
        mActionBarList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(mActionBarList, this);

        // try to restore previously set show filter
        int showfilter = prefs.getInt(SeriesGuidePreferences.KEY_SHOWFILTER, 0);
        actionBar.setSelectedNavigationItem(showfilter);
        // prevent the onNavigationItemSelected listener from reacting
        mIsPreventLoaderRestart = true;

        updatePreferences(prefs);

        // setup show adapter
        String[] from = new String[] {
                SeriesContract.Shows.TITLE, SeriesContract.Shows.NEXTTEXT,
                SeriesContract.Shows.AIRSTIME, SeriesContract.Shows.NETWORK,
                SeriesContract.Shows.POSTER
        };
        int[] to = new int[] {
                R.id.seriesname, R.id.TextViewShowListNextEpisode, R.id.TextViewShowListAirtime,
                R.id.TextViewShowListNetwork, R.id.showposter
        };
        int layout = R.layout.show_rowairtime;

        mAdapter = new SlowAdapter(this, layout, null, from, to, 0);

        GridView list = (GridView) findViewById(R.id.showlist);
        list.setAdapter(mAdapter);
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent i = new Intent(ShowsActivity.this, OverviewActivity.class);
                i.putExtra(Shows._ID, String.valueOf(id));
                startActivity(i);
            }
        });
        list.setOnScrollListener(this);
        View emptyView = findViewById(R.id.empty);
        if (emptyView != null) {
            list.setEmptyView(emptyView);
        }

        // start loading data
        Bundle args = new Bundle();
        args.putInt(FILTER_ID, showfilter);
        getSupportLoaderManager().initLoader(LOADER_ID, args, this);

        registerForContextMenu(list);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.getInstance(this).trackPageView("/Shows");

        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        // auto-update
        final boolean isAutoUpdateEnabled = prefs.getBoolean(SeriesGuidePreferences.KEY_AUTOUPDATE,
                true);
        if (isAutoUpdateEnabled && !TaskManager.getInstance(this).isUpdateTaskRunning(false)) {
            // allow auto-update if 12 hours have passed
            final long previousUpdateTime = prefs.getLong(SeriesGuidePreferences.KEY_LASTUPDATE, 0);
            long currentTime = System.currentTimeMillis();
            final boolean isTime = currentTime - (previousUpdateTime) > 15 * DateUtils.MINUTE_IN_MILLIS;

            if (isTime) {
                // allow auto-update only on allowed connection
                final boolean isAutoUpdateWlanOnly = prefs.getBoolean(
                        SeriesGuidePreferences.KEY_AUTOUPDATEWLANONLY, true);
                if (!isAutoUpdateWlanOnly || Utils.isWifiConnected(this)) {
                    performUpdateTask(false, null);
                }
            }
        }
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
    protected void onDestroy() {
        super.onDestroy();
        onCancelTasks();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
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
                AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                        "Art Task Restored", 0);
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

            AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                    "Art Task Saved", 0);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_DELETE_DIALOG:
                return new AlertDialog.Builder(this).setMessage(getString(R.string.confirm_delete))
                        .setPositiveButton(getString(R.string.delete_show), new OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {

                                final ProgressDialog progress = new ProgressDialog(
                                        ShowsActivity.this);
                                progress.setCancelable(false);
                                progress.show();

                                new Thread(new Runnable() {
                                    public void run() {
                                        DBUtils.deleteShow(getApplicationContext(),
                                                String.valueOf(mToDeleteId));
                                        if (progress.isShowing()) {
                                            progress.dismiss();
                                        }
                                    }
                                }).start();
                            }
                        }).setNegativeButton(getString(R.string.dontdelete_show), null).create();
            case SORT_DIALOG:
                final CharSequence[] items = getResources().getStringArray(R.array.shsorting);

                return new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.pref_showsorting))
                        .setSingleChoiceItems(items, mSorting.index(),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int item) {
                                        SharedPreferences.Editor prefEditor = PreferenceManager
                                                .getDefaultSharedPreferences(
                                                        getApplicationContext()).edit();
                                        prefEditor
                                                .putString(
                                                        SeriesGuidePreferences.KEY_SHOWSSORTORDER,
                                                        (getResources()
                                                                .getStringArray(R.array.shsortingData))[item]);
                                        prefEditor.commit();
                                        removeDialog(SORT_DIALOG);
                                    }
                                }).create();
        }
        return null;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menuInfo.toString();
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Cursor show = getContentResolver().query(Shows.buildShowUri(String.valueOf(info.id)),
                new String[] {
                        Shows.FAVORITE, Shows.HIDDEN
                }, null, null, null);
        show.moveToFirst();
        if (show.getInt(0) == 0) {
            menu.add(0, CONTEXT_FAVORITE, 0, R.string.context_favorite);
        } else {
            menu.add(0, CONTEXT_UNFAVORITE, 0, R.string.context_unfavorite);
        }
        if (show.getInt(1) == 0) {
            menu.add(0, CONTEXT_HIDE, 3, R.string.context_hide);
        } else {
            menu.add(0, CONTEXT_UNHIDE, 3, R.string.context_unhide);
        }
        show.close();

        menu.add(0, CONTEXT_MARKNEXT, 1, R.string.context_marknext);
        menu.add(0, CONTEXT_UPDATESHOW_ID, 2, R.string.context_updateshow);
        menu.add(0, CONTEXT_DELETE_ID, 4, R.string.delete_show);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_FAVORITE: {
                fireTrackerEvent("Favorite show");

                ContentValues values = new ContentValues();
                values.put(Shows.FAVORITE, true);
                getContentResolver().update(Shows.buildShowUri(String.valueOf(info.id)), values,
                        null, null);
                Toast.makeText(this, getString(R.string.favorited), Toast.LENGTH_SHORT).show();
                return true;
            }
            case CONTEXT_UNFAVORITE: {
                fireTrackerEvent("Unfavorite show");

                ContentValues values = new ContentValues();
                values.put(Shows.FAVORITE, false);
                getContentResolver().update(Shows.buildShowUri(String.valueOf(info.id)), values,
                        null, null);
                Toast.makeText(this, getString(R.string.unfavorited), Toast.LENGTH_SHORT).show();
                return true;
            }
            case CONTEXT_HIDE: {
                fireTrackerEvent("Hidden show");

                ContentValues values = new ContentValues();
                values.put(Shows.HIDDEN, true);
                getContentResolver().update(Shows.buildShowUri(String.valueOf(info.id)), values,
                        null, null);
                Toast.makeText(this, getString(R.string.hidden), Toast.LENGTH_SHORT).show();
                return true;
            }
            case CONTEXT_UNHIDE: {
                fireTrackerEvent("Unhidden show");

                ContentValues values = new ContentValues();
                values.put(Shows.HIDDEN, false);
                getContentResolver().update(Shows.buildShowUri(String.valueOf(info.id)), values,
                        null, null);
                Toast.makeText(this, getString(R.string.unhidden), Toast.LENGTH_SHORT).show();
                return true;
            }
            case CONTEXT_DELETE_ID:
                fireTrackerEvent("Delete show");

                if (!TaskManager.getInstance(this).isUpdateTaskRunning(true)) {
                    mToDeleteId = info.id;
                    showDialog(CONFIRM_DELETE_DIALOG);
                }
                return true;
            case CONTEXT_UPDATESHOW_ID:
                fireTrackerEvent("Update show");

                performUpdateTask(false, String.valueOf(info.id));
                return true;
            case CONTEXT_MARKNEXT:
                fireTrackerEvent("Mark next episode");

                DBUtils.markNextEpisode(this, info.id);
                Utils.updateLatestEpisode(this, String.valueOf(info.id));
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.seriesguide_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            final CharSequence[] items = getResources().getStringArray(R.array.shsorting);
            menu.findItem(R.id.menu_showsortby).setTitle(
                    getString(R.string.sort) + ": " + items[mSorting.index()]);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
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
            case R.id.menu_showsortby:
                fireTrackerEvent("Sort shows");

                showDialog(SORT_DIALOG);
                return true;
            case R.id.menu_updateart:
                fireTrackerEvent("Fetch missing posters");

                if (isArtTaskRunning()) {
                    return true;
                }

                // already fail if there is no external storage
                if (!Utils.isExtStorageAvailable()) {
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

    private void performUpdateTask(boolean isFullUpdate, String showId) {
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
        TaskManager.getInstance(this).tryUpdateTask(task, messageId);
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
                    AnalyticsUtils.getInstance(ShowsActivity.this).trackEvent("Shows",
                            "Fetch missing posters", "Success", 0);

                    Toast.makeText(getApplicationContext(), getString(R.string.update_success),
                            Toast.LENGTH_SHORT).show();
                    break;
                case UPDATE_INCOMPLETE:
                    AnalyticsUtils.getInstance(ShowsActivity.this).trackEvent("Shows",
                            "Fetch missing posters", "Incomplete", 0);

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

            AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                    "Art Task Canceled", 0);
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

    private void requery() {
        int filterId = getSupportActionBar().getSelectedNavigationIndex();
        // just reuse the onNavigationItemSelected callback method
        onNavigationItemSelected(filterId, filterId);
    }

    final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean isAffectingChange = false;
            if (key.equalsIgnoreCase(SeriesGuidePreferences.KEY_SHOWSSORTORDER)) {
                updateSorting(sharedPreferences);
                isAffectingChange = true;
            }
            // TODO: maybe don't requery every time a pref changes (possibly
            // problematic if you change a setting in the settings activity)
            if (isAffectingChange) {
                requery();
            }
        }
    };

    /**
     * Called once on activity creation to load initial settings and display
     * one-time information dialogs.
     */
    private void updatePreferences(SharedPreferences prefs) {
        updateSorting(prefs);

        // between-version upgrade code
        final int lastVersion = prefs.getInt(SeriesGuidePreferences.KEY_VERSION, -1);
        try {
            final int currentVersion = getPackageManager().getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA).versionCode;
            if (currentVersion > lastVersion) {
                Editor editor = prefs.edit();

                if (lastVersion < VER_TRAKT_SEC_CHANGES) {
                    editor.putString(SeriesGuidePreferences.KEY_TRAKTPWD, null);
                    editor.putString(SeriesGuidePreferences.KEY_SECURE, null);
                }

                // BETA warning dialog switch
                // ChangesDialogFragment.show(getSupportFragmentManager());

                // set this as lastVersion
                editor.putInt(SeriesGuidePreferences.KEY_VERSION, currentVersion);

                editor.commit();
            }

        } catch (NameNotFoundException e) {
            // this should never happen
        }

        prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
    }

    /**
     * Fetch the sorting preference and store it in this class.
     * 
     * @param prefs
     * @return Returns true if the value changed, false otherwise.
     */
    private boolean updateSorting(SharedPreferences prefs) {
        final Constants.ShowSorting oldSorting = mSorting;
        final CharSequence[] items = getResources().getStringArray(R.array.shsortingData);
        final String sortsetting = prefs.getString(SeriesGuidePreferences.KEY_SHOWSSORTORDER,
                "alphabetic");

        for (int i = 0; i < items.length; i++) {
            if (sortsetting.equals(items[i])) {
                mSorting = Constants.ShowSorting.values()[i];
                break;
            }
        }

        AnalyticsUtils.getInstance(ShowsActivity.this).trackEvent("Shows", "Sorting",
                mSorting.name(), 0);

        return oldSorting != mSorting;
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String selection = null;
        String[] selectionArgs = null;

        int filterId = args.getInt(FILTER_ID);
        switch (filterId) {
            case SHOWFILTER_ALL:
                selection = Shows.HIDDEN + "=?";
                selectionArgs = new String[] {
                    "0"
                };
                break;
            case SHOWFILTER_FAVORITES:
                selection = Shows.FAVORITE + "=? AND " + Shows.HIDDEN + "=?";
                selectionArgs = new String[] {
                        "1", "0"
                };
                break;
            case SHOWFILTER_UNSEENEPISODES:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                int upcomingLimit = prefs.getInt(SeriesGuidePreferences.KEY_UPCOMING_LIMIT, 1);

                selection = Shows.NEXTAIRDATEMS + "!=? AND " + Shows.NEXTAIRDATEMS + " <=? AND "
                        + Shows.HIDDEN + "=?";
                String nowIn24Hours = String.valueOf(System.currentTimeMillis() + upcomingLimit
                        * DateUtils.DAY_IN_MILLIS);
                selectionArgs = new String[] {
                        DBUtils.UNKNOWN_NEXT_AIR_DATE, nowIn24Hours, "0"
                };
                break;
            case SHOWFILTER_HIDDEN:
                selection = Shows.HIDDEN + "=?";
                selectionArgs = new String[] {
                    "1"
                };
                break;
        }

        return new CursorLoader(this, Shows.CONTENT_URI, ShowsQuery.PROJECTION, selection,
                selectionArgs, mSorting.query());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        // only handle events after the event caused when creating the activity
        if (mIsPreventLoaderRestart) {
            mIsPreventLoaderRestart = false;
        } else {
            // requery with the new filter
            Bundle args = new Bundle();
            args.putInt(FILTER_ID, itemPosition);
            getSupportLoaderManager().restartLoader(LOADER_ID, args, this);

            // save the selected filter back to settings
            Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                    .edit();
            editor.putInt(SeriesGuidePreferences.KEY_SHOWFILTER, itemPosition);
            editor.commit();
        }
        return true;
    }

    private interface ShowsQuery {
        String[] PROJECTION = {
                BaseColumns._ID, Shows.TITLE, Shows.NEXTTEXT, Shows.AIRSTIME, Shows.NETWORK,
                Shows.POSTER, Shows.AIRSDAYOFWEEK, Shows.STATUS, Shows.NEXTAIRDATETEXT
        };

        // int _ID = 0;

        int TITLE = 1;

        int NEXTTEXT = 2;

        int AIRSTIME = 3;

        int NETWORK = 4;

        int POSTER = 5;

        int AIRSDAYOFWEEK = 6;

        int STATUS = 7;

        int NEXTAIRDATETEXT = 8;
    }

    private class SlowAdapter extends SimpleCursorAdapter {

        private LayoutInflater mLayoutInflater;

        private int mLayout;

        public SlowAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
            super(context, layout, c, from, to, flags);

            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLayout = layout;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid) {
                throw new IllegalStateException(
                        "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(mLayout, null);

                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.seriesname);
                viewHolder.network = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNetwork);
                viewHolder.next = (TextView) convertView.findViewById(R.id.next);
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNextEpisode);
                viewHolder.episodeTime = (TextView) convertView.findViewById(R.id.episodetime);
                viewHolder.airsTime = (TextView) convertView
                        .findViewById(R.id.TextViewShowListAirtime);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.showposter);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            viewHolder.name.setText(mCursor.getString(ShowsQuery.TITLE));
            viewHolder.network.setText(mCursor.getString(ShowsQuery.NETWORK));

            // next episode info
            String fieldValue = mCursor.getString(ShowsQuery.NEXTTEXT);
            if (fieldValue.length() == 0) {
                // show show status if there are currently no more
                // episodes
                int status = mCursor.getInt(ShowsQuery.STATUS);

                // Continuing == 1 and Ended == 0
                if (status == 1) {
                    viewHolder.next.setText(getString(R.string.show_isalive));
                } else if (status == 0) {
                    viewHolder.next.setText(getString(R.string.show_isnotalive));
                } else {
                    viewHolder.next.setText("");
                }
                viewHolder.episode.setText("");
                viewHolder.episodeTime.setText("");
            } else {
                viewHolder.next.setText(getString(R.string.nextepisode));
                viewHolder.episode.setText(fieldValue);
                fieldValue = mCursor.getString(ShowsQuery.NEXTAIRDATETEXT);
                viewHolder.episodeTime.setText(fieldValue);
            }

            // airday
            String[] values = Utils.parseMillisecondsToTime(mCursor.getLong(ShowsQuery.AIRSTIME),
                    mCursor.getString(ShowsQuery.AIRSDAYOFWEEK), ShowsActivity.this);
            viewHolder.airsTime.setText(values[1] + " " + values[0]);

            // set poster only when not busy scrolling
            final String path = mCursor.getString(ShowsQuery.POSTER);
            if (!mBusy) {
                // load poster
                Utils.setPosterBitmap(viewHolder.poster, path, false, null);

                // Null tag means the view has the correct data
                viewHolder.poster.setTag(null);
            } else {
                // only load in-memory poster
                Utils.setPosterBitmap(viewHolder.poster, path, true, null);
            }

            return convertView;
        }
    }

    public final class ViewHolder {

        public TextView name;

        public TextView network;

        public TextView next;

        public TextView episode;

        public TextView episodeTime;

        public TextView airsTime;

        public ImageView poster;
    }

    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {
            case OnScrollListener.SCROLL_STATE_IDLE:
                mBusy = false;

                int count = view.getChildCount();
                for (int i = 0; i < count; i++) {
                    final ViewHolder holder = (ViewHolder) view.getChildAt(i).getTag();
                    final ImageView poster = holder.poster;
                    if (poster.getTag() != null) {
                        Utils.setPosterBitmap(poster, (String) poster.getTag(), false, null);
                        poster.setTag(null);
                    }
                }

                break;
            case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                mBusy = false;
                break;
            case OnScrollListener.SCROLL_STATE_FLING:
                mBusy = true;
                break;
        }
    }
}
