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
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.SeasonsAdapter;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.EpisodeTools.SeasonWatchedType;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;

/**
 * Displays a list of seasons of one show.
 */
public class SeasonsFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnClickListener,
        SeasonsAdapter.PopupMenuClickListener {

    private static final int CONTEXT_WATCHED_SHOW_ALL_ID = 0;

    private static final int CONTEXT_WATCHED_SHOW_NONE_ID = 1;

    private static final int CONTEXT_COLLECTED_SHOW_ALL_ID = 2;

    private static final int CONTEXT_COLLECTED_SHOW_NONE_ID = 3;

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
        View v = inflater.inflate(R.layout.fragment_seasons, container, false);

        mButtonWatchedAll = (ImageView) v.findViewById(R.id.imageViewSeasonsWatchedToggle);
        mButtonCollectedAll = (ImageView) v.findViewById(R.id.imageViewSeasonsCollectedToggle);
        mTextViewRemaining = (TextView) v.findViewById(R.id.textViewSeasonsRemaining);

        return v;
    }

    private void setWatchedToggleState(int unwatchedEpisodes) {
        mWatchedAllEpisodes = unwatchedEpisodes == 0;
        mButtonWatchedAll.setImageResource(mWatchedAllEpisodes ?
                Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableWatchedAll)
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableWatchAll));
        // set onClick listener not before here to avoid unexpected actions
        mButtonWatchedAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                if (!mWatchedAllEpisodes) {
                    popupMenu.getMenu().add(0, CONTEXT_WATCHED_SHOW_ALL_ID, 0, R.string.mark_all);
                }
                popupMenu.getMenu().add(0, CONTEXT_WATCHED_SHOW_NONE_ID, 0, R.string.unmark_all);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
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
                        }
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }

    private void setCollectedToggleState(int uncollectedEpisodes) {
        mCollectedAllEpisodes = uncollectedEpisodes == 0;
        mButtonCollectedAll.setImageResource(mCollectedAllEpisodes ? R.drawable.ic_collected_all
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableCollectAll));
        // set onClick listener not before here to avoid unexpected actions
        mButtonCollectedAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                if (!mCollectedAllEpisodes) {
                    popupMenu.getMenu()
                            .add(0, CONTEXT_COLLECTED_SHOW_ALL_ID, 0, R.string.collect_all);
                }
                popupMenu.getMenu()
                        .add(0, CONTEXT_COLLECTED_SHOW_NONE_ID, 0, R.string.uncollect_all);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
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
                        return false;
                    }
                });
                popupMenu.show();
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getPreferences();

        // populate list
        mAdapter = new SeasonsAdapter(getActivity(), null, 0, this);
        setListAdapter(mAdapter);
        // now let's get a loader or reconnect to existing one
        getLoaderManager().initLoader(OverviewActivity.SEASONS_LOADER_ID, null, this);

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.seasons_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_sesortby) {
            fireTrackerEvent("Sort");
            showSortDialog();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        Intent intent = new Intent(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.SEASON_TVDBID, (int) id);

        ActivityCompat.startActivity(getActivity(), intent,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    @Override
    public void onPopupMenuClick(View v, final int seasonTvdbId, final int seasonNumber) {
        PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
        popupMenu.inflate(R.menu.seasons_popup_menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_seasons_watched_all: {
                        onFlagSeasonWatched(seasonTvdbId, seasonNumber, true);
                        fireTrackerEventContextMenu("Flag all watched");
                        return true;
                    }
                    case R.id.menu_action_seasons_watched_none: {
                        onFlagSeasonWatched(seasonTvdbId, seasonNumber, false);
                        fireTrackerEventContextMenu("Flag all unwatched");
                        return true;
                    }
                    case R.id.menu_action_seasons_collection_add: {
                        onFlagSeasonCollected(seasonTvdbId, seasonNumber, true);
                        fireTrackerEventContextMenu("Flag all collected");
                        return true;
                    }
                    case R.id.menu_action_seasons_collection_remove: {
                        onFlagSeasonCollected(seasonTvdbId, seasonNumber, false);
                        fireTrackerEventContextMenu("Flag all uncollected");
                        return true;
                    }
                    case R.id.menu_action_seasons_skip: {
                        onFlagSeasonSkipped(seasonTvdbId, seasonNumber);
                        fireTrackerEventContextMenu("Flag all skipped");
                        return true;
                    }
                    case R.id.menu_action_seasons_manage_lists: {
                        ManageListsDialogFragment.showListsDialog(seasonTvdbId,
                                ListItemTypes.SEASON,
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

    private int getShowId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void onFlagSeasonSkipped(long seasonId, int seasonNumber) {
        EpisodeTools.seasonWatched(getActivity(), getShowId(), (int) seasonId, seasonNumber,
                EpisodeFlags.SKIPPED);
    }

    /**
     * Changes the seasons episodes watched flags, updates the status label of the season.
     */
    private void onFlagSeasonWatched(long seasonId, int seasonNumber, boolean isWatched) {
        EpisodeTools.seasonWatched(getActivity(), getShowId(), (int) seasonId, seasonNumber,
                isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
    }

    /**
     * Changes the seasons episodes collected flags.
     */
    private void onFlagSeasonCollected(long seasonId, int seasonNumber, boolean isCollected) {
        EpisodeTools.seasonCollected(getActivity(), getShowId(), (int) seasonId, seasonNumber,
                isCollected);
    }

    /**
     * Changes the watched flag for all episodes of the given show, updates the status labels of all
     * seasons.
     */
    private void onFlagShowWatched(boolean isWatched) {
        EpisodeTools.showWatched(getActivity(), getShowId(), isWatched);
    }

    /**
     * Changes the collected flag for all episodes of the given show, updates the status labels of
     * all seasons.
     */
    private void onFlagShowCollected(boolean isCollected) {
        EpisodeTools.showCollected(getActivity(), getShowId(), isCollected);
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

            if (mSeasonId != null) {
                // update one season
                DBUtils.updateUnwatchedCount(context, mSeasonId);
            } else {
                // update all seasons of this show, start with the most recent
                // one
                final Cursor seasons = context.getContentResolver().query(
                        Seasons.buildSeasonsOfShowUri(mShowId), new String[] {
                                Seasons._ID
                        }, null, null, Seasons.COMBINED + " DESC"
                );
                if (seasons == null) {
                    return;
                }
                while (seasons.moveToNext()) {
                    String seasonId = seasons.getString(0);
                    DBUtils.updateUnwatchedCount(context, seasonId);

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
                mSorting.query()
        );
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
        AsyncTask<String, Void, int[]> task = new AsyncTask<String, Void, int[]>() {

            @Override
            protected int[] doInBackground(String... params) {
                if (isCancelled()) {
                    return null;
                }

                int[] counts = new int[2];

                counts[0] = DBUtils.getUnwatchedEpisodesOfShow(getActivity(), params[0]);
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
                        } else if (result[0] == 0) {
                            mTextViewRemaining.setText(null);
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
        AndroidUtils.executeOnPool(task, String.valueOf(getShowId()));
    }

    public interface SeasonsQuery {

        String[] PROJECTION = {
                BaseColumns._ID, Seasons.COMBINED, Seasons.WATCHCOUNT, Seasons.UNAIREDCOUNT,
                Seasons.NOAIRDATECOUNT, Seasons.TOTALCOUNT, Seasons.TAGS
        };

        String SELECTION = Seasons.TOTALCOUNT + ">0";

        int _ID = 0;

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
        getLoaderManager().restartLoader(OverviewActivity.SEASONS_LOADER_ID, null,
                SeasonsFragment.this);
        getActivity().invalidateOptionsMenu();
    }

    public void onEvent(EpisodeTools.EpisodeActionCompletedEvent event) {
        /**
         * Updates the total remaining episodes counter, updates season
         * counters.
         */
        if (isAdded()) {
            onLoadRemainingCounter();
            if (event.mType instanceof EpisodeTools.SeasonWatchedType) {
                // If we can narrow it down to just one season...
                EpisodeTools.SeasonWatchedType seasonWatchedType = (SeasonWatchedType) event.mType;
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
