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
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.Constants.EpisodeSorting;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.WatchedBox;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.SortDialogFragment;
import com.battlelancer.seriesguide.util.FlagTask;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Displays a list of episodes of a season.
 */
public class EpisodesFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CONTEXT_FLAG_WATCHED_ID = 0;

    private static final int CONTEXT_FLAG_UNWATCHED_ID = 1;

    private static final int CONTEXT_DELETE_EPISODE_ID = 2;

    private static final int CONTEXT_MANAGE_LISTS_ID = 3;

    private static final int CONTEXT_FLAG_UNTILHERE_ID = 4;

    private static final int EPISODES_LOADER = 4;

    private Constants.EpisodeSorting mSorting;

    private boolean mDualPane;

    private SimpleCursorAdapter mAdapter;

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

    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent("Episodes", "Click", label, (long) 0);
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
        View pagerFragment = getActivity().findViewById(R.id.pager);
        mDualPane = pagerFragment != null && pagerFragment.getVisibility() == View.VISIBLE;

        if (mDualPane) {
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        String[] from = new String[] {
                Episodes.WATCHED, Episodes.TITLE, Episodes.NUMBER, Episodes.FIRSTAIREDMS
        };
        int[] to = new int[] {
                R.id.CustomCheckBoxWatched, R.id.title, R.id.number, R.id.airdate
        };

        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.episode_row, null, from, to, 0);
        mAdapter.setViewBinder(new ViewBinder() {

            public boolean setViewValue(View view, final Cursor cursor, int columnIndex) {
                // binding the watched column? set checkbox to watched value if
                // yes
                if (columnIndex == EpisodesQuery.WATCHED) {
                    WatchedBox wb = (WatchedBox) view;
                    wb.setChecked(cursor.getInt(columnIndex) > 0);

                    final int episodeId = cursor.getInt(EpisodesQuery._ID);
                    final int episodeNumber = cursor.getInt(EpisodesQuery.NUMBER);
                    wb.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            ((WatchedBox) v).toggle();
                            onFlagEpisodeWatched(episodeId, episodeNumber,
                                    ((WatchedBox) v).isChecked());
                        }
                    });
                    wb.setOnLongClickListener(new OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            Toast infoToast = Toast.makeText(getActivity(), ((WatchedBox) v)
                                    .isChecked() ? R.string.unmark_episode : R.string.mark_episode,
                                    Toast.LENGTH_SHORT);

                            // position toast near view
                            int[] location = new int[2];
                            v.getLocationOnScreen(location);
                            infoToast.setGravity(Gravity.TOP | Gravity.LEFT,
                                    location[0] - v.getWidth() / 2,
                                    location[1] - v.getHeight() - v.getHeight() / 2);

                            infoToast.show();
                            return true;
                        }
                    });

                    return true;
                } else if (columnIndex == EpisodesQuery.NUMBER) {
                    // set episode number and if available dvd episode number
                    TextView tv = (TextView) view;
                    StringBuilder episodenumber = new StringBuilder(cursor
                            .getString(EpisodesQuery.NUMBER));
                    float dvdnumber = cursor.getFloat(EpisodesQuery.DVDNUMBER);
                    if (dvdnumber != 0) {
                        episodenumber.append("(").append(dvdnumber).append(")");
                    }
                    tv.setText(episodenumber);
                    return true;
                } else if (columnIndex == EpisodesQuery.FIRSTAIREDMS) {
                    TextView tv = (TextView) view;
                    long airtime = cursor.getLong(EpisodesQuery.FIRSTAIREDMS);
                    if (airtime != -1) {
                        tv.setText(Utils.formatToTimeAndDay(airtime, getActivity())[2]);
                    } else {
                        tv.setText(getString(R.string.episode_firstaired) + " "
                                + getString(R.string.unknown));
                    }
                    return true;
                }
                // if we did not bind, let the cursor adapter try text and image
                // views
                return false;
            }
        });
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
            intent.setClass(getActivity(), EpisodeDetailsActivity.class);
            intent.putExtra(EpisodeDetailsActivity.InitBundle.EPISODE_TVDBID, (int) episodeId);
            startActivity(intent);
            getSherlockActivity().overridePendingTransition(R.anim.fragment_slide_left_enter,
                    R.anim.fragment_slide_left_exit);
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

        // only display the action appropiate for the items current state
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        WatchedBox watchedBox = (WatchedBox) info.targetView
                .findViewById(R.id.CustomCheckBoxWatched);
        if (watchedBox.isChecked()) {
            menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 1, R.string.unmark_episode);
        } else {
            menu.add(0, CONTEXT_FLAG_WATCHED_ID, 0, R.string.mark_episode);
        }
        menu.add(0, CONTEXT_FLAG_UNTILHERE_ID, 2, R.string.mark_untilhere);
        menu.add(0, CONTEXT_MANAGE_LISTS_ID, 3, R.string.list_item_manage);
        menu.add(0, CONTEXT_DELETE_EPISODE_ID, 4, R.string.delete_show);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_FLAG_WATCHED_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onFlagEpisodeWatched((int) info.id, items.getInt(EpisodesQuery.NUMBER), true);
                return true;
            }
            case CONTEXT_FLAG_UNWATCHED_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onFlagEpisodeWatched((int) info.id, items.getInt(EpisodesQuery.NUMBER), false);
                return true;
            }
            case CONTEXT_FLAG_UNTILHERE_ID: {
                final Cursor items = (Cursor) mAdapter.getItem(info.position);
                onMarkUntilHere((int) info.id, items.getLong(EpisodesQuery.FIRSTAIREDMS));
                return true;
            }
            case CONTEXT_MANAGE_LISTS_ID: {
                ListsDialogFragment.showListsDialog(String.valueOf(info.id), 3,
                        getFragmentManager());
                return true;
            }
            case CONTEXT_DELETE_EPISODE_ID: {
                getActivity().getContentResolver().delete(
                        Episodes.buildEpisodeUri(String.valueOf(info.id)), null, null);
                getActivity().getContentResolver().notifyChange(
                        Episodes.buildEpisodesOfSeasonWithShowUri(String.valueOf(getSeasonId())),
                        null);
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
        switch (item.getItemId()) {
            case R.id.mark_all:
                fireTrackerEvent("Mark all episodes");

                onFlagSeasonWatched(true);
                return true;
            case R.id.unmark_all:
                fireTrackerEvent("Unmark all episodes");

                onFlagSeasonWatched(false);
                return true;
            case R.id.menu_epsorting:
                fireTrackerEvent("Sort episodes");

                showSortDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails(position);
    }

    private void onFlagEpisodeWatched(int episodeId, int episodeNumber, boolean isWatched) {
        new FlagTask(getActivity(), getShowId(), null)
                .episodeWatched(getSeasonNumber(), episodeNumber).setItemId(episodeId)
                .setFlag(isWatched).execute();
    }

    private void onFlagSeasonWatched(boolean isWatched) {
        new FlagTask(getActivity(), getShowId(), null).seasonWatched(getSeasonNumber())
                .setItemId(getSeasonId()).setFlag(isWatched).execute();
    }

    private void onMarkUntilHere(int episodeId, long firstaired) {
        new FlagTask(getActivity(), getShowId(), null).episodeWatchedPrevious(firstaired)
                .setItemId(episodeId).execute();
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

    interface EpisodesQuery {
        String[] PROJECTION = new String[] {
                Tables.EPISODES + "." + Episodes._ID, Episodes.WATCHED, Episodes.TITLE,
                Episodes.NUMBER, Episodes.FIRSTAIREDMS, Episodes.DVDNUMBER
        };

        int _ID = 0;

        int WATCHED = 1;

        int TITLE = 2;

        int NUMBER = 3;

        int FIRSTAIREDMS = 4;

        int DVDNUMBER = 5;

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

    private void updateSorting(SharedPreferences prefs) {
        mSorting = EpisodeSorting
                .fromValue(prefs.getString(SeriesGuidePreferences.KEY_EPISODE_SORT_ORDER,
                        EpisodeSorting.OLDEST_FIRST.value()));

        EasyTracker.getTracker().trackEvent("Episodes", "Sorting", mSorting.name(), (long) 0);

        getLoaderManager().restartLoader(EPISODES_LOADER, null, EpisodesFragment.this);
        getSherlockActivity().invalidateOptionsMenu();
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
}
