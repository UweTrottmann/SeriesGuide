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
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.Constants.EpisodeSorting;
import com.battlelancer.seriesguide.adapters.EpisodesAdapter;
import com.battlelancer.seriesguide.adapters.EpisodesAdapter.OnFlagEpisodeListener;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

/**
 * Displays a list of episodes of a season.
 */
public class EpisodesFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnClickListener, OnFlagEpisodeListener {

    private static final String TAG = "Episodes";

    private static final int CONTEXT_FLAG_WATCHED_ID = 0;

    private static final int CONTEXT_FLAG_UNWATCHED_ID = 1;

    private static final int CONTEXT_FLAG_COLLECTED_ID = 2;

    private static final int CONTEXT_FLAG_UNCOLLECTED_ID = 3;

    private static final int CONTEXT_MANAGE_LISTS_ID = 4;

    private static final int CONTEXT_FLAG_UNTILHERE_ID = 5;

    private static final int EPISODES_LOADER = 4;

    private Constants.EpisodeSorting mSorting;

    private boolean mDualPane;

    private EpisodesAdapter mAdapter;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {

        String SHOW_TVDBID = "show_tvdbid";

        String SEASON_TVDBID = "season_tvdbid";

        String SEASON_NUMBER = "season_number";

    }

    public static EpisodesFragment newInstance(int showId, int seasonId, int seasonNumber) {
        EpisodesFragment f = new EpisodesFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showId);
        args.putInt(InitBundle.SEASON_TVDBID, seasonId);
        args.putInt(InitBundle.SEASON_NUMBER, seasonNumber);
        f.setArguments(args);

        return f;
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

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View pager = getActivity().findViewById(R.id.pagerEpisodes);
        mDualPane = pager != null && pager.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        mAdapter = new EpisodesAdapter(getActivity(), null, 0, this, this);
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(EPISODES_LOADER, null, this);

