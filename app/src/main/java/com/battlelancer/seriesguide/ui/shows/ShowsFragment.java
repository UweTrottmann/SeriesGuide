package com.battlelancer.seriesguide.ui.shows;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.dialogs.SingleChoiceDialogFragment;
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager;
import com.battlelancer.seriesguide.ui.shows.ShowsDistillationSettings.ShowsSortOrder;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.TabClickEvent;
import com.battlelancer.seriesguide.util.ViewTools;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

/**
 * Displays the list of shows in a users local library with sorting and filtering abilities. The
 * main view of the app.
 */
public class ShowsFragment extends Fragment {

    private int sortOrderId;
    private boolean isSortFavoritesFirst;
    private boolean isSortIgnoreArticles;
    private boolean isFilterFavorites;
    private boolean isFilterUnwatched;
    private boolean isFilterUpcoming;
    private boolean isFilterHidden;

    private ShowsAdapter adapter;
    private RecyclerView recyclerView;
    private Button emptyView;
    private Button emptyViewFilter;

    private Handler handler;
    private ShowsViewModel model;

    public static ShowsFragment newInstance() {
        return new ShowsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_shows, container, false);

        recyclerView = v.findViewById(R.id.recyclerViewShows);
        emptyView = v.findViewById(R.id.emptyViewShows);
        ViewTools.setVectorIconTop(getActivity().getTheme(), emptyView,
                R.drawable.ic_add_white_24dp);
        emptyView.setOnClickListener(view -> startActivityAddShows());
        emptyViewFilter = v.findViewById(R.id.emptyViewShowsFilter);
        ViewTools.setVectorIconTop(getActivity().getTheme(), emptyViewFilter,
                R.drawable.ic_filter_white_24dp);
        emptyViewFilter.setOnClickListener(view -> {
            isFilterFavorites = isFilterUnwatched = isFilterUpcoming = isFilterHidden = false;

            // already start loading, do not need to wait on saving prefs
            updateShowsQuery();

            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_FAVORITES, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UPCOMING, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_HIDDEN, false)
                    .apply();

            // refresh filter menu check box states
            getActivity().invalidateOptionsMenu();
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView.setHasFixedSize(true);
        AutoGridLayoutManager layoutManager = new AutoGridLayoutManager(getContext(),
                R.dimen.showgrid_columnWidth, 1, 1);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == ShowsAdapter.VIEW_TYPE_FIRST_RUN) {
                    return layoutManager.getSpanCount();
                } else {
                    return 1;
                }
            }
        });
        recyclerView.setLayoutManager(layoutManager);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get settings
        getSortAndFilterSettings();

        // prepare view adapter
        adapter = new ShowsAdapter(getContext(), onItemClickListener);
        if (!FirstRunView.hasSeenFirstRunFragment(getContext())) {
            adapter.setDisplayFirstRunHeader(true);
        }
        recyclerView.setAdapter(adapter);

        model = ViewModelProviders.of(this).get(ShowsViewModel.class);
        model.getShowItemsLiveData().observe(this, showItems -> {
            adapter.submitList(showItems);
            // note: use adapter count, may display header
            boolean isEmpty = adapter.getItemCount() == 0;
            updateEmptyView(isEmpty);
        });
        updateShowsQuery();

        // hide floating action button when scrolling shows
        FloatingActionButton buttonAddShow = getActivity().findViewById(R.id.buttonShowsAdd);
        recyclerView.addOnScrollListener(new FabRecyclerViewScrollDetector(buttonAddShow));

        // listen for some settings changes
        PreferenceManager
                .getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(onPreferenceChangeListener);

        setHasOptionsMenu(true);
    }

    private void updateShowsQuery() {
        model.updateQuery(isFilterFavorites, isFilterUnwatched, isFilterUpcoming, isFilterHidden,
                ShowsDistillationSettings.getSortQuery(sortOrderId, isSortFavoritesFirst,
                        isSortIgnoreArticles));
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

    private void updateEmptyView(boolean isEmpty) {
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) {
            if (isFilterFavorites || isFilterUnwatched || isFilterUpcoming || isFilterHidden) {
                emptyViewFilter.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.VISIBLE);
                emptyViewFilter.setVisibility(View.GONE);
            }
        } else {
            emptyView.setVisibility(View.GONE);
            emptyViewFilter.setVisibility(View.GONE);
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

        // keep unwatched and upcoming shows from becoming stale
        schedulePeriodicDataRefresh(true);
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
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_unwatched) {
            isFilterUnwatched = !isFilterUnwatched;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, isFilterUnwatched
            );
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_upcoming) {
            isFilterUpcoming = !isFilterUpcoming;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_UPCOMING, isFilterUpcoming
            );
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_hidden) {
            isFilterHidden = !isFilterHidden;
            changeSortOrFilter(ShowsDistillationSettings.KEY_FILTER_HIDDEN, isFilterHidden);
            return true;
        } else if (itemId == R.id.menu_action_shows_filter_remove) {
            isFilterFavorites = false;
            isFilterUnwatched = false;
            isFilterUpcoming = false;
            isFilterHidden = false;

            // already start loading, do not need to wait on saving prefs
            updateShowsQuery();

            // update menu item state, then save at last
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_FAVORITES, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UNWATCHED, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_UPCOMING, false)
                    .putBoolean(ShowsDistillationSettings.KEY_FILTER_HIDDEN, false)
                    .apply();
            // refresh filter icon state
            getActivity().invalidateOptionsMenu();
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

            SingleChoiceDialogFragment.show(getFragmentManager(),
                    R.array.upcominglimit,
                    R.array.upcominglimitData,
                    selectedIndex,
                    AdvancedSettings.KEY_UPCOMING_LIMIT,
                    R.string.pref_upcominglimit,
                    "upcomingRangeDialog");
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_title) {
            sortOrderId = ShowsSortOrder.TITLE_ID;
            changeSort();
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_latest_episode) {
            sortOrderId = ShowsSortOrder.LATEST_EPISODE_ID;
            changeSort();
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_oldest_episode) {
            sortOrderId = ShowsSortOrder.OLDEST_EPISODE_ID;
            changeSort();
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_last_watched) {
            sortOrderId = ShowsSortOrder.LAST_WATCHED_ID;
            changeSort();
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_remaining) {
            sortOrderId = ShowsSortOrder.LEAST_REMAINING_EPISODES_ID;
            changeSort();
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_favorites) {
            isSortFavoritesFirst = !isSortFavoritesFirst;
            changeSortOrFilter(ShowsDistillationSettings.KEY_SORT_FAVORITES_FIRST,
                    isSortFavoritesFirst);
            return true;
        } else if (itemId == R.id.menu_action_shows_sort_ignore_articles) {
            isSortIgnoreArticles = !isSortIgnoreArticles;
            changeSortOrFilter(DisplaySettings.KEY_SORT_IGNORE_ARTICLE,
                    isSortIgnoreArticles);
            // refresh all list widgets
            ListWidgetProvider.notifyDataChanged(getContext());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void changeSortOrFilter(String key, boolean state) {
        // already start loading, do not need to wait on saving prefs
        updateShowsQuery();

        // save new setting
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putBoolean(key, state).apply();

        // refresh filter icon state
        getActivity().invalidateOptionsMenu();
    }

    private void changeSort() {
        // already start loading, do not need to wait on saving prefs
        updateShowsQuery();

        // save new sort order to preferences
        PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putInt(ShowsDistillationSettings.KEY_SORT_ORDER, sortOrderId).apply();

        // refresh menu state to indicate current order
        getActivity().invalidateOptionsMenu();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventFirstRunButton(FirstRunView.ButtonEvent event) {
        switch (event.getType()) {
            case ADD_SHOW: {
                startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH));
                break;
            }
            case SIGN_IN: {
                ((BaseNavDrawerActivity) getActivity()).openNavDrawer();
                break;
            }
            case RESTORE_BACKUP: {
                startActivity(new Intent(getActivity(), DataLiberationActivity.class));
                break;
            }
            case DISMISS: {
                adapter.setDisplayFirstRunHeader(false);
                model.reRunQuery();
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventTabClick(TabClickEvent event) {
        if (event.position == ShowsActivity.InitBundle.INDEX_TAB_SHOWS) {
            recyclerView.smoothScrollToPosition(0);
        }
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
            model.reRunQuery();
        }
    };

    private void startActivityAddShows() {
        startActivity(new Intent(getActivity(), SearchActivity.class).putExtra(
                SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH));
    }

    private ShowsAdapter.OnItemClickListener onItemClickListener
            = new ShowsAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(@NotNull View anchor, int showTvdbId) {
            // display overview for this show
            Intent intent = OverviewActivity.intentShow(getContext(), showTvdbId);
            ActivityCompat.startActivity(getContext(), intent,
                    ActivityOptionsCompat
                            .makeScaleUpAnimation(anchor, 0, 0, anchor.getWidth(),
                                    anchor.getHeight())
                            .toBundle()
            );
        }

        @Override
        public void onItemMenuClick(@NotNull View view, @NotNull ShowsAdapter.ShowItem show) {
            PopupMenu popupMenu = new PopupMenu(view.getContext(), view);
            popupMenu.inflate(R.menu.shows_popup_menu);

            // show/hide some menu items depending on show properties
            Menu menu = popupMenu.getMenu();
            menu.findItem(R.id.menu_action_shows_favorites_add).setVisible(!show.isFavorite());
            menu.findItem(R.id.menu_action_shows_favorites_remove).setVisible(show.isFavorite());
            menu.findItem(R.id.menu_action_shows_hide).setVisible(!show.isHidden());
            menu.findItem(R.id.menu_action_shows_unhide).setVisible(show.isHidden());

            popupMenu.setOnMenuItemClickListener(
                    new ShowMenuItemClickListener(getContext(), getFragmentManager(),
                            show.getShowTvdbId(), show.getEpisodeTvdbId()));
            popupMenu.show();
        }

        @Override
        public void onItemSetWatchedClick(@NotNull ShowsAdapter.ShowItem show) {
            DBUtils.markNextEpisode(getContext(), show.getShowTvdbId(), show.getEpisodeTvdbId());
        }
    };

    private final OnSharedPreferenceChangeListener onPreferenceChangeListener
            = (sharedPreferences, key) -> {
        if (key.equals(AdvancedSettings.KEY_UPCOMING_LIMIT)) {
            updateShowsQuery();
            // refresh all list widgets
            ListWidgetProvider.notifyDataChanged(getContext());
        }
    };
}
