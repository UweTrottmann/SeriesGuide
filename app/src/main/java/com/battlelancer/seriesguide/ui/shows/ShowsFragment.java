package com.battlelancer.seriesguide.ui.shows;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import android.widget.Button;
import android.widget.PopupMenu;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings.ShowsSortOrder;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Displays the list of shows in a users local library with sorting and filtering abilities. The
 * main view of the app.
 */
public class ShowsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener, OnClickListener {

    private static final String TAG = "Shows";
    private static final String TAG_FIRST_RUN = "First Run";

    private int sortOrderId;
    private boolean isSortFavoritesFirst;
    private boolean isSortIgnoreArticles;
    private boolean isFilterFavorites;
    private boolean isFilterUnwatched;
    private boolean isFilterUpcoming;
    private boolean isFilterHidden;

    private ShowsAdapter adapter;
    private HeaderGridView gridView;
    private Button emptyView;
    private Button emptyViewFilter;

    private Handler handler;

    public static ShowsFragment newInstance() {
        return new ShowsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shows, container, false);

        gridView = v.findViewById(android.R.id.list);
        emptyView = v.findViewById(R.id.emptyViewShows);
        ViewTools.setVectorIconTop(getActivity().getTheme(), emptyView,
                R.drawable.ic_add_white_24dp);
        emptyView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityAddShows();
            }
        });
        emptyViewFilter = v.findViewById(R.id.emptyViewShowsFilter);
        ViewTools.setVectorIconTop(getActivity().getTheme(), emptyViewFilter,
                R.drawable.ic_filter_white_24dp);
        emptyViewFilter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                isFilterFavorites = isFilterUnwatched = isFilterUpcoming = isFilterHidden
                        = false;

                // already start loading, do not need to wait on saving prefs
                getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null,
                        ShowsFragment.this);

                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_FAVORITES, false)
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, false)
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_UPCOMING, false)
                        .putBoolean(ShowsDistillationSettings.KEY_FILTER_HIDDEN, false)
                        .apply();

                // refresh filter menu check box states
                getActivity().invalidateOptionsMenu();
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
        adapter = new ShowsAdapter(getActivity(), onShowMenuClickListener);

        // setup grid view
        // enable app bar scrolling out of view only on L or higher
        ViewCompat.setNestedScrollingEnabled(gridView, AndroidUtils.isLollipopOrHigher());
        if (!FirstRunView.hasSeenFirstRunFragment(getContext())) {
            FirstRunView headerView = (FirstRunView) getActivity().getLayoutInflater()
                    .inflate(R.layout.item_first_run, gridView, false);
            gridView.addHeaderView(headerView);
        }
        gridView.setAdapter(adapter);
        gridView.setOnItemClickListener(this);

        // hide floating action button when scrolling shows
        FloatingActionButton buttonAddShow = getActivity().findViewById(R.id.buttonShowsAdd);
        gridView.setOnScrollListener(new FabAbsListViewScrollDetector(buttonAddShow));

        // listen for some settings changes
        PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(onPreferenceChangeListener);

        setHasOptionsMenu(true);
    }

    private void getSortAndFilterSettings() {
        isFilterFavorites = ShowsDistillationSettings.isFilteringFavorites(getActivity());
        isFilterUnwatched = ShowsDistillationSettings.isFilteringUnwatched(getActivity());
        isFilterUpcoming = ShowsDistillationSettings.isFilteringUpcoming(getActivity());
        isFilterHidden = ShowsDistillationSettings.isFilteringHidden(getActivity());

        sortOrderId = ShowsDistillationSettings.getSortOrderId(getActivity());
        isSortFavoritesFirst = ShowsDistillationSettings.isSortFavoritesFirst(getActivity());
        isSortIgnoreArticles = DisplaySettings.isSortOrderIgnoringArticles(getActivity());
    }

    private void updateEmptyView() {
        if (getView() == null) {
            return;
        }

        View oldEmptyView = gridView.getEmptyView();

        View emptyView;
        if (isFilterFavorites || isFilterUnwatched || isFilterUpcoming || isFilterHidden) {
            emptyView = emptyViewFilter;
        } else {
            emptyView = this.emptyView;
        }

        if (oldEmptyView != null) {
            oldEmptyView.setVisibility(View.GONE);
        }

        if (emptyView != null) {
            gridView.setEmptyView(emptyView);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
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
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.unregisterOnSharedPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.shows_menu, menu);

        // set filter icon state
        menu.findItem(R.id.menu_action_shows_filter)
                .setIcon(isFilterFavorites || isFilterUnwatched || isFilterUpcoming
                        || isFilterHidden ?
                        R.drawable.ic_filter_selected_white_24dp : R.drawable.ic_filter_white_24dp);

        // set filter check box states
        menu.findItem(R.id.menu_action_shows_filter_favorites)
                .setChecked(isFilterFavorites);
        menu.findItem(R.id.menu_action_shows_filter_unwatched)
                .setChecked(isFilterUnwatched);
        menu.findItem(R.id.menu_action_shows_filter_upcoming)
                .setChecked(isFilterUpcoming);
        menu.findItem(R.id.menu_action_shows_filter_hidden)
                .setChecked(isFilterHidden);

        // set current sort order and check box states
        MenuItem sortTitleItem = menu.findItem(R.id.menu_action_shows_sort_title);
        sortTitleItem.setTitle(R.string.action_shows_sort_title);
        MenuItem sortLatestItem = menu.findItem(R.id.menu_action_shows_sort_latest_episode);
        sortLatestItem.setTitle(R.string.action_shows_sort_latest_episode);
        MenuItem sortOldestItem = menu.findItem(R.id.menu_action_shows_sort_oldest_episode);
        sortOldestItem.setTitle(R.string.action_shows_sort_oldest_episode);
        MenuItem lastWatchedItem = menu.findItem(R.id.menu_action_shows_sort_last_watched);
        lastWatchedItem.setTitle(R.string.action_shows_sort_last_watched);
        MenuItem remainingItem = menu.findItem(R.id.menu_action_shows_sort_remaining);
        remainingItem.setTitle(R.string.action_shows_sort_remaining);
        if (sortOrderId == ShowsSortOrder.TITLE_ID) {
            ViewTools.setMenuItemActiveString(sortTitleItem);
        } else if (sortOrderId == ShowsSortOrder.LATEST_EPISODE_ID) {
            ViewTools.setMenuItemActiveString(sortLatestItem);
        } else if (sortOrderId == ShowsSortOrder.OLDEST_EPISODE_ID) {
            ViewTools.setMenuItemActiveString(sortOldestItem);
        } else if (sortOrderId == ShowsSortOrder.LAST_WATCHED_ID) {
            ViewTools.setMenuItemActiveString(lastWatchedItem);
        } else if (sortOrderId == ShowsSortOrder.LEAST_REMAINING_EPISODES_ID) {
            ViewTools.setMenuItemActiveString(remainingItem);
        }
        menu.findItem(R.id.menu_action_shows_sort_favorites)
                .setChecked(isSortFavoritesFirst);
        menu.findItem(R.id.menu_action_shows_sort_ignore_articles)
                .setChecked(isSortIgnoreArticles);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_add) {
            startActivityAddShows();
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_favorites) {
            isFilterFavorites = !isFilterFavorites;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_FAVORITES, isFilterFavorites
            );

            Utils.trackAction(getActivity(), TAG, "Filter Favorites");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_unwatched) {
            isFilterUnwatched = !isFilterUnwatched;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, isFilterUnwatched
            );

            Utils.trackAction(getActivity(), TAG, "Filter Unwatched");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_upcoming) {
            isFilterUpcoming = !isFilterUpcoming;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UPCOMING, isFilterUpcoming
            );

            Utils.trackAction(getActivity(), TAG, "Filter Upcoming");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_hidden) {
            isFilterHidden = !isFilterHidden;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_HIDDEN, isFilterHidden);

            Utils.trackAction(getActivity(), TAG, "Filter Hidden");
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_remove) {
            isFilterFavorites = false;
            isFilterUnwatched = false;
            isFilterUpcoming = false;
            isFilterHidden = false;

            // already start loading, do not need to wait on saving prefs
            getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);

            // update menu item state, then save at last
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_FAVORITES, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UPCOMING, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_HIDDEN, false)
                    .apply();
            // refresh filter icon state
            getActivity().invalidateOptionsMenu();

            Utils.trackAction(getActivity(), TAG, "Filter Removed");
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
            sortOrderId = ShowsSortOrder.TITLE_ID;
            changeSort();
            Utils.trackAction(getActivity(), TAG, "Sort Title");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_latest_episode) {
            sortOrderId = ShowsSortOrder.LATEST_EPISODE_ID;
            changeSort();
            Utils.trackAction(getActivity(), TAG, "Sort Episode (latest)");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_oldest_episode) {
            sortOrderId = ShowsSortOrder.OLDEST_EPISODE_ID;
            changeSort();
            Utils.trackAction(getActivity(), TAG, "Sort Episode (oldest)");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_last_watched) {
            sortOrderId = ShowsSortOrder.LAST_WATCHED_ID;
            changeSort();
            Utils.trackAction(getActivity(), TAG, "Sort Last watched");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_remaining) {
            sortOrderId = ShowsSortOrder.LEAST_REMAINING_EPISODES_ID;
            changeSort();
            Utils.trackAction(getActivity(), TAG, "Sort Remaining episodes");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_favorites) {
            isSortFavoritesFirst = !isSortFavoritesFirst;
            changeSortOrFilter(ShowsDistillationSettings.KEY_SORT_FAVORITES_FIRST,
                    isSortFavoritesFirst);

            Utils.trackAction(getActivity(), TAG, "Sort Favorites");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_ignore_articles) {
            isSortIgnoreArticles = !isSortIgnoreArticles;
            changeSortOrFilter(DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                    isSortIgnoreArticles);
            // refresh all list widgets
            ListWidgetProvider.notifyDataChanged(getContext());

            Utils.trackAction(getActivity(), TAG, "Sort Ignore Articles");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void changeSortOrFilter(String key, boolean state) {
        // already start loading, do not need to wait on saving prefs
        getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);

        // save new setting
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putBoolean(key, state).apply();

        // refresh filter icon state
        getActivity().invalidateOptionsMenu();
    }

    private void changeSort() {
        // already start loading, do not need to wait on saving prefs
        getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null, this);

        // save new sort order to preferences
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(ShowsDistillationSettings.KEY_SORT_ORDER, sortOrderId).apply();

        // refresh menu state to indicate current order
        getActivity().invalidateOptionsMenu();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventFirstRunButton(FirstRunView.ButtonEvent event) {
        switch (event.type) {
            case FirstRunView.ButtonType.ADD_SHOW: {
                startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH));
                Utils.trackClick(getActivity(), TAG_FIRST_RUN, "Add show");
                break;
            }
            case FirstRunView.ButtonType.SIGN_IN: {
                ((BaseNavDrawerActivity) getActivity()).openNavDrawer();
                Utils.trackClick(getActivity(), TAG_FIRST_RUN, "Sign in");
                break;
            }
            case FirstRunView.ButtonType.RESTORE_BACKUP: {
                startActivity(new Intent(getActivity(), DataLiberationActivity.class));
                Utils.trackClick(getActivity(), TAG_FIRST_RUN, "Restore backup");
                break;
            }
            case FirstRunView.ButtonType.DISMISS: {
                if (gridView != null) {
                    gridView.removeHeaderView(event.firstRunView);
                    Utils.trackClick(getActivity(), TAG_FIRST_RUN, "Dismiss");
                }
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if (event.position == ShowsActivity.InitBundle.INDEX_TAB_SHOWS) {
            gridView.smoothScrollToPosition(0);
        }
    }

    @Override
    public void onClick(View v) {
        getActivity().openContextMenu(v);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // display overview for this show
        Intent intent = OverviewActivity.intentShow(getContext(), (int) id);
        ActivityCompat.startActivity(getActivity(), intent,
                ActivityOptionsCompat
                        .makeScaleUpAnimation(view, 0, 0, view.getWidth(), view.getHeight())
                        .toBundle()
        );
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        StringBuilder selection = new StringBuilder();

        // create temporary copies
        final boolean isFilterFavorites = this.isFilterFavorites;
        final boolean isFilterUnwatched = this.isFilterUnwatched;
        final boolean isFilterUpcoming = this.isFilterUpcoming;
        final boolean isFilterHidden = this.isFilterHidden;

        // restrict to favorites?
        if (isFilterFavorites) {
            selection.append(Shows.FAVORITE).append("=1");
        }

        final long timeInAnHour = TimeTools.getCurrentTime(getContext()) + DateUtils.HOUR_IN_MILLIS;

        // restrict to shows with a next episode?
        if (isFilterUnwatched) {
            if (selection.length() != 0) {
                selection.append(" AND ");
            }
            selection.append(Shows.SELECTION_WITH_RELEASED_NEXT_EPISODE);

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
                ShowsDistillationSettings.getSortQuery(sortOrderId, isSortFavoritesFirst,
                        isSortIgnoreArticles)
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        adapter.swapCursor(data);

        // prepare an updated empty view
        updateEmptyView();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        adapter.swapCursor(null);
    }

    /**
     * Periodically restart the shows loader.
     *
     * <p>Some changes to the displayed data are not based on actual (detectable) changes to the
     * underlying data, but because time has passed (e.g. relative time displays, release time has
     * passed).
     */
    private void schedulePeriodicDataRefresh(boolean enableRefresh) {
        if (handler == null) {
            handler = new Handler();
        }
        handler.removeCallbacks(dataRefreshRunnable);
        if (enableRefresh) {
            handler.postDelayed(dataRefreshRunnable, 5 * DateUtils.MINUTE_IN_MILLIS);
        }
    }

    private Runnable dataRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (isAdded()) {
                getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null,
                        ShowsFragment.this);
            }
        }
    };

    private void startActivityAddShows() {
        startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH));
    }

    private BaseShowsAdapter.OnItemClickListener onShowMenuClickListener
            = new BaseShowsAdapter.OnItemClickListener() {
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
                    new ShowMenuItemClickListener(getContext(), getFragmentManager(),
                            viewHolder.showTvdbId, viewHolder.episodeTvdbId, TAG));
            popupMenu.show();
        }

        @Override
        public void onFavoriteClick(int showTvdbId, boolean isFavorite) {
            SgApp.getServicesComponent(getContext()).showTools()
                    .storeIsFavorite(showTvdbId, isFavorite);
        }
    };

    private final OnSharedPreferenceChangeListener onPreferenceChangeListener
            = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(AdvancedSettings.KEY_UPCOMING_LIMIT)) {
                getLoaderManager().restartLoader(ShowsActivity.SHOWS_LOADER_ID, null,
                        ShowsFragment.this);
                // refresh all list widgets
                ListWidgetProvider.notifyDataChanged(getContext());
            }
        }
    };
}
