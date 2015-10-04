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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewCompat;
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
import com.battlelancer.seriesguide.adapters.ShowsAdapter;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.ShowsDistillationSettings;
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.FabAbsListViewScrollDetector;
import com.battlelancer.seriesguide.util.ShowMenuItemClickListener;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;

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
        View v = inflater.inflate(R.layout.fragment_shows, container, false);

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
        mAdapter = new ShowsAdapter(getActivity(), onShowMenuClickListener);

        // setup grid view
        mGrid = (GridView) getView().findViewById(android.R.id.list);
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(mGrid, AndroidUtils.isLollipopOrHigher());
        mGrid.setAdapter(mAdapter);
        mGrid.setOnItemClickListener(this);

        // hide floating action button when scrolling shows
        FloatingActionButton buttonAddShow = (FloatingActionButton) getActivity().findViewById(
                R.id.buttonShowsAdd);
        mGrid.setOnScrollListener(new FabAbsListViewScrollDetector(buttonAddShow));

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

        View emptyView;
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
    }

    @Override
    public void onPause() {
        super.onPause();

        // avoid CPU activity
        schedulePeriodicDataRefresh(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(mPrefsListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.shows_menu, menu);

        // set filter icon state
        menu.findItem(R.id.menu_action_shows_filter)
                .setIcon(mIsFilterFavorites || mIsFilterUnwatched || mIsFilterUpcoming
                        || mIsFilterHidden ?
                        R.drawable.ic_action_filter_selected : R.drawable.ic_action_filter);

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
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_add) {
            startActivity(new Intent(getActivity(), AddActivity.class));
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
        } else if (itemId == R.id.menu_action_shows_filter_upcoming_range) {
            // yes, converting back to a string for comparison
            String upcomingLimit = String.valueOf(
                    AdvancedSettings.getUpcomingLimitInDays(getActivity()));
            String[] filterRanges = getResources().getStringArray(R.array.upcominglimitData);
            int selectedIndex = 0;
            for (int i = 0, filterRangesLength = filterRanges.length; i < filterRangesLength; i++) {
                String range = filterRanges[i];
                if (upcomingLimit.equals(range)) {
                    selectedIndex = i;
                    break;
                }
            }

            SingleChoiceDialogFragment upcomingRangeDialog = SingleChoiceDialogFragment.newInstance(
                    R.array.upcominglimit,
                    R.array.upcominglimitData,
                    selectedIndex,
                    AdvancedSettings.KEY_UPCOMING_LIMIT,
                    R.string.pref_upcominglimit);
            upcomingRangeDialog.show(getFragmentManager(), "upcomingRangeDialog");
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
        } else if (itemId == R.id.menu_action_shows_sort_network) {
            if (mSortOrderId == ShowsDistillationSettings.ShowsSortOrder.NETWORK_ID) {
                mSortOrderId = ShowsDistillationSettings.ShowsSortOrder.NETWORK_REVERSE_ID;
            } else {
                mSortOrderId = ShowsDistillationSettings.ShowsSortOrder.NETWORK_ID;
            }
            changeSort();

            fireTrackerEventAction("Sort Network");
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

        ActivityCompat.startActivity(getActivity(), i,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
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

        return new CursorLoader(getActivity(), Shows.CONTENT_URI, ShowsAdapter.Query.PROJECTION,
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

    /**
     * Periodically restart the shows loader.
     *
     * <p>Some changes to the displayed data are not based on actual (detectable) changes to the
     * underlying data, but because time has passed (e.g. relative time displays, release time has
     * passed).
     */
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

    private BaseShowsAdapter.OnContextMenuClickListener onShowMenuClickListener
            = new BaseShowsAdapter.OnContextMenuClickListener() {
        @Override
        public void onClick(View view, BaseShowsAdapter.ShowViewHolder viewHolder) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.shows_popup_menu);

            // show/hide some menu items depending on show properties
            Menu menu = popupMenu.getMenu();
            menu.findItem(R.id.menu_action_shows_favorites_add)
                    .setVisible(!viewHolder.isFavorited);
            menu.findItem(R.id.menu_action_shows_favorites_remove)
                    .setVisible(viewHolder.isFavorited);
            menu.findItem(R.id.menu_action_shows_hide).setVisible(!viewHolder.isHidden);
            menu.findItem(R.id.menu_action_shows_unhide).setVisible(viewHolder.isHidden);

            popupMenu.setOnMenuItemClickListener(
                    new ShowMenuItemClickListener(getActivity(), getFragmentManager(),
                            viewHolder.showTvdbId, viewHolder.episodeTvdbId, TAG));
            popupMenu.show();
        }
    };

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

    private void fireTrackerEventAction(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }
}
