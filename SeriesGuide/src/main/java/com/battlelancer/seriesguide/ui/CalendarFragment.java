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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.CalendarAdapter;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.settings.CalendarSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.EpisodeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.tonicartos.widget.stickygridheaders.StickyGridHeadersGridView;

/**
 * Displays upcoming or recent episodes in a scrollable grid, by default grouped by day.
 */
public class CalendarFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener,
        OnSharedPreferenceChangeListener, AdapterView.OnItemLongClickListener {

    private static final String TAG = "Calendar";

    private static final int CONTEXT_FLAG_WATCHED_ID = 0;

    private static final int CONTEXT_FLAG_UNWATCHED_ID = 1;

    private static final int CONTEXT_CHECKIN_ID = 2;

    private static final int CONTEXT_COLLECTION_ADD_ID = 3;

    private static final int CONTEXT_COLLECTION_REMOVE_ID = 4;

    private StickyGridHeadersGridView mGridView;

    private CalendarAdapter mAdapter;

    private Handler mHandler;

    /**
     * Data which has to be passed when creating {@link CalendarFragment}. All Bundle extras are
     * strings, except LOADER_ID and EMPTY_STRING_ID.
     */
    public interface InitBundle {

        String TYPE = "type";

        String ANALYTICS_TAG = "analyticstag";

        String LOADER_ID = "loaderid";

        String EMPTY_STRING_ID = "emptyid";
    }

    public interface CalendarType {

        String UPCOMING = "upcoming";
        String RECENT = "recent";
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);

        TextView emptyView = (TextView) v.findViewById(R.id.emptyViewCalendar);
        emptyView.setText(getString(getArguments().getInt(InitBundle.EMPTY_STRING_ID)));

        mGridView = (StickyGridHeadersGridView) v.findViewById(R.id.gridViewCalendar);
        mGridView.setEmptyView(emptyView);
        mGridView.setAreHeadersSticky(false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // setup adapter
        mAdapter = new CalendarAdapter(getActivity(), null, 0);
        boolean infiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());
        mAdapter.setIsShowingHeaders(!infiniteScrolling);

        // setup grid view
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mGridView.setFastScrollEnabled(infiniteScrolling);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        // prevent stale upcoming/recent episodes, also:
        /**
         * Workaround for loader issues on config changes. For some reason the
         * CursorLoader holds on to a cursor with old data. See
         * https://github.com/UweTrottmann/SeriesGuide/issues/257.
         */
        boolean isLoaderExists = getLoaderManager().getLoader(getLoaderId()) != null;
        getLoaderManager().initLoader(getLoaderId(), null, this);
        if (isLoaderExists) {
            onRequery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        // avoid CPU activity
        schedulePeriodicDataRefresh(false);
    }

    @Override
    public void onDestroy() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.calendar_menu, menu);

        // set menu items to current values
        menu.findItem(R.id.menu_onlyfavorites)
                .setChecked(CalendarSettings.isOnlyFavorites(getActivity()));
        menu.findItem(R.id.menu_nospecials)
                .setChecked(DisplaySettings.isHidingSpecials(getActivity()));
        menu.findItem(R.id.menu_nowatched)
                .setChecked(DisplaySettings.isNoWatchedEpisodes(getActivity()));
        menu.findItem(R.id.menu_infinite_scrolling)
                .setChecked(CalendarSettings.isInfiniteScrolling(getActivity()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_onlyfavorites) {
            toggleFilterSetting(item, CalendarSettings.KEY_ONLY_FAVORITE_SHOWS);
            fireTrackerEvent("Only favorite shows Toggle");
            return true;
        } else if (itemId == R.id.menu_nospecials) {
            toggleFilterSetting(item, DisplaySettings.KEY_HIDE_SPECIALS);
            fireTrackerEvent("Hide specials Toggle");
            return true;
        } else if (itemId == R.id.menu_nowatched) {
            toggleFilterSetting(item, DisplaySettings.KEY_NO_WATCHED_EPISODES);
            fireTrackerEvent("Hide watched Toggle");
            return true;
        } else if (itemId == R.id.menu_infinite_scrolling) {
            toggleFilterSetting(item, CalendarSettings.KEY_INFINITE_SCROLLING);
            fireTrackerEvent("Infinite Scrolling Toggle");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    public void checkInEpisode(int episodeTvdbId) {
        CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(), episodeTvdbId);
        if (f != null) {
            f.show(getFragmentManager(), "checkin-dialog");
        }
    }

    private void updateEpisodeCollectionState(int showTvdbId, int episodeTvdbId, int seasonNumber,
            int episodeNumber, boolean addToCollection) {
        EpisodeTools.episodeCollected(getActivity(), showTvdbId, episodeTvdbId, seasonNumber,
                episodeNumber, addToCollection);
    }

    private void updateEpisodeWatchedState(int showTvdbId, int episodeTvdbId, int seasonNumber,
            int episodeNumber, boolean isWatched) {
        EpisodeTools.episodeWatched(getActivity(), showTvdbId, episodeTvdbId, seasonNumber,
                episodeNumber, isWatched ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int episodeId = (int) id;

        Intent intent = new Intent();
        intent.setClass(getActivity(), EpisodesActivity.class);
        intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);

        ActivityCompat.startActivity(getActivity(), intent,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, final int position,
            final long id) {
        PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
        Menu menu = popupMenu.getMenu();

        Cursor episode = (Cursor) mAdapter.getItem(position);
        if (episode == null) {
            return false;
        }

        // only display the action appropriate for the items current state
        menu.add(0, CONTEXT_CHECKIN_ID, 0, R.string.checkin);
        if (EpisodeTools.isWatched(episode.getInt(CalendarAdapter.Query.WATCHED))) {
            menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 1, R.string.action_unwatched);
        } else {
            menu.add(0, CONTEXT_FLAG_WATCHED_ID, 1, R.string.action_watched);
        }
        if (EpisodeTools.isCollected(episode.getInt(CalendarAdapter.Query.COLLECTED))) {
            menu.add(0, CONTEXT_COLLECTION_REMOVE_ID, 2, R.string.action_collection_remove);
        } else {
            menu.add(0, CONTEXT_COLLECTION_ADD_ID, 2, R.string.action_collection_add);
        }

        final int showTvdbId = episode.getInt(CalendarAdapter.Query.SHOW_ID);
        final int episodeTvdbId = episode.getInt(CalendarAdapter.Query._ID);
        final int seasonNumber = episode.getInt(CalendarAdapter.Query.SEASON);
        final int episodeNumber = episode.getInt(CalendarAdapter.Query.NUMBER);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case CONTEXT_CHECKIN_ID: {
                        checkInEpisode((int) id);
                        return true;
                    }
                    case CONTEXT_FLAG_WATCHED_ID: {
                        updateEpisodeWatchedState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, true);
                        return true;
                    }
                    case CONTEXT_FLAG_UNWATCHED_ID: {
                        updateEpisodeWatchedState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, false);
                        return true;
                    }
                    case CONTEXT_COLLECTION_ADD_ID: {
                        updateEpisodeCollectionState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, true);
                        return true;
                    }
                    case CONTEXT_COLLECTION_REMOVE_ID: {
                        updateEpisodeCollectionState(showTvdbId, episodeTvdbId, seasonNumber,
                                episodeNumber, false);
                        return true;
                    }
                }
                return false;
            }
        });

        popupMenu.show();

        return true;
    }

    public void onRequery() {
        getLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    private int getLoaderId() {
        return getArguments().getInt("loaderid");
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String type = getArguments().getString(InitBundle.TYPE);
        boolean isInfiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());

        // infinite or 30 days activity stream
        String[][] queryArgs = DBUtils.buildActivityQuery(getActivity(), type,
                isInfiniteScrolling ? -1 : 30);

        // prevent upcoming/recent episodes from becoming stale
        schedulePeriodicDataRefresh(true);

        return new CursorLoader(getActivity(), Episodes.CONTENT_URI_WITHSHOW,
                CalendarAdapter.Query.PROJECTION, queryArgs[0][0], queryArgs[1], queryArgs[2][0]);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private void schedulePeriodicDataRefresh(boolean enableRefresh) {
        if (mHandler == null) {
            mHandler = new Handler();
        }
        mHandler.removeCallbacks(mDataRefreshRunnable);
        if (enableRefresh) {
            mHandler.postDelayed(mDataRefreshRunnable, 5 * DateUtils.MINUTE_IN_MILLIS);
        }
    }

    private Runnable mDataRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                getLoaderManager().restartLoader(getLoaderId(), null, CalendarFragment.this);
            }
        }
    };

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (CalendarSettings.KEY_INFINITE_SCROLLING.equals(key)) {
            boolean infiniteScrolling = CalendarSettings.isInfiniteScrolling(getActivity());
            mAdapter.setIsShowingHeaders(!infiniteScrolling);
            mGridView.setFastScrollEnabled(infiniteScrolling);
        }
        if (CalendarSettings.KEY_ONLY_FAVORITE_SHOWS.equals(key)
                || DisplaySettings.KEY_HIDE_SPECIALS.equals(key)
                || DisplaySettings.KEY_NO_WATCHED_EPISODES.equals(key)
                || CalendarSettings.KEY_INFINITE_SCROLLING.equals(key)) {
            onRequery();
        }
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    @SuppressLint("CommitPrefEdits")
    private void toggleFilterSetting(MenuItem item, String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().putBoolean(key, !item.isChecked()).commit();

        // refresh filter icon state
        getActivity().supportInvalidateOptionsMenu();
    }
}
