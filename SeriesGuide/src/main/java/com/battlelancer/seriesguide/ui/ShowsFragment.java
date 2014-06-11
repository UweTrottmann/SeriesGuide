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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.BaseShowsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.ShowsDistillationSettings;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.ui.dialogs.ConfirmDeleteDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListsDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FlagTask.FlagTaskCompletedEvent;
import com.battlelancer.seriesguide.util.ImageProvider;
import com.battlelancer.seriesguide.util.LatestEpisodeUpdateTask;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;

/**
 * Displays the list of shows in a users local library with sorting and filtering abilities. The
 * main view of the app.
 */
public class ShowsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener, OnClickListener {

    private static final String TAG = "Shows";

    private ShowsAdapter mAdapter;

    private GridView mGrid;

    private int mSortOrderId;

    private boolean mIsSortFavoritesFirst;

    private boolean mIsSortIgnoreArticles;

    private boolean mIsFilterFavorites;

    private boolean mIsFilterUnwatched;

    private boolean mIsFilterUpcoming;

    private boolean mIsFilterHidden;

    private Handler mHandler;

    public static ShowsFragment newInstance() {
        return new ShowsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
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
                getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null,
                        ShowsFragment.this);

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
        int resIdStar = Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                R.attr.drawableStar);
        int resIdStarZero = Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                R.attr.drawableStar0);
        mAdapter = new ShowsAdapter(getActivity(), null, 0, resIdStar, resIdStarZero);

        // setup grid view
        mGrid = (GridView) getView().findViewById(android.R.id.list);
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);

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
        mIsSortIgnoreArticles = DisplaySettings.isSortOrderIgnoringArticles(getActivity());
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

        boolean isLoaderExists = getLoaderManager().getLoader(ShowsActivity.SHOWS_LOADER_ID)
                != null;
        // create new loader or re-attach
        // call is necessary to keep scroll position on config change
        getLoaderManager().initLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);
        if (isLoaderExists) {
            // if re-attached to existing loader, restart it to
            // keep unwatched and upcoming shows from becoming stale
            getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        // avoid CPU activity
        schedulePeriodicDataRefresh(false);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
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
        menu.findItem(R.id.menu_action_shows_sort_ignore_articles)
                .setChecked(mIsSortIgnoreArticles);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isDrawerOpen();
        menu.findItem(R.id.menu_action_shows_add).setVisible(!isDrawerOpen);
        MenuItem filter = menu.findItem(R.id.menu_action_shows_filter);
        filter.setVisible(!isDrawerOpen);
        filter.setIcon(mIsFilterFavorites || mIsFilterUnwatched || mIsFilterUpcoming
                || mIsFilterHidden ?
                R.drawable.ic_action_filter_selected : R.drawable.ic_action_filter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_add) {
            startActivity(new Intent(getActivity(), AddActivity.class));
            getActivity().overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            return true;
        } else if (itemId == R.id.menu_action_shows_filter) {
            fireTrackerEventAction("Filter shows");
            // did not handle here
            return super.onOptionsItemSelected(item);
        } else if (itemId == R.id.menu_action_shows_sort) {
            fireTrackerEventAction("Sort shows");
            // did not handle here
            return super.onOptionsItemSelected(item);
        } else if (itemId == R.id.menu_action_shows_filter_favorites) {
            mIsFilterFavorites = !mIsFilterFavorites;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_FAVORITES, mIsFilterFavorites,
                    item);

            fireTrackerEventAction("Filter Favorites");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_unwatched) {
            mIsFilterUnwatched = !mIsFilterUnwatched;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, mIsFilterUnwatched,
                    item);

            fireTrackerEventAction("Filter Unwatched");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_upcoming) {
            mIsFilterUpcoming = !mIsFilterUpcoming;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UPCOMING, mIsFilterUpcoming,
                    item);

            fireTrackerEventAction("Filter Upcoming");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_hidden) {
            mIsFilterHidden = !mIsFilterHidden;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_HIDDEN, mIsFilterHidden, item);

            fireTrackerEventAction("Filter Hidden");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_remove) {
            mIsFilterFavorites = false;
            mIsFilterUnwatched = false;
            mIsFilterUpcoming = false;
            mIsFilterHidden = false;

            // already start loading, do not need to wait on saving prefs
            getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);

            // update menu item state, then save at last
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_FAVORITES, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UPCOMING, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_HIDDEN, false)
                    .commit();
            // refresh filter icon state
            getActivity().supportInvalidateOptionsMenu();

            fireTrackerEventAction("Filter Removed");
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
        } else if (itemId == R.id.menu_action_shows_sort_ignore_articles) {
            mIsSortIgnoreArticles = !mIsSortIgnoreArticles;
            changeSortOrFilter(DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                    mIsSortIgnoreArticles, item);

            fireTrackerEventAction("Sort Ignore Articles");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void changeSortOrFilter(String key, boolean state, MenuItem item) {
        // already start loading, do not need to wait on saving prefs
        getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);

        // save new setting
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putBoolean(key, state).commit();

        // refresh filter icon state
        getActivity().supportInvalidateOptionsMenu();
    }

    private void changeSort() {
        // already start loading, do not need to wait on saving prefs
        getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);

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
            selection.append(Shows.NEXTAIRDATEMS).append("!=")
                    .append(DBUtils.UNKNOWN_NEXT_RELEASE_DATE);

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

        // keep unwatched and upcoming shows from becoming stale
        schedulePeriodicDataRefresh(true);

        return new CursorLoader(getActivity(), Shows.CONTENT_URI, ShowsQuery.PROJECTION,
                selection.toString(), null,
                ShowsDistillationSettings.getSortQuery(mSortOrderId, mIsSortFavoritesFirst,
                        mIsSortIgnoreArticles)
        );
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
                getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null,
                        ShowsFragment.this);
            }
        }
    };

    private class ShowsAdapter extends BaseShowsAdapter {

        private int mStarDrawableId;

        private int mStarZeroDrawableId;

        public ShowsAdapter(Context context, Cursor c, int flags, int starDrawableResId,
                int starZeroDrawableId) {
            super(context, c, flags);
            mStarDrawableId = starDrawableResId;
            mStarZeroDrawableId = starZeroDrawableId;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            BaseShowsAdapter.ViewHolder viewHolder = (BaseShowsAdapter.ViewHolder) view.getTag();

            // set text properties immediately
            viewHolder.name.setText(cursor.getString(ShowsQuery.TITLE));

            // favorite toggle
            final int showId = cursor.getInt(ShowsQuery._ID);
            final boolean isFavorited = cursor.getInt(ShowsQuery.FAVORITE) == 1;
            viewHolder.favorited.setImageResource(isFavorited ? mStarDrawableId
                    : mStarZeroDrawableId);
            viewHolder.favorited.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onFavoriteShow(showId, !isFavorited);
                }
            });

            // next episode info
            String fieldValue = cursor.getString(ShowsQuery.NEXTTEXT);
            if (TextUtils.isEmpty(fieldValue)) {
                // show show status if there are currently no more
                // episodes
                int status = cursor.getInt(ShowsQuery.STATUS);

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
                fieldValue = cursor.getString(ShowsQuery.NEXTAIRDATETEXT);
                viewHolder.episodeTime.setText(fieldValue);
            }

            // network, day and time
            String[] values = TimeTools.formatToShowReleaseTimeAndDay(context,
                    cursor.getLong(ShowsQuery.RELEASE_TIME),
                    cursor.getString(ShowsQuery.RELEASE_COUNTRY),
                    cursor.getString(ShowsQuery.RELEASE_DAY));
            // one line: 'Network / Tue 08:00 PM'
            viewHolder.timeAndNetwork.setText(cursor.getString(ShowsQuery.NETWORK) + " / "
                    + values[1] + " " + values[0]);

            // set poster
            final String imagePath = cursor.getString(ShowsQuery.POSTER);
            ImageProvider.getInstance(context).loadPosterThumb(viewHolder.poster, imagePath);

            // context menu
            viewHolder.contextMenu.setVisibility(View.VISIBLE);
            final boolean isHidden = DBUtils.restoreBooleanFromInt(
                    cursor.getInt(ShowsQuery.HIDDEN));
            final int episodeTvdbId = cursor.getInt(ShowsQuery.NEXTEPISODE);
            viewHolder.contextMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.inflate(R.menu.shows_popup_menu);

                    // show/hide some menu items depending on show properties
                    Menu menu = popupMenu.getMenu();
                    menu.findItem(R.id.menu_action_shows_favorites_add).setVisible(!isFavorited);
                    menu.findItem(R.id.menu_action_shows_favorites_remove).setVisible(isFavorited);
                    menu.findItem(R.id.menu_action_shows_hide).setVisible(!isHidden);
                    menu.findItem(R.id.menu_action_shows_unhide).setVisible(isHidden);

                    popupMenu.setOnMenuItemClickListener(
                            new PopupMenuItemClickListener(showId, episodeTvdbId));
                    popupMenu.show();
                }
            });
        }

        private class PopupMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {

            private final int mShowTvdbId;
            private final int mEpisodeTvdbId;

            public PopupMenuItemClickListener(int showTvdbId, int episodeTvdbId) {
                mShowTvdbId = showTvdbId;
                mEpisodeTvdbId = episodeTvdbId;
            }

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_action_shows_watched_next: {
                        DBUtils.markNextEpisode(getActivity(), mShowTvdbId, mEpisodeTvdbId);
                        fireTrackerEventContext("Mark next episode");
                        return true;
                    }
                    case R.id.menu_action_shows_favorites_add: {
                        onFavoriteShow(mShowTvdbId, true);
                        return true;
                    }
                    case R.id.menu_action_shows_favorites_remove: {
                        onFavoriteShow(mShowTvdbId, false);
                        return true;
                    }
                    case R.id.menu_action_shows_hide: {
                        ShowTools.get(getActivity()).storeIsHidden(mShowTvdbId, true);
                        fireTrackerEventContext("Hide show");
                        return true;
                    }
                    case R.id.menu_action_shows_unhide: {
                        ShowTools.get(getActivity()).storeIsHidden(mShowTvdbId, false);
                        fireTrackerEventContext("Unhide show");
                        return true;
                    }
                    case R.id.menu_action_shows_manage_lists: {
                        ListsDialogFragment.showListsDialog(mShowTvdbId, ListItemTypes.SHOW,
                                getFragmentManager());
                        fireTrackerEventContext("Manage lists");
                        return true;
                    }
                    case R.id.menu_action_shows_update: {
                        SgSyncAdapter.requestSyncImmediate(getActivity(),
                                SgSyncAdapter.SyncType.SINGLE, mShowTvdbId, true);
                        fireTrackerEventContext("Update show");
                        return true;
                    }
                    case R.id.menu_action_shows_remove: {
                        if (!SgSyncAdapter.isSyncActive(getActivity(), true)) {
                            showDeleteDialog(mShowTvdbId);
                        }
                        fireTrackerEventContext("Delete show");
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private interface ShowsQuery {

        String[] PROJECTION = {
                BaseColumns._ID, Shows.TITLE, Shows.NEXTTEXT, Shows.AIRSTIME, Shows.NETWORK,
                Shows.POSTER, Shows.AIRSDAYOFWEEK, Shows.STATUS, Shows.NEXTAIRDATETEXT,
                Shows.FAVORITE, Shows.NEXTEPISODE, Shows.RELEASE_COUNTRY, Shows.HIDDEN
        };

        int _ID = 0;

        int TITLE = 1;

        int NEXTTEXT = 2;

        int RELEASE_TIME = 3;

        int NETWORK = 4;

        int POSTER = 5;

        int RELEASE_DAY = 6;

        int STATUS = 7;

        int NEXTAIRDATETEXT = 8;

        int FAVORITE = 9;

        int NEXTEPISODE = 10;

        int RELEASE_COUNTRY = 11;

        int HIDDEN = 12;
    }

    private void onFavoriteShow(int showTvdbId, boolean isFavorite) {
        // store new value
        ShowTools.get(getActivity()).storeIsFavorite(showTvdbId, isFavorite);

        // favoriting makes show eligible for notifications
        Utils.runNotificationService(getActivity());

        fireTrackerEventContext(isFavorite ? "Favorite show" : "Unfavorite show");
    }

    private void showDeleteDialog(long showId) {
        FragmentManager fm = getFragmentManager();
        ConfirmDeleteDialogFragment deleteDialog = ConfirmDeleteDialogFragment
                .newInstance((int) showId);
        deleteDialog.show(fm, "fragment_delete");
    }

    private final OnSharedPreferenceChangeListener mPrefsListener
            = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(AdvancedSettings.KEY_UPCOMING_LIMIT)) {
                getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null,
                        ShowsFragment.this);
            }
        }
    };

    public void onEvent(FlagTaskCompletedEvent event) {
        if (isAdded()) {
            Utils.executeInOrder(new LatestEpisodeUpdateTask(getActivity()),
                    event.mType.getShowTvdbId());
        }
    }

    private void fireTrackerEventAction(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    private void fireTrackerEventContext(String label) {
        Utils.trackContextMenu(getActivity(), TAG, label);
    }
}
