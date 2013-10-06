/*
 * Copyright 2012 Uwe Trottmann
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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.ShowsDistillationSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.CheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ConfirmDeleteDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FlagTask.FlagTaskCompletedEvent;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;

import de.greenrobot.event.EventBus;

/**
 * Displays the list of shows in a users local library with sorting and filtering abilities. The
 * main view of the app.
 */
public class ShowsFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, OnClickListener {

    private static final String TAG = "Shows";

    public static final int LOADER_ID = R.layout.shows_fragment;

    // context menu items
    private static final int CONTEXT_DELETE_ID = 200;

    private static final int CONTEXT_UPDATE_ID = 201;

    private static final int CONTEXT_FLAG_NEXT_ID = 202;

    private static final int CONTEXT_FAVORITE_ID = 203;

    private static final int CONTEXT_UNFAVORITE_ID = 204;

    private static final int CONTEXT_HIDE_ID = 205;

    private static final int CONTEXT_UNHIDE_ID = 206;

    private static final int CONTEXT_MANAGE_LISTS_ID = 207;

    private static final int CONTEXT_CHECKIN_ID = 208;

    private SlowAdapter mAdapter;

    private GridView mGrid;

    private int mSortOrderId;
    private boolean mIsSortFavoritesFirst;

    private boolean mIsFilterFavorites;
    private boolean mIsFilterUnwatched;
    private boolean mIsFilterUpcoming;
    private boolean mIsFilterHidden;

    public static ShowsFragment newInstance() {
        ShowsFragment f = new ShowsFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.shows_fragment, container, false);

