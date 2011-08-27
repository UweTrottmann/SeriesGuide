
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.AddShow;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SeriesDatabase;
import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.SeriesGuideData;
import com.battlelancer.seriesguide.SeriesGuideData.ShowSorting;
import com.battlelancer.seriesguide.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ShowInfo;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.AnalyticsUtils;
import com.battlelancer.seriesguide.util.EulaHelper;
import com.battlelancer.seriesguide.util.UIUtils;
import com.battlelancer.seriesguide.util.UpdateTask;
import com.battlelancer.thetvdbapi.ImageCache;
import com.battlelancer.thetvdbapi.TheTVDB;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.widget.SimpleCursorAdapter;
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
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ShowsActivity extends BaseActivity implements AbsListView.OnScrollListener,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int UPDATE_SUCCESS = 100;

    private static final int UPDATE_INCOMPLETE = 104;

    private static final int CONTEXT_DELETE_ID = 200;

    private static final int CONTEXT_UPDATESHOW_ID = 201;

    private static final int CONTEXT_SHOWINFO = 202;

    private static final int CONTEXT_MARKNEXT = 203;

    private static final int CONTEXT_FAVORITE = 204;

    private static final int CONTEXT_UNFAVORITE = 205;

    public static final int UPDATE_OFFLINE_DIALOG = 300;

    public static final int UPDATE_SAXERROR_DIALOG = 302;

    private static final int CONFIRM_DELETE_DIALOG = 304;

    private static final int WHATS_NEW_DIALOG = 305;

    private static final int SORT_DIALOG = 306;

    private static final int BETA_WARNING_DIALOG = 307;

    private static final int LOADER_ID = 900;

    private static final String STATE_UPDATE_IN_PROGRESS = "seriesguide.update.inprogress";

    private static final String STATE_UPDATE_SHOWS = "seriesguide.update.shows";

    private static final String STATE_UPDATE_INDEX = "seriesguide.update.index";

    private static final String STATE_UPDATE_FAILEDSHOWS = "seriesguide.update.failedshows";

    private static final String STATE_ART_IN_PROGRESS = "seriesguide.art.inprogress";

    private static final String STATE_ART_PATHS = "seriesguide.art.paths";

    private static final String STATE_ART_INDEX = "seriesguide.art.index";

    private Bundle mSavedState;

    private UpdateTask mUpdateTask;

    private FetchArtTask mArtTask;

    public View mProgressOverlay;

    public ProgressBar mUpdateProgress;

    public View mCancelButton;

    private SlowAdapter mAdapter;

    private ImageCache mImageCache;

    private String mFailedShowsString;

    private ShowSorting mSorting;

    private long toDeleteID;

    public boolean mBusy;

    private boolean mOnlyUnwatchedShows;

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(this).trackEvent("Shows", "Click", label, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.shows);

        final Context context = this;

        if (!EulaHelper.hasAcceptedEula(this)) {
            EulaHelper.showEula(false, this);
        }

        updatePreferences();

        mImageCache = ((SeriesGuideApplication) getApplication()).getImageCache();

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

        GridView list = (GridView) findViewById(android.R.id.list);
        list.setAdapter(mAdapter);
        list.setFastScrollEnabled(true);
        list.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                Intent i = new Intent(context, OverviewActivity.class);
                i.putExtra(Shows._ID, String.valueOf(id));
                startActivity(i);
            }
        });
        list.setOnScrollListener(this);
        View emptyView = findViewById(android.R.id.empty);
        if (emptyView != null) {
            list.setEmptyView(emptyView);
        }

        getSupportLoaderManager().initLoader(LOADER_ID, null, this);

        registerForContextMenu(list);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AnalyticsUtils.getInstance(this).trackPageView("/Shows");
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLatestEpisode();
        if (mSavedState != null) {
            restoreLocalState(mSavedState);
        }
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
        saveUpdateTask(outState);
        saveArtTask(outState);
        mSavedState = outState;
    }

    private void restoreLocalState(Bundle savedInstanceState) {
        restoreUpdateTask(savedInstanceState);
        restoreArtTask(savedInstanceState);
    }

    private void restoreArtTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_ART_IN_PROGRESS)) {
            ArrayList<String> paths = savedInstanceState.getStringArrayList(STATE_ART_PATHS);
            int index = savedInstanceState.getInt(STATE_ART_INDEX);

            if (paths != null) {
                mArtTask = (FetchArtTask) new FetchArtTask(paths, index).execute();
                AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                        "Art Task Restored", 0);
            }
        }
    }

    private void saveArtTask(Bundle outState) {
        final FetchArtTask task = mArtTask;
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

    private void restoreUpdateTask(Bundle savedInstanceState) {
        if (savedInstanceState.getBoolean(STATE_UPDATE_IN_PROGRESS)) {
            String[] shows = savedInstanceState.getStringArray(STATE_UPDATE_SHOWS);
            int index = savedInstanceState.getInt(STATE_UPDATE_INDEX);
            String failedShows = savedInstanceState.getString(STATE_UPDATE_FAILEDSHOWS);

            if (shows != null) {
                mUpdateTask = (UpdateTask) new UpdateTask(shows, index, failedShows, this)
                        .execute();
                AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                        "Update Task Restored", 0);
            }
        }
    }

    private void saveUpdateTask(Bundle outState) {
        final UpdateTask task = mUpdateTask;
        if (task != null && task.getStatus() != AsyncTask.Status.FINISHED) {
            task.cancel(true);

            outState.putBoolean(STATE_UPDATE_IN_PROGRESS, true);
            outState.putStringArray(STATE_UPDATE_SHOWS, task.mShows);
            outState.putInt(STATE_UPDATE_INDEX, task.mUpdateCount.get());
            outState.putString(STATE_UPDATE_FAILEDSHOWS, task.mFailedShows);

            mUpdateTask = null;

            AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                    "Update Task Saved", 0);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        String message = "";
        switch (id) {
            case UPDATE_SAXERROR_DIALOG:
                if (getFailedShowsString() != null && getFailedShowsString().length() != 0) {
                    message += getString(R.string.update_incomplete1) + " "
                            + getFailedShowsString() + getString(R.string.update_incomplete2)
                            + getString(R.string.saxerror);
                } else {
                    message += getString(R.string.update_error) + " "
                            + getString(R.string.saxerror);
                }
                return new AlertDialog.Builder(this).setTitle(getString(R.string.saxerror_title))
                        .setMessage(message).setPositiveButton(android.R.string.ok, null).create();
            case UPDATE_OFFLINE_DIALOG:
                return new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.offline_title))
                        .setMessage(
                                getString(R.string.update_error) + " "
                                        + getString(R.string.offline))
                        .setPositiveButton(android.R.string.ok, null).create();
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
                                        SeriesDatabase.deleteShow(getApplicationContext(),
                                                String.valueOf(toDeleteID));
                                        if (progress.isShowing()) {
                                            progress.dismiss();
                                        }
                                    }
                                }).start();
                            }
                        }).setNegativeButton(getString(R.string.dontdelete_show), null).create();
            case WHATS_NEW_DIALOG:
                return new AlertDialog.Builder(this).setTitle(getString(R.string.whatsnew_title))
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setMessage(getString(R.string.whatsnew_content))
                        .setPositiveButton(android.R.string.ok, null).create();
            case BETA_WARNING_DIALOG:
                /* Used for unstable beta releases */
                return new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.whatsnew_title))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(getString(R.string.betawarning))
                        .setPositiveButton(R.string.gobreak, null)
                        .setNeutralButton(getString(R.string.download_stable),
                                new OnClickListener() {

                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            Intent myIntent = new Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("market://details?id=com.battlelancer.seriesguide"));
                                            startActivity(myIntent);
                                        } catch (ActivityNotFoundException e) {
                                            Intent myIntent = new Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("http://market.android.com/details?id=com.battlelancer.seriesguide"));
                                            startActivity(myIntent);
                                        }
                                        finish();
                                    }
                                }).create();
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
                    Shows.FAVORITE
                }, null, null, null);
        show.moveToFirst();
        if (show.getInt(0) == 0) {
            menu.add(0, CONTEXT_FAVORITE, 0, R.string.context_favorite);
        } else {
            menu.add(0, CONTEXT_UNFAVORITE, 0, R.string.context_unfavorite);
        }
        show.close();

        menu.add(0, CONTEXT_SHOWINFO, 1, R.string.context_showinfo);
        menu.add(0, CONTEXT_MARKNEXT, 2, R.string.context_marknext);
        menu.add(0, CONTEXT_UPDATESHOW_ID, 3, R.string.context_updateshow);
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
            case CONTEXT_DELETE_ID:
                fireTrackerEvent("Delete show");

                if (!isUpdateTaskRunning()) {
                    toDeleteID = info.id;
                    showDialog(CONFIRM_DELETE_DIALOG);
                }
                return true;
            case CONTEXT_UPDATESHOW_ID:
                fireTrackerEvent("Update show");

                if (!isUpdateTaskRunning()) {
                    performUpdateTask(false, String.valueOf(info.id));
                }
                return true;
            case CONTEXT_SHOWINFO:
                fireTrackerEvent("Display show info");

                Intent i = new Intent(this, ShowInfo.class);
                i.putExtra(Shows._ID, String.valueOf(info.id));
                startActivity(i);
                return true;
            case CONTEXT_MARKNEXT:
                fireTrackerEvent("Mark next episode");

                SeriesDatabase.markNextEpisode(this, info.id);
                Thread t = new UpdateLatestEpisodeThread(this, String.valueOf(info.id));
                t.start();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.seriesguide_menu, menu);
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
                fireTrackerEvent("Update all shows");

                if (!isUpdateTaskRunning() && !isArtTaskRunning()) {
                    performUpdateTask(false, null);
                }
                return true;
            case R.id.menu_upcoming:
                startActivity(new Intent(this, UpcomingRecentActivity.class));
                return true;
            case R.id.menu_new_show:
                startActivity(new Intent(this, AddShow.class));
                return true;
            case R.id.menu_showsortby:
                fireTrackerEvent("Sort shows");

                showDialog(SORT_DIALOG);
                return true;
            case R.id.menu_updateart:
                fireTrackerEvent("Fetch missing posters");

                if (isArtTaskRunning() || isUpdateTaskRunning()) {
                    return true;
                }

                // already fail if there is no external storage
                if (!UIUtils.isExtStorageAvailable()) {
                    Toast.makeText(this, getString(R.string.update_nosdcard), Toast.LENGTH_LONG)
                            .show();
                } else {
                    Toast.makeText(this, getString(R.string.update_inbackground), Toast.LENGTH_LONG)
                            .show();
                    mArtTask = (FetchArtTask) new FetchArtTask().execute();
                }
                return true;
            case R.id.menu_preferences:
                startActivity(new Intent(this, SeriesGuidePreferences.class));
                return true;
            case R.id.menu_fullupdate:
                fireTrackerEvent("Full Update");

                if (!isUpdateTaskRunning() && !isArtTaskRunning()) {
                    performUpdateTask(true, null);
                }
                return true;
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
    }

    /**
     * Update the latest episode fields for all existing shows.
     */
    public void updateLatestEpisode() {
        Thread t = new UpdateLatestEpisodeThread(this);
        t.start();
    }

    private static class UpdateLatestEpisodeThread extends Thread {
        private Context mContext;

        private String mShowId;

        public UpdateLatestEpisodeThread(Context context) {
            mContext = context;
            this.setName("UpdateLatestEpisode");
        }

        public UpdateLatestEpisodeThread(Context context, String showId) {
            this(context);
            mShowId = showId;
        }

        public void run() {
            if (mShowId != null) {
                // update single show
                SeriesDatabase.updateLatestEpisode(mContext, mShowId);
            } else {
                // update all shows
                final Cursor shows = mContext.getContentResolver().query(Shows.CONTENT_URI,
                        new String[] {
                            Shows._ID
                        }, null, null, null);
                while (shows.moveToNext()) {
                    String id = shows.getString(0);
                    SeriesDatabase.updateLatestEpisode(mContext, id);
                }
                shows.close();
            }

            // Adapter gets notified by ContentProvider
        }
    }

    private void performUpdateTask(boolean isFullUpdate, String showId) {
        Toast.makeText(this, getString(R.string.update_inbackground), Toast.LENGTH_SHORT).show();

        if (isFullUpdate) {
            mUpdateTask = (UpdateTask) new UpdateTask(true, this).execute();
        } else {
            if (showId == null) {
                // (delta) update all shows
                mUpdateTask = (UpdateTask) new UpdateTask(false, this).execute();
            } else {
                // update a single show
                mUpdateTask = (UpdateTask) new UpdateTask(new String[] {
                    showId
                }, 0, "", this).execute();
            }
        }
    }

    /**
     * If the updateThread is already running, shows a toast telling the user to
     * wait.
     * 
     * @return true if an update is in progress and toast was shown, false
     *         otherwise
     */
    private boolean isUpdateTaskRunning() {
        if (mUpdateTask != null && mUpdateTask.getStatus() != AsyncTask.Status.FINISHED) {
            Toast.makeText(this, getString(R.string.update_inprogress), Toast.LENGTH_LONG).show();
            return true;
        } else {
            return false;
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Integer> {
        final AtomicInteger mFetchCount = new AtomicInteger();

        ArrayList<String> mPaths;

        protected FetchArtTask() {
        }

        protected FetchArtTask(ArrayList<String> paths, int index) {
            mPaths = paths;
            mFetchCount.set(index);
        }

        @Override
        protected void onPreExecute() {
            if (mProgressOverlay == null) {
                mProgressOverlay = ((ViewStub) findViewById(R.id.stub_update)).inflate();
                mUpdateProgress = (ProgressBar) findViewById(R.id.ProgressBarShowListDet);

                mCancelButton = mProgressOverlay.findViewById(R.id.overlayCancel);
                mCancelButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        onCancelTasks();
                    }
                });
            }

            mUpdateProgress.setIndeterminate(true);
            showOverlay(mProgressOverlay);
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

                    Toast.makeText(getApplicationContext(),
                            getString(R.string.imagedownload_incomplete), Toast.LENGTH_LONG).show();
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
        if (mUpdateTask != null && mUpdateTask.getStatus() == AsyncTask.Status.RUNNING) {
            mUpdateTask.cancel(true);
            mUpdateTask = null;

            AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                    "Update Task Canceled", 0);
        }
        if (mArtTask != null && mArtTask.getStatus() == AsyncTask.Status.RUNNING) {
            mArtTask.cancel(true);
            mArtTask = null;

            AnalyticsUtils.getInstance(this).trackEvent("Shows", "Task Lifecycle",
                    "Art Task Canceled", 0);
        }
    }

    public void showOverlay(View overlay) {
        overlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in));
        overlay.setVisibility(View.VISIBLE);
    }

    public void hideOverlay(View overlay) {
        overlay.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out));
        overlay.setVisibility(View.GONE);
    }

    private void requery() {
        getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            boolean isAffectingChange = false;
            if (key.equalsIgnoreCase(SeriesGuidePreferences.KEY_ONLY_UNWATCHED_SHOWS)) {
                updateFilters(sharedPreferences);
                isAffectingChange = true;
            }
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
    private void updatePreferences() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        updateSorting(prefs);
        updateFilters(prefs);

        // // display whats new dialog
        // int lastVersion = prefs.getInt(SeriesGuideData.KEY_VERSION, -1);
        // try {
        // int currentVersion =
        // getPackageManager().getPackageInfo(getPackageName(),
        // PackageManager.GET_META_DATA).versionCode;
        // if (currentVersion > lastVersion) {
        // // BETA warning dialog switch
        // showDialog(BETA_WARNING_DIALOG);
        // showDialog(WHATS_NEW_DIALOG);
        // // set this as lastVersion
        // prefs.edit().putInt(SeriesGuideData.KEY_VERSION,
        // currentVersion).commit();
        // }
        // } catch (NameNotFoundException e) {
        // // this should never happen
        // }

        prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
    }

    private void updateFilters(SharedPreferences prefs) {
        mOnlyUnwatchedShows = prefs.getBoolean(SeriesGuidePreferences.KEY_ONLY_UNWATCHED_SHOWS,
                false);
    }

    /**
     * Fetch the sorting preference and store it in this class.
     * 
     * @param prefs
     * @return Returns true if the value changed, false otherwise.
     */
    private boolean updateSorting(SharedPreferences prefs) {
        final ShowSorting oldSorting = mSorting;
        final CharSequence[] items = getResources().getStringArray(R.array.shsortingData);
        final String sortsetting = prefs.getString(SeriesGuidePreferences.KEY_SHOWSSORTORDER,
                "alphabetic");

        for (int i = 0; i < items.length; i++) {
            if (sortsetting.equals(items[i])) {
                mSorting = ShowSorting.values()[i];
                break;
            }
        }

        AnalyticsUtils.getInstance(ShowsActivity.this).trackEvent("Shows", "Sorting",
                mSorting.name(), 0);

        return oldSorting != mSorting;
    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        String selection = null;
        String[] selectionArgs = null;
        if (mOnlyUnwatchedShows) {
            selection = Shows.NEXTAIRDATE + "!=? AND julianday(" + Shows.NEXTAIRDATE
                    + ") <= julianday('now')";
            selectionArgs = new String[] {
                SeriesDatabase.UNKNOWN_NEXT_AIR_DATE
            };
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

    public void setFailedShowsString(String mFailedShowsString) {
        this.mFailedShowsString = mFailedShowsString;
    }

    public String getFailedShowsString() {
        return mFailedShowsString;
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
            String[] values = SeriesGuideData.parseMillisecondsToTime(
                    mCursor.getLong(ShowsQuery.AIRSTIME),
                    mCursor.getString(ShowsQuery.AIRSDAYOFWEEK), ShowsActivity.this);
            viewHolder.airsTime.setText(values[1] + " " + values[0]);

            // set poster only when not busy scrolling
            final String path = mCursor.getString(ShowsQuery.POSTER);
            if (!mBusy) {
                // load poster
                setPosterBitmap(viewHolder.poster, path, false);

                // Null tag means the view has the correct data
                viewHolder.poster.setTag(null);
            } else {
                // only load in-memory poster
                setPosterBitmap(viewHolder.poster, path, true);
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
                        setPosterBitmap(poster, (String) poster.getTag(), false);
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

    /**
     * If {@code isBusy} is {@code true}, then the image is only loaded if it is
     * in memory. In every other case a place-holder is shown.
     * 
     * @param poster
     * @param path
     * @param isBusy
     */
    private void setPosterBitmap(ImageView poster, String path, boolean isBusy) {
        Bitmap bitmap = null;
        if (path.length() != 0) {
            bitmap = mImageCache.getThumb(path, isBusy);
        }

        if (bitmap != null) {
            poster.setImageBitmap(bitmap);
            poster.setTag(null);
        } else {
            // set placeholder
            poster.setImageResource(R.drawable.show_generic);
            // Non-null tag means the view still needs to load it's data
            poster.setTag(path);
        }
    }
}
