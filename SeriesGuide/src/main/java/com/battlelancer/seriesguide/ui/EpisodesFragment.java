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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.EpisodesAdapter;
import com.battlelancer.seriesguide.adapters.EpisodesAdapter.OnFlagEpisodeListener;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Displays a list of episodes of a season.
 */
public class EpisodesFragment extends ListFragment
        implements OnClickListener, OnFlagEpisodeListener, EpisodesAdapter.PopupMenuClickListener {

    private static final String TAG = "Episodes";

    private Constants.EpisodeSorting mSorting;

    private boolean mDualPane;

    private EpisodesAdapter mAdapter;

    private int mStartingPosition;
    private long mLastCheckedItemId;

    /**
     * All values have to be integer.
     */
    public interface InitBundle {

        String SHOW_TVDBID = "show_tvdbid";

        String SEASON_TVDBID = "season_tvdbid";

        String SEASON_NUMBER = "season_number";

        String STARTING_POSITION = "starting_position";
    }

    public static EpisodesFragment newInstance(int showId, int seasonId, int seasonNumber,
            int startingPosition) {
        EpisodesFragment f = new EpisodesFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showId);
        args.putInt(InitBundle.SEASON_TVDBID, seasonId);
        args.putInt(InitBundle.SEASON_NUMBER, seasonNumber);
        args.putInt(InitBundle.STARTING_POSITION, startingPosition);
        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_episodes, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        loadSortOrder();

        // listen to changes to the sorting preference
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.registerOnSharedPreferenceChangeListener(mSortOrderChangeListener);

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View pager = getActivity().findViewById(R.id.pagerEpisodes);
        mDualPane = pager != null && pager.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            mStartingPosition = getArguments().getInt(InitBundle.STARTING_POSITION);
        } else {
            mStartingPosition = -1;
        }
        mLastCheckedItemId = -1;

        mAdapter = new EpisodesAdapter(getActivity(), null, 0, this, this);
        setListAdapter(mAdapter);

        getLoaderManager().initLoader(EpisodesActivity.EPISODES_LOADER_ID, null, mLoaderCallbacks);

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
     * Display the episode at the given position in a detail pane or if not available in a new
     * activity.
     */
    private void showDetails(View view, int position) {
        if (mDualPane) {
            EpisodesActivity activity = (EpisodesActivity) getActivity();
            activity.setCurrentPage(position);
            setItemChecked(position);
        } else {
            int episodeId = (int) getListView().getItemIdAtPosition(position);

            Intent intent = new Intent();
            intent.setClass(getActivity(), EpisodesActivity.class);
            intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

            ActivityCompat.startActivity(getActivity(), intent,
                    ActivityOptionsCompat
                            .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                            .toBundle()
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSortOrder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // stop listening to sort pref changes
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mSortOrderChangeListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.episodelist_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_epsorting) {
            fireTrackerEvent("Sort");
            showSortDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        showDetails(view, position);
    }

    @Override
    public void onPopupMenuClick(View v, final int episodeTvdbId, final int episodeNumber,
            final long releaseTimeMs, final int watchedFlag, final boolean isCollected) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.inflate(R.menu.episodes_popup_menu);

        Menu menu = popupMenu.getMenu();
        menu.findItem(R.id.menu_action_episodes_collection_add).setVisible(!isCollected);
        menu.findItem(R.id.menu_action_episodes_collection_remove).setVisible(isCollected);
        boolean isWatched = EpisodeTools.isWatched(watchedFlag);
        menu.findItem(R.id.menu_action_episodes_watched).setVisible(!isWatched);
        menu.findItem(R.id.menu_action_episodes_not_watched).setVisible(isWatched);
        boolean isSkipped = EpisodeTools.isSkipped(watchedFlag);
        menu.findItem(R.id.menu_action_episodes_skip).setVisible(!isWatched && !isSkipped);
        menu.findItem(R.id.menu_action_episodes_dont_skip).setVisible(isSkipped);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_episodes_watched: {
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, true);
                        fireTrackerEventContextMenu("Flag watched");
                        return true;
                    }
                    case R.id.menu_action_episodes_not_watched: {
                        onFlagEpisodeWatched(episodeTvdbId, episodeNumber, false);
                        fireTrackerEventContextMenu("Flag unwatched");
                        return true;
                    }
                    case R.id.menu_action_episodes_collection_add: {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, true);
                        fireTrackerEventContextMenu("Flag collected");
                        return true;
                    }
                    case R.id.menu_action_episodes_collection_remove: {
                        onFlagEpisodeCollected(episodeTvdbId, episodeNumber, false);
                        fireTrackerEventContextMenu("Flag uncollected");
                        return true;
                    }
                    case R.id.menu_action_episodes_skip: {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, true);
                        fireTrackerEventContextMenu("Flag skipped");
                        return true;
                    }
                    case R.id.menu_action_episodes_dont_skip: {
                        onFlagEpisodeSkipped(episodeTvdbId, episodeNumber, false);
                        fireTrackerEvent("Flag not skipped");
                        return true;
                    }
                    case R.id.menu_action_episodes_watched_previous: {
                        onMarkUntilHere(releaseTimeMs);
                        fireTrackerEventContextMenu("Flag previously aired");
                        return true;
                    }
                    case R.id.menu_action_episodes_manage_lists: {
                        ManageListsDialogFragment.showListsDialog(episodeTvdbId,
                                ListItemTypes.EPISODE,
                                getFragmentManager());
                        fireTrackerEventContextMenu("Manage lists");
                        return true;
                    }
                }
                return false;
            }
        });
        popupMenu.show();
    }

    @Override
    public void onFlagEpisodeWatched(int episodeTvdbId, int episode, boolean isWatched) {
        EpisodeTools.episodeWatched(getActivity(), getShowId(), episodeTvdbId, getSeasonNumber(),
                episode, isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
    }

    public void onFlagEpisodeSkipped(int episodeTvdbId, int episode, boolean isSkipped) {
        EpisodeTools.episodeWatched(getActivity(), getShowId(), episodeTvdbId, getSeasonNumber(),
                episode, isSkipped ? EpisodeFlags.SKIPPED : EpisodeFlags.UNWATCHED);
    }

    public void onFlagEpisodeCollected(int episodeTvdbId, int episode, boolean isCollected) {
        EpisodeTools.episodeCollected(getActivity(), getShowId(), episodeTvdbId, getSeasonNumber(),
                episode, isCollected);
    }

    private void onMarkUntilHere(long episodeFirstReleaseMs) {
        EpisodeTools.episodeWatchedPrevious(getActivity(), getShowId(), episodeFirstReleaseMs);
    }

    private LoaderManager.LoaderCallbacks<Cursor> mLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(),
                    Episodes.buildEpisodesOfSeasonWithShowUri(String.valueOf(getSeasonId())),
                    EpisodesQuery.PROJECTION, null, null, mSorting.query());
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            mAdapter.swapCursor(data);
            // set an initial checked item
            if (mStartingPosition != -1) {
                setItemChecked(mStartingPosition);
                mStartingPosition = -1;
            }
            // correctly restore the last checked item
            else if (mLastCheckedItemId != -1) {
                setItemChecked(mAdapter.getItemPosition(mLastCheckedItemId));
                mLastCheckedItemId = -1;
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mAdapter.swapCursor(null);
        }
    };

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

    private void loadSortOrder() {
        mSorting = DisplaySettings.getEpisodeSortOrder(getActivity());
    }

    private void showSortDialog() {
        FragmentManager fm = getFragmentManager();
        SortDialogFragment sortDialog = SortDialogFragment.newInstance(R.array.epsorting,
                R.array.epsortingData, mSorting.index(),
                DisplaySettings.KEY_EPISODE_SORT_ORDER, R.string.pref_episodesorting);
        sortDialog.show(fm, "fragment_sort");
    }

    private final OnSharedPreferenceChangeListener mSortOrderChangeListener
            = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (DisplaySettings.KEY_EPISODE_SORT_ORDER.equals(key)) {
                onSortOrderChanged();
            }
        }
    };

    private void onSortOrderChanged() {
        loadSortOrder();

        mLastCheckedItemId = getListView().getItemIdAtPosition(
                getListView().getCheckedItemPosition());
        getLoaderManager().restartLoader(EpisodesActivity.EPISODES_LOADER_ID, null,
                mLoaderCallbacks);

        Utils.trackCustomEvent(getActivity(), TAG, "Sorting", mSorting.name());
    }

    /**
     * Highlight the given episode in the list.
     */
    public void setItemChecked(int position) {
        ListView list = getListView();
        list.setItemChecked(position, true);
        if (position <= list.getFirstVisiblePosition()
                || position >= list.getLastVisiblePosition()) {
            list.smoothScrollToPosition(position);
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