        v.findViewById(R.id.emptyViewShows).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getActivity(), AddActivity.class));
            }
        });
        v.findViewById(R.id.emptyViewShowsFilter).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mIsFilterFavorites = mIsFilterUnwatched = mIsFilterUpcoming = mIsFilterHidden
                        = false;

                // already start loading, do not need to wait on saving prefs
                getLoaderManager().restartLoader(ShowsFragment.LOADER_ID, null, ShowsFragment.this);

                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_FAVORITES, false)
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, false)
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_UPCOMING, false)
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_HIDDEN, false)
                        .commit();

                // refresh filter menu check box states
                getActivity().supportInvalidateOptionsMenu();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get settings
        getSortAndFilterSettings();

        // prepare view adapter
        int resIdStar = Utils.resolveAttributeToResourceId(getSherlockActivity().getTheme(),
                R.attr.drawableStar);
        int resIdStarZero = Utils.resolveAttributeToResourceId(getSherlockActivity().getTheme(),
                R.attr.drawableStar0);
        mAdapter = new SlowAdapter(getActivity(), null, 0, resIdStar, resIdStarZero, this);

        // setup grid view
        mGrid = (GridView) getView().findViewById(R.id.gridViewShows);
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);
        registerForContextMenu(mGrid);

        // start loading data
        getLoaderManager().initLoader(LOADER_ID, null, this);

        // listen for some settings changes
        PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(mPrefsListener);

        setHasOptionsMenu(true);
    }

    private void getSortAndFilterSettings() {
        mIsFilterFavorites = ShowsDistillationSettings.isFilteringFavorites(getActivity());
        mIsFilterUnwatched = ShowsDistillationSettings.isFilteringUnwatched(getActivity());
        mIsFilterUpcoming = ShowsDistillationSettings.isFilteringUpcoming(getActivity());
        mIsFilterHidden = ShowsDistillationSettings.isFilteringHidden(getActivity());

        mSortOrderId = ShowsDistillationSettings.getSortOrderId(getActivity());
        mIsSortFavoritesFirst = ShowsDistillationSettings.isSortFavoritesFirst(getActivity());
    }

    private void updateEmptyView() {
        View oldEmptyView = mGrid.getEmptyView();

        View emptyView = null;
        if (mIsFilterFavorites || mIsFilterUnwatched || mIsFilterUpcoming || mIsFilterHidden) {
            emptyView = getView().findViewById(R.id.emptyViewShowsFilter);
        } else {
            emptyView = getView().findViewById(R.id.emptyViewShows);
        }

        if (oldEmptyView != null) {
            oldEmptyView.setVisibility(View.GONE);
        }

        if (emptyView != null) {
            mGrid.setEmptyView(emptyView);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menuInfo.toString();

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        final Cursor show = getActivity().getContentResolver().query(
                Shows.buildShowUri(String.valueOf(info.id)), new String[] {
                        Shows.FAVORITE, Shows.HIDDEN
                }, null, null, null);
        show.moveToFirst();
        if (show.getInt(0) == 0) {
            menu.add(0, CONTEXT_FAVORITE_ID, 2, R.string.context_favorite);
        } else {
            menu.add(0, CONTEXT_UNFAVORITE_ID, 2, R.string.context_unfavorite);
        }
        if (show.getInt(1) == 0) {
            menu.add(0, CONTEXT_HIDE_ID, 3, R.string.context_hide);
        } else {
            menu.add(0, CONTEXT_UNHIDE_ID, 3, R.string.context_unhide);
        }
        show.close();

        menu.add(0, CONTEXT_CHECKIN_ID, 0, R.string.checkin);
        menu.add(0, CONTEXT_FLAG_NEXT_ID, 1, R.string.context_marknext);
        menu.add(0, CONTEXT_MANAGE_LISTS_ID, 4, R.string.list_item_manage);
        menu.add(0, CONTEXT_UPDATE_ID, 5, R.string.context_updateshow);
        menu.add(0, CONTEXT_DELETE_ID, 6, R.string.delete_show);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId()) {
            case CONTEXT_CHECKIN_ID: {
                fireTrackerEventContext("Check in");

                Cursor show = (Cursor) mAdapter.getItem(info.position);
                int episodeTvdbId = show.getInt(ShowsQuery.NEXTEPISODE);
                if (episodeTvdbId <= 0) {
                    return true;
                }

                // display a check-in dialog
                CheckInDialogFragment f = CheckInDialogFragment.newInstance(getActivity(),
                        episodeTvdbId);
                f.show(getFragmentManager(), "checkin-dialog");

                return true;
            }
            case CONTEXT_FAVORITE_ID: {
                onFavoriteShow(String.valueOf(info.id), true);
                return true;
            }
            case CONTEXT_UNFAVORITE_ID: {
                onFavoriteShow(String.valueOf(info.id), false);
                return true;
            }
            case CONTEXT_HIDE_ID: {
                fireTrackerEventContext("Hide show");

                ContentValues values = new ContentValues();
                values.put(Shows.HIDDEN, true);
                getActivity().getContentResolver().update(
                        Shows.buildShowUri(String.valueOf(info.id)), values, null, null);
                Toast.makeText(getActivity(), getString(R.string.hidden), Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
            case CONTEXT_UNHIDE_ID: {
                fireTrackerEventContext("Unhide show");

                ContentValues values = new ContentValues();
                values.put(Shows.HIDDEN, false);
                getActivity().getContentResolver().update(
                        Shows.buildShowUri(String.valueOf(info.id)), values, null, null);
                Toast.makeText(getActivity(), getString(R.string.unhidden), Toast.LENGTH_SHORT)
                        .show();
                return true;
            }
            case CONTEXT_DELETE_ID:
                if (!SgSyncAdapter.isSyncActive(getActivity(), true)) {
                    showDeleteDialog(info.id);
                }

                fireTrackerEventContext("Delete show");
                return true;
            case CONTEXT_UPDATE_ID:
                SgSyncAdapter.requestSync(getActivity(), (int) info.id);

                fireTrackerEventContext("Update show");
                return true;
            case CONTEXT_FLAG_NEXT_ID:
                fireTrackerEventContext("Mark next episode");

                Cursor show = (Cursor) mAdapter.getItem(info.position);
                DBUtils.markNextEpisode(getActivity(), (int) info.id,
                        show.getInt(ShowsQuery.NEXTEPISODE));

                return true;
            case CONTEXT_MANAGE_LISTS_ID: {
                fireTrackerEventContext("Manage lists");

                ListsDialogFragment.showListsDialog(String.valueOf(info.id), ListItemTypes.SHOW,
                        getFragmentManager());
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.shows_menu, menu);

        // set filter check box states
        menu.findItem(R.id.menu_action_shows_filter_favorites)
                .setChecked(mIsFilterFavorites);
        menu.findItem(R.id.menu_action_shows_filter_unwatched)
                .setChecked(mIsFilterUnwatched);
        menu.findItem(R.id.menu_action_shows_filter_upcoming)
                .setChecked(mIsFilterUpcoming);
        menu.findItem(R.id.menu_action_shows_filter_hidden)
                .setChecked(mIsFilterHidden);

        // set sort check box state
        menu.findItem(R.id.menu_action_shows_sort_favorites)
                .setChecked(mIsSortFavoritesFirst);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isMenuDrawerOpen();
        menu.findItem(R.id.menu_action_shows_filter).setVisible(!isDrawerOpen);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_filter) {
            fireTrackerEventAction("Filter shows");
            // did not handle here
            return super.onOptionsItemSelected(item);
        } else if (itemId == R.id.menu_action_shows_sort) {
            fireTrackerEventAction("Sort shows");
            // did not handle here
            return super.onOptionsItemSelected(item);
        } else if (itemId == R.id.menu_action_shows_filter_favorites) {
            mIsFilterFavorites = !mIsFilterFavorites;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_FAVORITES, mIsFilterFavorites, item);

            fireTrackerEventAction("Filter Favorites");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_unwatched) {
            mIsFilterUnwatched = !mIsFilterUnwatched;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, mIsFilterUnwatched, item);

            fireTrackerEventAction("Filter Unwatched");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_upcoming) {
            mIsFilterUpcoming = !mIsFilterUpcoming;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UPCOMING, mIsFilterUpcoming, item);

            fireTrackerEventAction("Filter Upcoming");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_hidden) {
            mIsFilterHidden = !mIsFilterHidden;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_HIDDEN, mIsFilterHidden, item);

            fireTrackerEventAction("Filter Hidden");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_title) {
            if (mSortOrderId == ShowsDistillationSettings.ShowsSortOrder.TITLE_ID) {
                mSortOrderId = ShowsDistillationSettings.ShowsSortOrder.TITLE_REVERSE_ID;
            } else {
                mSortOrderId = ShowsDistillationSettings.ShowsSortOrder.TITLE_ID;
            }
            changeSort();

            fireTrackerEventAction("Sort Title");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_episode) {
            if (mSortOrderId == ShowsDistillationSettings.ShowsSortOrder.EPISODE_ID) {
                mSortOrderId = ShowsDistillationSettings.ShowsSortOrder.EPISODE_REVERSE_ID;
            } else {
                mSortOrderId = ShowsDistillationSettings.ShowsSortOrder.EPISODE_ID;
            }
            changeSort();

            fireTrackerEventAction("Sort Episode");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_favorites) {
            mIsSortFavoritesFirst = !mIsSortFavoritesFirst;
            changeSortOrFilter(ShowsDistillationSettings.KEY_SORT_FAVORITES_FIRST,
                    mIsSortFavoritesFirst, item);

            fireTrackerEventAction("Sort Favorites");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void changeSortOrFilter(String key, boolean state, MenuItem item) {
        // already start loading, do not need to wait on saving prefs
        getLoaderManager().restartLoader(ShowsFragment.LOADER_ID, null, this);

        // update menu item state, then save at last
        item.setChecked(state);
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putBoolean(key, state).commit();
    }

    private void changeSort() {
        // already start loading, do not need to wait on saving prefs
        getLoaderManager().restartLoader(ShowsFragment.LOADER_ID, null, this);

        // save new sort order to preferences
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(ShowsDistillationSettings.KEY_SORT_ORDER, mSortOrderId).commit();
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @TargetApi(16)
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // display overview for this show

        Intent i = new Intent(getActivity(), OverviewActivity.class);
        i.putExtra(OverviewFragment.InitBundle.SHOW_TVDBID, (int) id);
        startActivity(i);
        getActivity().overridePendingTransition(R.anim.blow_up_enter, R.anim.blow_up_exit);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        StringBuilder selection = new StringBuilder();

        // create temporary copies
        final boolean isFilterFavorites = mIsFilterFavorites;
        final boolean isFilterUnwatched = mIsFilterUnwatched;
        final boolean isFilterUpcoming = mIsFilterUpcoming;
        final boolean isFilterHidden = mIsFilterHidden;

        // restrict to favorites?
        if (isFilterFavorites) {
            selection.append(Shows.FAVORITE).append("=1");
        }

        final long timeInAnHour = System.currentTimeMillis() + DateUtils.HOUR_IN_MILLIS;

        // restrict to shows with a next episode?
        if (isFilterUnwatched) {
            if (selection.length() != 0) {
                selection.append(" AND ");
            }
            selection.append(Shows.NEXTAIRDATEMS).append("!=").append(DBUtils.UNKNOWN_NEXT_AIR_DATE);

            // exclude shows with upcoming next episode
            if (!isFilterUpcoming) {
                selection.append(" AND ")
                        .append(Shows.NEXTAIRDATEMS).append("<=")
                        .append(timeInAnHour);
            }
        }
        // restrict to shows with an upcoming (yet to air) next episode?
        if (isFilterUpcoming) {
            if (selection.length() != 0) {
                selection.append(" AND ");
            }
            // Display shows upcoming within <limit> days + 1 hour
            int upcomingLimitInDays = AdvancedSettings.getUpcomingLimitInDays(getActivity());
            long latestAirtime = timeInAnHour
                    + upcomingLimitInDays * DateUtils.DAY_IN_MILLIS;

            selection.append(Shows.NEXTAIRDATEMS).append("<=").append(latestAirtime);

            // exclude shows with no upcoming next episode if not filtered for unwatched, too
            if (!isFilterUnwatched) {
                selection.append(" AND ")
                        .append(Shows.NEXTAIRDATEMS).append(">=")
                        .append(timeInAnHour);
            }
        }

        // special: if hidden filter is disabled, exclude hidden shows
        if (selection.length() != 0) {
            selection.append(" AND ");
        }
        selection.append(Shows.HIDDEN).append(isFilterHidden ? "=1" : "=0");

        return new CursorLoader(getActivity(), Shows.CONTENT_URI, ShowsQuery.PROJECTION,
                selection.toString(), null,
                ShowsDistillationSettings.getSortQuery(mSortOrderId, mIsSortFavoritesFirst));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // prepare an updated empty view
        updateEmptyView();

    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    private class SlowAdapter extends CursorAdapter {

        private final int LAYOUT = R.layout.shows_row;

        private LayoutInflater mLayoutInflater;

        private OnClickListener mOnClickListener;

        private int mStarDrawableId;

        private int mStarZeroDrawableId;

        public SlowAdapter(Context context, Cursor c, int flags, int starDrawableResId,
                int starZeroDrawableId, OnClickListener listener) {
            super(context, c, flags);
            mLayoutInflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mStarDrawableId = starDrawableResId;
            mStarZeroDrawableId = starZeroDrawableId;
            mOnClickListener = listener;
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

            final ViewHolder viewHolder;

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(LAYOUT, null);

                viewHolder = new ViewHolder();
                viewHolder.name = (TextView) convertView.findViewById(R.id.seriesname);
                viewHolder.timeAndNetwork = (TextView) convertView
                        .findViewById(R.id.textViewShowsTimeAndNetwork);
                viewHolder.episode = (TextView) convertView
                        .findViewById(R.id.TextViewShowListNextEpisode);
                viewHolder.episodeTime = (TextView) convertView.findViewById(R.id.episodetime);
                viewHolder.poster = (ImageView) convertView.findViewById(R.id.showposter);
                viewHolder.favorited = (ImageView) convertView.findViewById(R.id.favoritedLabel);
                viewHolder.contextMenu = (ImageView) convertView
                        .findViewById(R.id.imageViewShowsContextMenu);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            // set text properties immediately
            viewHolder.name.setText(mCursor.getString(ShowsQuery.TITLE));

            // favorite toggle
            final String showId = mCursor.getString(ShowsQuery._ID);
            final boolean isFavorited = mCursor.getInt(ShowsQuery.FAVORITE) == 1;
            viewHolder.favorited.setImageResource(isFavorited ? mStarDrawableId
                    : mStarZeroDrawableId);
            viewHolder.favorited.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFavoriteShow(showId, !isFavorited);
                }
            });

            // next episode info
            String fieldValue = mCursor.getString(ShowsQuery.NEXTTEXT);
            if (fieldValue.length() == 0) {
                // show show status if there are currently no more
                // episodes
                int status = mCursor.getInt(ShowsQuery.STATUS);

                // Continuing == 1 and Ended == 0
                if (status == 1) {
                    viewHolder.episodeTime.setText(getString(R.string.show_isalive));
                } else if (status == 0) {
                    viewHolder.episodeTime.setText(getString(R.string.show_isnotalive));
                } else {
                    viewHolder.episodeTime.setText("");
                }
                viewHolder.episode.setText("");
            } else {
                viewHolder.episode.setText(fieldValue);
                fieldValue = mCursor.getString(ShowsQuery.NEXTAIRDATETEXT);
                viewHolder.episodeTime.setText(fieldValue);
            }

            // airday
            final String[] values = Utils.parseMillisecondsToTime(
                    mCursor.getLong(ShowsQuery.AIRSTIME),
                    mCursor.getString(ShowsQuery.AIRSDAYOFWEEK), mContext);
            if (getResources().getBoolean(R.bool.isLargeTablet)) {
                // network first, then time, one line
                viewHolder.timeAndNetwork.setText(mCursor.getString(ShowsQuery.NETWORK) + " / "
                        + values[1] + " " + values[0]);
            } else {
                // smaller screen, time first, network second line
                viewHolder.timeAndNetwork.setText(values[1] + " " + values[0] + "\n"
                        + mCursor.getString(ShowsQuery.NETWORK));
            }

            // set poster
            final String imagePath = mCursor.getString(ShowsQuery.POSTER);
            ImageProvider.getInstance(mContext).loadPosterThumb(viewHolder.poster, imagePath);

            // context menu
            viewHolder.contextMenu.setVisibility(View.VISIBLE);
            viewHolder.contextMenu.setOnClickListener(mOnClickListener);

            return convertView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // do nothing here
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mLayoutInflater.inflate(LAYOUT, parent, false);
        }
    }

    static class ViewHolder {

        public TextView name;

        public TextView timeAndNetwork;

        public TextView episode;

        public TextView episodeTime;

        public ImageView poster;

        public ImageView favorited;

        public ImageView contextMenu;

    }

    private interface ShowsQuery {

        String[] PROJECTION = {
                BaseColumns._ID, Shows.TITLE, Shows.NEXTTEXT, Shows.AIRSTIME, Shows.NETWORK,
                Shows.POSTER, Shows.AIRSDAYOFWEEK, Shows.STATUS, Shows.NEXTAIRDATETEXT,
                Shows.FAVORITE, Shows.NEXTEPISODE
        };

        int _ID = 0;

        int TITLE = 1;

        int NEXTTEXT = 2;

        int AIRSTIME = 3;

        int NETWORK = 4;

        int POSTER = 5;

        int AIRSDAYOFWEEK = 6;

        int STATUS = 7;

        int NEXTAIRDATETEXT = 8;

        int FAVORITE = 9;

        int NEXTEPISODE = 10;
    }

    private void onFavoriteShow(String showId, boolean isFavorite) {
        ContentValues values = new ContentValues();
        values.put(Shows.FAVORITE, isFavorite);
        getActivity().getContentResolver().update(
                Shows.buildShowUri(showId), values, null, null);

        Utils.runNotificationService(getActivity());

        Toast.makeText(getActivity(),
                getString(isFavorite ? R.string.favorited : R.string.unfavorited),
                Toast.LENGTH_SHORT)
                .show();

        fireTrackerEventContext(isFavorite ? "Favorite show" : "Unfavorite show");
    }

    private void showDeleteDialog(long showId) {
        FragmentManager fm = getFragmentManager();
        ConfirmDeleteDialogFragment deleteDialog = ConfirmDeleteDialogFragment.newInstance(String
                .valueOf(showId));
        deleteDialog.show(fm, "fragment_delete");
    }

    private final OnSharedPreferenceChangeListener mPrefsListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(AdvancedSettings.KEY_UPCOMING_LIMIT)) {
                getLoaderManager().restartLoader(ShowsFragment.LOADER_ID, null, ShowsFragment.this);
            }
        }
    };

    public void onEvent(FlagTaskCompletedEvent event) {
        if (isAdded()) {
            Utils.updateLatestEpisode(getActivity(), String.valueOf(event.mType.getShowTvdbId()));
        }
    }

    private static void fireTrackerEventAction(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Action Item", label, (long) 0);
    }

    private static void fireTrackerEventContext(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Context Item", label, (long) 0);
    }

}
