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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.Constants.SeasonSorting;
import com.battlelancer.seriesguide.adapters.SeasonsAdapter;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.FlagTask.FlagTaskCompletedEvent;
import com.battlelancer.seriesguide.util.FlagTask.SeasonWatchedType;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

import de.greenrobot.event.EventBus;

/**
 * Displays a list of seasons of one show.
 */
public class SeasonsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {

    private static final int CONTEXT_FLAG_ALL_WATCHED_ID = 0;

    private static final int CONTEXT_FLAG_ALL_UNWATCHED_ID = 1;

    private static final int CONTEXT_FLAG_ALL_COLLECTED_ID = 2;

    private static final int CONTEXT_FLAG_ALL_UNCOLLECTED_ID = 3;

    private static final int CONTEXT_MANAGE_LISTS_ID = 4;

    private static final int LOADER_ID = 1;

    private static final String TAG = "Seasons";

    private Constants.SeasonSorting mSorting;

    private SeasonsAdapter mAdapter;

    private TextView mTextViewRemaining;

    private ImageView mButtonCollectedAll;

    private ImageView mButtonWatchedAll;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {
        String SHOW_TVDBID = "show_tvdbid";
    }

    public static SeasonsFragment newInstance(int showId) {
        SeasonsFragment f = new SeasonsFragment();

        // Supply index input as an argument.
        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showId);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.seasons_fragment, container, false);

        mButtonWatchedAll = (ImageView) v.findViewById(R.id.imageViewSeasonsWatchedToggle);

        mButtonCollectedAll = (ImageView) v.findViewById(R.id.imageViewSeasonsCollectedToggle);

        mTextViewRemaining = (TextView) v.findViewById(R.id.textViewSeasonsRemaining);

        return v;
    }

    OnClickListener mListenerFlagAllWatched = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onFlagShowWatched(true);
            fireTrackerEvent("Flag all watched (inline)");
        }
    };
    OnClickListener mListenerFlagAllUnwatched = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onFlagShowWatched(false);
            fireTrackerEvent("Flag all unwatched (inline)");
        }
    };
    OnClickListener mListenerFlagAllCollected = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onFlagShowCollected(true);
            fireTrackerEvent("Flag all collected (inline)");
        }
    };
    OnClickListener mListenerFlagAllUncollected = new OnClickListener() {
        @Override
        public void onClick(View v) {
            onFlagShowCollected(false);
            fireTrackerEvent("Flag all uncollected (inline)");
        }
    };

    private void setWatchedToggleState(Integer result) {
        mButtonWatchedAll.setImageResource(result == 0 ? R.drawable.ic_ticked
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(), R.attr.drawableWatch));
        mButtonWatchedAll
                .setOnClickListener(result == 0 ? mListenerFlagAllUnwatched
                        : mListenerFlagAllWatched);
        CheatSheet.setup(mButtonWatchedAll, result == 0 ? R.string.unmark_all
                : R.string.mark_all);
    }

    private void setCollectedToggleState(Integer result) {
        mButtonCollectedAll.setImageResource(result == 0 ? R.drawable.ic_collected
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(), R.attr.drawableCollect));
        mButtonCollectedAll
                .setOnClickListener(result == 0 ? mListenerFlagAllUncollected
                        : mListenerFlagAllCollected);
        CheatSheet.setup(mButtonCollectedAll, result == 0 ? R.string.uncollect_all
                : R.string.collect_all);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updatePreferences();

        // populate list
        mAdapter = new SeasonsAdapter(getActivity(), null, 0, this);
        setListAdapter(mAdapter);
        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(LOADER_ID, null, this);

        // listen to changes to the sorting preference
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
        updateUnwatchedCounts();
        onLoadRemainingCounter();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // stop listening to sort pref changes
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, CONTEXT_FLAG_ALL_WATCHED_ID, 0, R.string.mark_all);
        menu.add(0, CONTEXT_FLAG_ALL_UNWATCHED_ID, 1, R.string.unmark_all);
        menu.add(0, CONTEXT_FLAG_ALL_COLLECTED_ID, 2, R.string.collect_all);
        menu.add(0, CONTEXT_FLAG_ALL_UNCOLLECTED_ID, 3, R.string.uncollect_all);
        menu.add(0, CONTEXT_MANAGE_LISTS_ID, 4, R.string.list_item_manage);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor season = (Cursor) mAdapter.getItem(info.position);

        switch (item.getItemId()) {
            case CONTEXT_FLAG_ALL_WATCHED_ID: {
                onFlagSeasonWatched(info.id, season.getInt(SeasonsQuery.COMBINED), true);
                fireTrackerEventContextMenu("Flag all watched");
                return true;
            }
            case CONTEXT_FLAG_ALL_UNWATCHED_ID: {
                onFlagSeasonWatched(info.id, season.getInt(SeasonsQuery.COMBINED), false);
                fireTrackerEventContextMenu("Flag all unwatched");
                return true;
            }
            case CONTEXT_FLAG_ALL_COLLECTED_ID: {
                onFlagSeasonCollected(info.id, season.getInt(SeasonsQuery.COMBINED), true);
                fireTrackerEventContextMenu("Flag all collected");
                return true;
            }
            case CONTEXT_FLAG_ALL_UNCOLLECTED_ID: {
                onFlagSeasonCollected(info.id, season.getInt(SeasonsQuery.COMBINED), false);
                fireTrackerEventContextMenu("Flag all uncollected");
                return true;
            }
            case CONTEXT_MANAGE_LISTS_ID: {
                fireTrackerEventContextMenu("Manage lists");
                ListsDialogFragment.showListsDialog(String.valueOf(info.id), ListItemTypes.SEASON,
                        getFragmentManager());
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.seasonlist_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT >= 11) {
            final CharSequence[] items = getResources().getStringArray(R.array.sesorting);
            menu.findItem(R.id.menu_sesortby).setTitle(
                    getString(R.string.sort) + ": " + items[mSorting.index()]);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_watched_all) {
            fireTrackerEvent("Flag all watched");
            onFlagShowWatched(true);
            return true;
        } else if (itemId == R.id.menu_unwatched_all) {
            fireTrackerEvent("Flag all unwatched");
            onFlagShowWatched(false);
            return true;
        } else if (itemId == R.id.menu_collect_all) {
            fireTrackerEvent("Flag all collected");
            onFlagShowCollected(true);
            return true;
        } else if (itemId == R.id.menu_uncollect_all) {
            fireTrackerEvent("Flag all uncollected");
            onFlagShowCollected(false);
            return true;
        } else if (itemId == R.id.menu_sesortby) {
            fireTrackerEvent("Sort");
            showSortDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(getActivity(), EpisodesActivity.class);

        intent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID, (int) id);
        startActivity(intent);
        getSherlockActivity().overridePendingTransition(R.anim.blow_up_enter,
                R.anim.blow_up_exit);
    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    /**
     * Changes the seasons episodes watched flags, updates the status label of
     * the season.
     */
    private void onFlagSeasonWatched(long seasonId, int seasonNumber, boolean isWatched) {
        new FlagTask(getActivity(), getShowId())
                .seasonWatched((int) seasonId, seasonNumber, isWatched)
                .execute();
    }

    /**
     * Changes the seasons episodes collected flags.
     */
    private void onFlagSeasonCollected(long seasonId, int seasonNumber, boolean isCollected) {
        new FlagTask(getActivity(), getShowId())
                .seasonCollected((int) seasonId, seasonNumber, isCollected)
                .execute();
    }

    /**
     * Changes the watched flag for all episodes of the given show, updates the
     * status labels of all seasons.
     */
    private void onFlagShowWatched(boolean isWatched) {
        new FlagTask(getActivity(), getShowId())
                .showWatched(isWatched)
                .execute();
    }

    /**
     * Changes the collected flag for all episodes of the given show, updates
     * the status labels of all seasons.
     */
    private void onFlagShowCollected(boolean isCollected) {
        new FlagTask(getActivity(), getShowId())
                .showCollected(isCollected)
                .execute();
    }

    /**
     * Update unwatched stats for all seasons of this fragments show. Requeries
     * the list afterwards.
     */
    protected void updateUnwatchedCounts() {
        Thread t = new UpdateUnwatchThread(String.valueOf(getShowId()));
        t.start();
    }

    private class UpdateUnwatchThread extends Thread {
        private String mSeasonId;

        private String mShowId;

        public UpdateUnwatchThread(String showId, String seasonid) {
            this(showId);
            mSeasonId = seasonid;
        }

        public UpdateUnwatchThread(String showId) {
            mShowId = showId;
            this.setName("UpdateWatchStatsThread");
        }

        public void run() {
            final FragmentActivity context = getActivity();
            if (context == null) {
                return;
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            if (mSeasonId != null) {
                // update one season
                DBUtils.updateUnwatchedCount(context, mSeasonId, prefs);
            } else {
                // update all seasons of this show, start with the most recent
                // one
                final Cursor seasons = context.getContentResolver().query(
                        Seasons.buildSeasonsOfShowUri(mShowId), new String[] {
                            Seasons._ID
                        }, null, null, Seasons.COMBINED + " DESC");
                while (seasons.moveToNext()) {
                    String seasonId = seasons.getString(0);
                    DBUtils.updateUnwatchedCount(context, seasonId, prefs);

                    notifyContentProvider(context);
                }
                seasons.close();
            }

            notifyContentProvider(context);
        }

        private void notifyContentProvider(final FragmentActivity context) {
            context.getContentResolver().notifyChange(Seasons.buildSeasonsOfShowUri(mShowId), null);
        }
    }

    private void updatePreferences() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        mSorting = SeasonSorting.fromValue(prefs.getString(
                SeriesGuidePreferences.KEY_SEASON_SORT_ORDER, SeasonSorting.LATEST_FIRST.value()));
    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), Seasons.buildSeasonsOfShowUri(String
                .valueOf(getShowId())), SeasonsQuery.PROJECTION, SeasonsQuery.SELECTION, null,
                mSorting.query());
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

    private void onLoadRemainingCounter() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());

        AsyncTask<String, Void, int[]> task = new AsyncTask<String, Void, int[]>() {

            @Override
            protected int[] doInBackground(String... params) {
                if (isCancelled()) {
                    return null;
                }

                int[] counts = new int[2];

                counts[0] = DBUtils.getUnwatchedEpisodesOfShow(getActivity(),
                        params[0],
                        prefs);
                counts[1] = DBUtils.getUncollectedEpisodesOfShow(getActivity(), params[0]);

                return counts;
            }

            @Override
            protected void onPostExecute(int[] result) {
                if (isAdded()) {
                    if (mTextViewRemaining != null) {
                        if (result[0] == -1) {
                            mTextViewRemaining.setText(getString(R.string.remaining,
                                    getString(R.string.norating)));
                        } else {
                            mTextViewRemaining.setText(getString(R.string.remaining, result[0]));
                        }
                    }
                    if (mButtonWatchedAll != null) {
                        setWatchedToggleState(result[0]);
                    }
                    if (mButtonCollectedAll != null) {
                        setCollectedToggleState(result[1]);
                    }
                }
            }

        };
        AndroidUtils.executeAsyncTask(task, String.valueOf(getShowId()));
    }

    public interface SeasonsQuery {

        String[] PROJECTION = {
                BaseColumns._ID, Seasons.COMBINED, Seasons.WATCHCOUNT, Seasons.UNAIREDCOUNT,
                Seasons.NOAIRDATECOUNT, Seasons.TOTALCOUNT
        };

        String SELECTION = Seasons.TOTALCOUNT + ">0";

        // int _ID = 0;

        int COMBINED = 1;

        int WATCHCOUNT = 2;

        int UNAIREDCOUNT = 3;

        int NOAIRDATECOUNT = 4;

        int TOTALCOUNT = 5;
    }

    private void showSortDialog() {
        FragmentManager fm = getFragmentManager();
        SortDialogFragment sortDialog = SortDialogFragment.newInstance(R.array.sesorting,
                R.array.sesortingData, mSorting.index(),
                SeriesGuidePreferences.KEY_SEASON_SORT_ORDER, R.string.pref_seasonsorting);
        sortDialog.show(fm, "fragment_sort");
    }

    private final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(SeriesGuidePreferences.KEY_SEASON_SORT_ORDER)) {
                updateSorting(sharedPreferences);
            }
        }
    };

    @SuppressLint("NewApi")
    private void updateSorting(SharedPreferences prefs) {
        mSorting = SeasonSorting.fromValue(prefs.getString(
                SeriesGuidePreferences.KEY_SEASON_SORT_ORDER, SeasonSorting.LATEST_FIRST.value()));

        EasyTracker.getTracker().sendEvent(TAG, "Sorting", mSorting.name(), (long) 0);

        // restart loader and update menu description
        getLoaderManager().restartLoader(LOADER_ID, null, SeasonsFragment.this);
        getSherlockActivity().invalidateOptionsMenu();
    }

    public void onEvent(FlagTaskCompletedEvent event) {
        /**
         * Updates the total remaining episodes counter, updates season
         * counters.
         */
        if (isAdded()) {
            onLoadRemainingCounter();
            if (event.mType instanceof SeasonWatchedType) {
                // If we can narrow it down to just one season...
                SeasonWatchedType seasonWatchedType = (SeasonWatchedType) event.mType;
                Thread t = new UpdateUnwatchThread(String.valueOf(getShowId()),
                        String.valueOf(seasonWatchedType.getSeasonTvdbId()));
                t.start();
            } else {
                updateUnwatchedCounts();
            }
        }
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    private static void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Action Item", label, (long) 0);
    }

    private static void fireTrackerEventContextMenu(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Context Item", label, (long) 0);
    }
}