        registerForContextMenu(getListView());
        setHasOptionsMenu(true);
    }

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private int getSeasonId() {
        return getArguments().getInt(InitBundle.SEASON_TVDBID);
    }

    private int getSeasonNumber() {
        return getArguments().getInt(InitBundle.SEASON_NUMBER);
    }

    /**
     * Convenience method for showDetails(episodeId) which looks up the episode
     * id in the list view at the given position.
     * 
     * @param position
     */
    private void showDetails(int position) {
        getListView().setItemChecked(position, true);
        showDetails(getListView().getItemIdAtPosition(position));
    }

    /**
     * If not already shown, display a new fragment containing the given
     * episodes information.
     * 
     * @param episodeId
     */
    private void showDetails(long episodeId) {
        if (mDualPane) {
            EpisodesActivity activity = (EpisodesActivity) getActivity();
            activity.onChangePage((int) episodeId);
        } else {
            Intent intent = new Intent();
            intent.setClass(getActivity(), EpisodesActivity.class);
            intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, (int) episodeId);
            startActivity(intent);
            getSherlockActivity().overridePendingTransition(R.anim.blow_up_enter,
                    R.anim.blow_up_exit);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
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

        // only display the action appropriate for the items current state
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Cursor episode = (Cursor) mAdapter.getItem(info.position);

        if (episode.getInt(EpisodesQuery.WATCHED) == 1) {
            menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 1, R.string.unmark_episode);
        } else {
            menu.add(0, CONTEXT_FLAG_WATCHED_ID, 0, R.string.mark_episode);
        }
        if (episode.getInt(EpisodesQuery.COLLECTED) == 1) {
            menu.add(0, CONTEXT_FLAG_UNCOLLECTED_ID, 3, R.string.uncollect);
        } else {
            menu.add(0, CONTEXT_FLAG_COLLECTED_ID, 2, R.string.collect);
        }
        menu.add(0, CONTEXT_FLAG_UNTILHERE_ID, 4, R.string.mark_untilhere);
        menu.add(0, CONTEXT_MANAGE_LISTS_ID, 5, R.string.list_item_manage);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_FLAG_WATCHED_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onFlagEpisodeWatched((int) info.id, items.getInt(EpisodesQuery.NUMBER), true);
                fireTrackerEventContextMenu("Flag watched");
                return true;
            }
            case CONTEXT_FLAG_UNWATCHED_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onFlagEpisodeWatched((int) info.id, items.getInt(EpisodesQuery.NUMBER), false);
                fireTrackerEventContextMenu("Flag unwatched");
                return true;
            }
            case CONTEXT_FLAG_COLLECTED_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onFlagEpisodeCollected((int) info.id, items.getInt(EpisodesQuery.NUMBER), true);
                fireTrackerEventContextMenu("Flag collected");
                return true;
            }
            case CONTEXT_FLAG_UNCOLLECTED_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onFlagEpisodeCollected((int) info.id, items.getInt(EpisodesQuery.NUMBER), false);
                fireTrackerEventContextMenu("Flag uncollected");
                return true;
            }
            case CONTEXT_FLAG_UNTILHERE_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onMarkUntilHere((int) info.id, items.getLong(EpisodesQuery.FIRSTAIREDMS));
                fireTrackerEventContextMenu("Flag previously aired");
                return true;
            }
            case CONTEXT_MANAGE_LISTS_ID: {
                fireTrackerEventContextMenu("Manage lists");
                ListsDialogFragment.showListsDialog(String.valueOf(info.id), ListItemTypes.EPISODE,
                        getFragmentManager());
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodelist_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            final CharSequence[] items = getResources().getStringArray(R.array.epsorting);
            menu.findItem(R.id.menu_epsorting).setTitle(
                    getString(R.string.sort) + ": " + items[mSorting.index()]);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_watched_all) {
            fireTrackerEvent("Flag all watched");
            onFlagSeasonWatched(true);
            return true;
        } else if (itemId == R.id.menu_unwatched_all) {
            fireTrackerEvent("Flag all unwatched");
            onFlagSeasonWatched(false);
            return true;
        } else if (itemId == R.id.menu_collect_all) {
            fireTrackerEvent("Flag all collected");
            onFlagSeasonCollected(true);
            return true;
        } else if (itemId == R.id.menu_uncollect_all) {
            fireTrackerEvent("Flag all uncollected");
            onFlagSeasonCollected(false);
            return true;
        } else if (itemId == R.id.menu_epsorting) {
            fireTrackerEvent("Sort");
            showSortDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails(position);
    }

    @Override
    public void onFlagEpisodeWatched(int episodeTvdbId, int episode, boolean isWatched) {
        new FlagTask(getActivity(), getShowId())
                .episodeWatched(episodeTvdbId, getSeasonNumber(), episode, isWatched)
                .execute();
    }

    public void onFlagEpisodeCollected(int episodeTvdbId, int episode, boolean isCollected) {
        new FlagTask(getActivity(), getShowId())
                .episodeCollected(episodeTvdbId, getSeasonNumber(), episode, isCollected)
                .execute();
    }

    private void onFlagSeasonWatched(boolean isWatched) {
        new FlagTask(getActivity(), getShowId())
                .seasonWatched(getSeasonId(), getSeasonNumber(), isWatched)
                .execute();
    }

    private void onFlagSeasonCollected(boolean isCollected) {
        new FlagTask(getActivity(), getShowId())
                .seasonCollected(getSeasonId(), getSeasonNumber(), isCollected)
                .execute();
    }

    private void onMarkUntilHere(int episodeId, long firstaired) {
        new FlagTask(getActivity(), getShowId())
                .episodeWatchedPrevious(firstaired)
                .execute();
    }

    private void updatePreferences() {
        mSorting = Utils.getEpisodeSorting(getActivity());
    }

    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(getActivity(), Episodes.buildEpisodesOfSeasonWithShowUri(String
                .valueOf(getSeasonId())), EpisodesQuery.PROJECTION, null, null, mSorting.query());
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mAdapter.swapCursor(cursor);
    }

    public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter.swapCursor(null);
    }

    public interface EpisodesQuery {

        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Episodes.WATCHED, Episodes.TITLE,
                Episodes.NUMBER, Episodes.FIRSTAIREDMS, Episodes.DVDNUMBER,
                Episodes.ABSOLUTE_NUMBER, Episodes.COLLECTED
        };

        int _ID = 0;

        int WATCHED = 1;

        int TITLE = 2;

        int NUMBER = 3;

        int FIRSTAIREDMS = 4;

        int DVDNUMBER = 5;

        int ABSOLUTE_NUMBER = 6;

        int COLLECTED = 7;
    }

    private void showSortDialog() {
        FragmentManager fm = getFragmentManager();
        SortDialogFragment sortDialog = SortDialogFragment.newInstance(R.array.epsorting,
                R.array.epsortingData, mSorting.index(),
                SeriesGuidePreferences.KEY_EPISODE_SORT_ORDER, R.string.pref_episodesorting);
        sortDialog.show(fm, "fragment_sort");
    }

    private final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(SeriesGuidePreferences.KEY_EPISODE_SORT_ORDER)) {
                updateSorting(sharedPreferences);
            }
        }
    };

    @SuppressLint("NewApi")
    private void updateSorting(SharedPreferences prefs) {
        mSorting = EpisodeSorting
                .fromValue(prefs.getString(SeriesGuidePreferences.KEY_EPISODE_SORT_ORDER,
                        EpisodeSorting.OLDEST_FIRST.value()));

        getLoaderManager().restartLoader(EPISODES_LOADER, null, EpisodesFragment.this);
        getSherlockActivity().invalidateOptionsMenu();

        EasyTracker.getTracker().sendEvent(TAG, "Sorting", mSorting.name(), (long) 0);
    }

    @TargetApi(8)
    public void setItemChecked(int position) {
        final ListView list = getListView();
        list.setItemChecked(position, true);
        if (AndroidUtils.isFroyoOrHigher()) {
            if (position <= list.getFirstVisiblePosition()
                    || position >= list.getLastVisiblePosition()) {
                list.smoothScrollToPosition(position);
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
