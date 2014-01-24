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

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.adapters.SeasonsAdapter;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.FlagTask.FlagTaskCompletedEvent;
import com.battlelancer.seriesguide.util.FlagTask.SeasonWatchedType;
import com.battlelancer.seriesguide.util.SeasonTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.seriesguide.R;

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

import de.greenrobot.event.EventBus;

/**
 * Displays a list of seasons of one show.
 */
public class SeasonsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnClickListener {

    private static final int CONTEXT_WATCHED_SHOW_ALL_ID = 0;

    private static final int CONTEXT_WATCHED_SHOW_NONE_ID = 1;

    private static final int CONTEXT_COLLECTED_SHOW_ALL_ID = 2;

    private static final int CONTEXT_COLLECTED_SHOW_NONE_ID = 3;

    private static final int CONTEXT_WATCHED_SEASON_ALL_ID = 4;

    private static final int CONTEXT_WATCHED_SEASON_NONE_ID = 5;

    private static final int CONTEXT_COLLECTED_SEASON_ALL_ID = 6;

    private static final int CONTEXT_COLLECTED_SEASON_NONE_ID = 7;

    private static final int CONTEXT_SKIPPED_SEASON_ALL_ID = 8;

    private static final int CONTEXT_MANAGE_LISTS_SEASON_ID = 9;

    private static final int LOADER_ID = 1;

    private static final String TAG = "Seasons";

    private Constants.SeasonSorting mSorting;

    private SeasonsAdapter mAdapter;

    private TextView mTextViewRemaining;

    private ImageView mButtonCollectedAll;

    private ImageView mButtonWatchedAll;

    private boolean mWatchedAllEpisodes;

