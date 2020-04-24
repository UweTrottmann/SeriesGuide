package com.battlelancer.seriesguide.ui.shows;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity;
import com.battlelancer.seriesguide.settings.AdvancedSettings;
import com.battlelancer.seriesguide.ui.OverviewActivity;
import com.battlelancer.seriesguide.ui.SearchActivity;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager;
import com.battlelancer.seriesguide.ui.preferences.MoreOptionsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
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

    private SortShowsView.ShowSortOrder showSortOrder;
    private FilterShowsView.ShowFilter showFilter;

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
        ViewTools.setVectorDrawableTop(emptyView, R.drawable.ic_add_white_24dp);
        emptyView.setOnClickListener(view -> startActivityAddShows());
        emptyViewFilter = v.findViewById(R.id.emptyViewShowsFilter);
        ViewTools.setVectorDrawableTop(emptyViewFilter, R.drawable.ic_filter_white_24dp);
        emptyViewFilter.setOnClickListener(view -> {
            ShowsDistillationSettings.filterLiveData
                    .setValue(FilterShowsView.ShowFilter.allDisabled());

            ShowsDistillationSettings.saveFilter(requireContext(), null, null, null, null, null);
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

        new ViewModelProvider(requireActivity()).get(ShowsActivityViewModel.class)
                .getScrollTabToTopLiveData()
                .observe(getViewLifecycleOwner(), tabPosition -> {
                    if (tabPosition != null
                            && tabPosition == ShowsActivity.InitBundle.INDEX_TAB_SHOWS) {
                        recyclerView.smoothScrollToPosition(0);
                    }
                });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // get settings
        showFilter = FilterShowsView.ShowFilter.fromSettings(requireContext());
        showSortOrder = SortShowsView.ShowSortOrder.fromSettings(requireContext());

        // prepare view adapter
        adapter = new ShowsAdapter(requireContext(), onItemClickListener);
        if (!FirstRunView.hasSeenFirstRunFragment(requireContext())) {
            adapter.setDisplayFirstRunHeader(true);
        }
        recyclerView.setAdapter(adapter);

        model = new ViewModelProvider(this).get(ShowsViewModel.class);
        model.getShowItemsLiveData().observe(getViewLifecycleOwner(), showItems -> {
            adapter.submitList(showItems);
            // note: header is added later, but if it is shown should not treat as empty
            boolean isEmpty = !adapter.getDisplayFirstRunHeader()
                    && (showItems == null || showItems.isEmpty());
            updateEmptyView(isEmpty);
        });
        updateShowsQuery();

        // watch for sort order changes
        ShowsDistillationSettings.sortOrderLiveData
                .observe(getViewLifecycleOwner(), showSortOrder -> {
                    this.showSortOrder = showSortOrder;
                    // re-run query
                    updateShowsQuery();
                });

        // watch for filter changes
        ShowsDistillationSettings.filterLiveData.observe(getViewLifecycleOwner(), showFilter -> {
            this.showFilter = showFilter;
            // re-run query
            updateShowsQuery();
            // refresh filter menu icon state
            requireActivity().invalidateOptionsMenu();
        });

        // hide floating action button when scrolling shows
        FloatingActionButton buttonAddShow = requireActivity().findViewById(R.id.buttonShowsAdd);
        recyclerView.addOnScrollListener(new FabRecyclerViewScrollDetector(buttonAddShow));

        // listen for some settings changes
        PreferenceManager
                .getDefaultSharedPreferences(requireActivity())
                .registerOnSharedPreferenceChangeListener(onPreferenceChangeListener);

        setHasOptionsMenu(true);
    }

    private void updateShowsQuery() {
        model.updateQuery(showFilter, ShowsDistillationSettings
                .getSortQuery(showSortOrder.getSortOrderId(), showSortOrder.isSortFavoritesFirst(),
                        showSortOrder.isSortIgnoreArticles()));
    }

    private void updateEmptyView(boolean isEmpty) {
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        if (isEmpty) {
            if (showFilter.isAnyFilterEnabled()) {
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
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        prefs.unregisterOnSharedPreferenceChangeListener(onPreferenceChangeListener);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.shows_menu, menu);

        // set filter icon state
        menu.findItem(R.id.menu_action_shows_filter)
                .setIcon(showFilter.isAnyFilterEnabled() ?
                        R.drawable.ic_filter_selected_white_24dp : R.drawable.ic_filter_white_24dp);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_shows_add) {
            startActivityAddShows();
            return true;
        } else if (itemId == R.id.menu_action_shows_filter) {
            ShowsDistillationFragment.show(getParentFragmentManager());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
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
                startActivity(new Intent(getActivity(), MoreOptionsActivity.class));
                // Launching a top activity, adjust animation to match.
                requireActivity().overridePendingTransition(
                        R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg);
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
            Intent intent = OverviewActivity.intentShow(requireContext(), showTvdbId);
            ActivityCompat.startActivity(requireContext(), intent,
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
                    new ShowMenuItemClickListener(getContext(), getParentFragmentManager(),
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
            ListWidgetProvider.notifyDataChanged(requireContext());
        }
    };
}
