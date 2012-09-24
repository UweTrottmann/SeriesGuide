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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.Constants.SeasonSorting;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.FlagTask.FlagAction;
import com.battlelancer.seriesguide.util.FlagTask.OnFlagListener;
import com.google.analytics.tracking.android.EasyTracker;

/**
 * Displays a list of seasons of one show.
 */
public class SeasonsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnFlagListener {

    private static final int CONTEXT_FLAG_ALL_WATCHED_ID = 0;

    private static final int CONTEXT_FLAG_ALL_UNWATCHED_ID = 1;

    private static final int CONTEXT_MANAGE_LISTS_ID = 2;

    private static final int LOADER_ID = 1;

    private Constants.SeasonSorting mSorting;

    private SimpleCursorAdapter mAdapter;

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

    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent("Seasons", "Click", label, (long) 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updatePreferences();

        // listen to changes to the sorting preference
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(mPrefsListener);

        // populate list
        fillData();

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
        updateUnwatchedCounts(false);
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
        menu.add(0, CONTEXT_MANAGE_LISTS_ID, 2, R.string.list_item_manage);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor season = (Cursor) mAdapter.getItem(info.position);

        switch (item.getItemId()) {
            case CONTEXT_FLAG_ALL_WATCHED_ID: {
                onFlagSeasonWatched(info.id, season.getInt(SeasonsQuery.COMBINED), true);
                return true;
            }
            case CONTEXT_FLAG_ALL_UNWATCHED_ID: {
                onFlagSeasonWatched(info.id, season.getInt(SeasonsQuery.COMBINED), false);
                return true;
            }
            case CONTEXT_MANAGE_LISTS_ID: {
                ListsDialogFragment.showListsDialog(String.valueOf(info.id), 2,
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
        switch (item.getItemId()) {
            case R.id.menu_markall:
                fireTrackerEvent("Mark all seasons");

                onFlagShowWatched(true);
                return true;
            case R.id.menu_unmarkall:
                fireTrackerEvent("Unmark all seasons");

                onFlagShowWatched(false);
                return true;
            case R.id.menu_sesortby:
                fireTrackerEvent("Sort seasons");

                showSortDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(getActivity(), EpisodesActivity.class);

        final Cursor item = (Cursor) (getListView().getItemAtPosition(position));
        final int seasonNumber = item.getInt(SeasonsQuery.COMBINED);
        intent.putExtra(EpisodesActivity.InitBundle.SHOW_TVDBID, getShowId());
        intent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID, (int) id);
        intent.putExtra(EpisodesActivity.InitBundle.SEASON_NUMBER, seasonNumber);
        startActivity(intent);
        getSherlockActivity().overridePendingTransition(R.anim.fragment_slide_left_enter,
                R.anim.fragment_slide_left_exit);
    }

    private void fillData() {
        String[] from = new String[] {
                Seasons.COMBINED, Seasons.WATCHCOUNT, Seasons.TOTALCOUNT
        };
        int[] to = new int[] {
                R.id.TextViewSeasonListTitle, R.id.TextViewSeasonListWatchCount,
                R.id.season_row_root
        };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.season_row, null, from, to,
                CursorAdapter.NO_SELECTION);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == SeasonsQuery.WATCHCOUNT) {
                    final TextView watchcount = (TextView) view;
                    String episodeCount = "";
                    final int count = cursor.getInt(SeasonsQuery.WATCHCOUNT);
                    final int unairedCount = cursor.getInt(SeasonsQuery.UNAIREDCOUNT);
                    final int noairdateCount = cursor.getInt(SeasonsQuery.NOAIRDATECOUNT);

                    // add strings for unwatched episodes
                    if (count == 0) {
                        // make sure there are no unchecked episodes that happen
                        // to have no airdate
                        if (noairdateCount == 0) {
                            episodeCount += getString(R.string.season_allwatched);
                        } else {
                            episodeCount += noairdateCount + " ";
                            if (noairdateCount == 1) {
                                episodeCount += getString(R.string.oneotherepisode);
                            } else {
                                episodeCount += getString(R.string.otherepisodes);
                            }
                        }
                        watchcount.setTextAppearance(getActivity(),
                                R.style.TextAppearance_XSmall_Dim);
                    } else if (count == 1) {
                        episodeCount += count + " " + getString(R.string.season_onenotwatched);
                        watchcount.setTextAppearance(getActivity(), R.style.TextAppearance_XSmall);
                    } else {
                        episodeCount += count + " " + getString(R.string.season_watchcount);
                        watchcount.setTextAppearance(getActivity(), R.style.TextAppearance_XSmall);
                    }

                    // add strings for unaired episodes
                    if (unairedCount > 0) {
                        episodeCount += " (+" + unairedCount + " "
                                + getString(R.string.season_unaired) + ")";
                    }
                    watchcount.setText(episodeCount);

                    return true;
                }
                if (columnIndex == SeasonsQuery.TOTALCOUNT) {
                    final int count = cursor.getInt(SeasonsQuery.WATCHCOUNT);
                    final int unairedCount = cursor.getInt(SeasonsQuery.UNAIREDCOUNT);
                    final int noairdateCount = cursor.getInt(SeasonsQuery.NOAIRDATECOUNT);
                    final int max = cursor.getInt(SeasonsQuery.TOTALCOUNT);
                    final int progress = max - count - unairedCount - noairdateCount;
                    final ProgressBar bar = (ProgressBar) view.findViewById(R.id.seasonProgressBar);
                    final TextView text = (TextView) view.findViewById(R.id.seasonProgressText);
                    bar.setMax(max);
                    bar.setProgress(progress);
                    text.setText(progress + "/" + max);
                    return true;
                }
                if (columnIndex == SeasonsQuery.COMBINED) {
                    final TextView seasonNameTextView = (TextView) view;
                    final String seasonNumber = cursor.getString(SeasonsQuery.COMBINED);
                    final String seasonName;
                    if (seasonNumber.equals("0") || seasonNumber.length() == 0) {
                        seasonName = getString(R.string.specialseason);
                    } else {
                        seasonName = getString(R.string.season) + " " + seasonNumber;
                    }
                    seasonNameTextView.setText(seasonName);

                    return true;
                }
                return false;
            }
        });
        setListAdapter(mAdapter);

        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    /**
     * Changes the seasons episodes watched flags, updates the status label of
     * the season.
     * 
     * @param seasonId
     * @param isWatched
     */
    private void onFlagSeasonWatched(long seasonId, int seasonNumber, boolean isWatched) {
        new FlagTask(getActivity(), getShowId(), this).seasonWatched(seasonNumber)
                .setItemId((int) seasonId).setFlag(isWatched).execute();
    }

    /**
     * Changes the watched flag for all episodes of the given show, updates the
     * status labels of all seasons.
     * 
     * @param seasonid
     * @param isWatched
     */
    private void onFlagShowWatched(boolean isWatched) {
        new FlagTask(getActivity(), getShowId(), this).showWatched().setFlag(isWatched).execute();
    }

    /**
     * Update unwatched stats for all seasons of this fragments show. Requeries
     * the list afterwards.
     */
    protected void updateUnwatchedCounts(boolean updateOverview) {
        Thread t = new UpdateUnwatchThread(String.valueOf(getShowId()), updateOverview);
        t.start();
    }

    private class UpdateUnwatchThread extends Thread {
        private String mSeasonId;

        private String mShowId;

        private boolean mUpdateOverview;

        public UpdateUnwatchThread(String showId, String seasonid, boolean updateOverview) {
            this(showId, updateOverview);
            mSeasonId = seasonid;
        }

        public UpdateUnwatchThread(String showId, boolean updateOverview) {
            mShowId = showId;
            mUpdateOverview = updateOverview;
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
                // update all seasons of this show
                final Cursor seasons = context.getContentResolver().query(
                        Seasons.buildSeasonsOfShowUri(mShowId), new String[] {
                            Seasons._ID
                        }, null, null, null);
                while (seasons.moveToNext()) {
                    String seasonId = seasons.getString(0);
                    DBUtils.updateUnwatchedCount(context, seasonId, prefs);

                    notifyContentProvider(context);
                }
                seasons.close();
            }

            notifyContentProvider(context);

            if (mUpdateOverview) {
                OverviewFragment overview = (OverviewFragment) context.getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_overview);
                if (overview != null) {
                    overview.onLoadEpisode();
                }
            }
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

    private interface SeasonsQuery {

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

    private void updateSorting(SharedPreferences prefs) {
        mSorting = SeasonSorting.fromValue(prefs.getString(
                SeriesGuidePreferences.KEY_SEASON_SORT_ORDER, SeasonSorting.LATEST_FIRST.value()));

        EasyTracker.getTracker().trackEvent("Seasons", "Sorting", mSorting.name(), (long) 0);

        // restart loader and update menu description
        getLoaderManager().restartLoader(LOADER_ID, null, SeasonsFragment.this);
        getSherlockActivity().invalidateOptionsMenu();
    }

    @Override
    public void onFlagCompleted(FlagAction action, int showId, int itemId, boolean isSuccessful) {
        if (isSuccessful) {
            switch (action) {
                case SEASON_WATCHED:
                    Thread t = new UpdateUnwatchThread(String.valueOf(getShowId()),
                            String.valueOf(itemId), true);
                    t.start();
                    break;
                default:
                    updateUnwatchedCounts(true);
                    break;
            }
        }
    }
}