    private boolean mCollectedAllEpisodes;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.seasons_fragment, container, false);

        mButtonWatchedAll = (ImageView) v.findViewById(R.id.imageViewSeasonsWatchedToggle);
        registerForContextMenu(mButtonWatchedAll);

        mButtonCollectedAll = (ImageView) v.findViewById(R.id.imageViewSeasonsCollectedToggle);
        registerForContextMenu(mButtonCollectedAll);

        mTextViewRemaining = (TextView) v.findViewById(R.id.textViewSeasonsRemaining);

        return v;
    }

    private void setWatchedToggleState(int unwatchedEpisodes) {
        mWatchedAllEpisodes = unwatchedEpisodes == 0;
        mButtonWatchedAll.setImageResource(mWatchedAllEpisodes ? R.drawable.ic_ticked
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableWatch));
        // set onClick listener not before here to avoid unexpected actions
        mButtonWatchedAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().openContextMenu(v);
            }
        });
        CheatSheet.setup(mButtonWatchedAll, mWatchedAllEpisodes ? R.string.unmark_all
                : R.string.mark_all);
    }

    private void setCollectedToggleState(int uncollectedEpisodes) {
        mCollectedAllEpisodes = uncollectedEpisodes == 0;
        mButtonCollectedAll.setImageResource(mCollectedAllEpisodes ? R.drawable.ic_collected
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableCollect));
        // set onClick listener not before here to avoid unexpected actions
        mButtonCollectedAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().openContextMenu(v);
            }
        });
        CheatSheet.setup(mButtonCollectedAll, mCollectedAllEpisodes ? R.string.uncollect_all
                : R.string.collect_all);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getPreferences();

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
        getPreferences();
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

        int viewId = v.getId();
        if (viewId == R.id.imageViewSeasonsWatchedToggle) {
            // display watch actions for show
            if (mWatchedAllEpisodes) {
                menu.add(0, CONTEXT_WATCHED_SHOW_NONE_ID, 0, R.string.unmark_all);
            } else {
                menu.add(0, CONTEXT_WATCHED_SHOW_ALL_ID, 0, R.string.mark_all);
            }
            return;
        }
        if (viewId == R.id.imageViewSeasonsCollectedToggle) {
            // display collect actions for show
            if (mCollectedAllEpisodes) {
                menu.add(0, CONTEXT_COLLECTED_SHOW_NONE_ID, 0, R.string.uncollect_all);
            } else {
                menu.add(0, CONTEXT_COLLECTED_SHOW_ALL_ID, 0, R.string.collect_all);
            }
            return;
        }

        // display season in title
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor season = (Cursor) mAdapter.getItem(info.position);
        if (season == null) {
            return;
        }
        menu.setHeaderTitle(
                SeasonTools.getSeasonString(getActivity(), season.getInt(SeasonsQuery.COMBINED)));

        // display actions for season
        menu.add(0, CONTEXT_WATCHED_SEASON_ALL_ID, 0, R.string.mark_all);
        menu.add(0, CONTEXT_WATCHED_SEASON_NONE_ID, 1, R.string.unmark_all);
        menu.add(0, CONTEXT_SKIPPED_SEASON_ALL_ID, 2, R.string.action_skip);
        menu.add(0, CONTEXT_COLLECTED_SEASON_ALL_ID, 3, R.string.collect_all);
        menu.add(0, CONTEXT_COLLECTED_SEASON_NONE_ID, 4, R.string.uncollect_all);
        menu.add(0, CONTEXT_MANAGE_LISTS_SEASON_ID, 5, R.string.list_item_manage);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        switch (item.getItemId()) {
            case CONTEXT_WATCHED_SHOW_ALL_ID: {
                onFlagShowWatched(true);
                fireTrackerEvent("Flag all watched (inline)");
                return true;
            }
            case CONTEXT_WATCHED_SHOW_NONE_ID: {
                onFlagShowWatched(false);
                fireTrackerEvent("Flag all unwatched (inline)");
                return true;
            }
            case CONTEXT_COLLECTED_SHOW_ALL_ID: {
                onFlagShowCollected(true);
                fireTrackerEvent("Flag all collected (inline)");
                return true;
            }
            case CONTEXT_COLLECTED_SHOW_NONE_ID: {
                onFlagShowCollected(false);
                fireTrackerEvent("Flag all uncollected (inline)");
                return true;
            }
        }

        // need the season cursor for all of the following items
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor season = (Cursor) mAdapter.getItem(info.position);

        switch (item.getItemId()) {
            case CONTEXT_WATCHED_SEASON_ALL_ID: {
                onFlagSeasonWatched(info.id, season.getInt(SeasonsQuery.COMBINED), true);
                fireTrackerEventContextMenu("Flag all watched");
                return true;
            }
            case CONTEXT_WATCHED_SEASON_NONE_ID: {
                onFlagSeasonWatched(info.id, season.getInt(SeasonsQuery.COMBINED), false);
                fireTrackerEventContextMenu("Flag all unwatched");
                return true;
            }
            case CONTEXT_COLLECTED_SEASON_ALL_ID: {
                onFlagSeasonCollected(info.id, season.getInt(SeasonsQuery.COMBINED), true);
                fireTrackerEventContextMenu("Flag all collected");
                return true;
            }
            case CONTEXT_COLLECTED_SEASON_NONE_ID: {
                onFlagSeasonCollected(info.id, season.getInt(SeasonsQuery.COMBINED), false);
                fireTrackerEventContextMenu("Flag all uncollected");
                return true;
            }
            case CONTEXT_SKIPPED_SEASON_ALL_ID: {
                onFlagSeasonSkipped(info.id, season.getInt(SeasonsQuery.COMBINED));
                fireTrackerEventContextMenu("Flag all skipped");
                return true;
            }
            case CONTEXT_MANAGE_LISTS_SEASON_ID: {
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
        inflater.inflate(R.menu.seasons_menu, menu);
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

    private void onFlagSeasonSkipped(long seasonId, int seasonNumber) {
        new FlagTask(getActivity(), getShowId())
                .seasonWatched((int) seasonId, seasonNumber, EpisodeFlags.SKIPPED)
                .execute();
    }

    /**
     * Changes the seasons episodes watched flags, updates the status label of the season.
     */
    private void onFlagSeasonWatched(long seasonId, int seasonNumber, boolean isWatched) {
        new FlagTask(getActivity(), getShowId())
                .seasonWatched((int) seasonId, seasonNumber,
                        isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED)
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
     * Changes the watched flag for all episodes of the given show, updates the status labels of all
     * seasons.
     */
    private void onFlagShowWatched(boolean isWatched) {
        new FlagTask(getActivity(), getShowId())
                .showWatched(isWatched)
                .execute();
    }

    /**
     * Changes the collected flag for all episodes of the given show, updates the status labels of
     * all seasons.
     */
    private void onFlagShowCollected(boolean isCollected) {
        new FlagTask(getActivity(), getShowId())
                .showCollected(isCollected)
                .execute();
    }

    /**
     * Update unwatched stats for all seasons of this fragments show. Requeries the list
     * afterwards.
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
                        Seasons.buildSeasonsOfShowUri(mShowId), new String[]{
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

    private void getPreferences() {
        mSorting = DisplaySettings.getSeasonSortOrder(getActivity());
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
                Seasons.NOAIRDATECOUNT, Seasons.TOTALCOUNT, Seasons.TAGS
        };

        String SELECTION = Seasons.TOTALCOUNT + ">0";

        // int _ID = 0;

        int COMBINED = 1;

        int WATCHCOUNT = 2;

        int UNAIREDCOUNT = 3;

        int NOAIRDATECOUNT = 4;

        int TOTALCOUNT = 5;

        int TAGS = 6;
    }

    private void showSortDialog() {
        FragmentManager fm = getFragmentManager();
        SortDialogFragment sortDialog = SortDialogFragment.newInstance(R.array.sesorting,
                R.array.sesortingData, mSorting.index(),
                DisplaySettings.KEY_SEASON_SORT_ORDER, R.string.pref_seasonsorting);
        sortDialog.show(fm, "fragment_sort");
    }

    private final OnSharedPreferenceChangeListener mPrefsListener
            = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (DisplaySettings.KEY_SEASON_SORT_ORDER.equals(key)) {
                onSortOrderChanged();
            }
        }
    };

    private void onSortOrderChanged() {
        getPreferences();

        Utils.trackCustomEvent(getActivity(), TAG, "Sorting", mSorting.name());

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

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    private void fireTrackerEventContextMenu(String label) {
        Utils.trackContextMenu(getActivity(), TAG, label);
    }
}
