// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Uwe Trottmann
// Copyright 2013 Andrew Neal

package com.battlelancer.seriesguide.shows

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withStarted
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider.Companion.notifyDataChanged
import com.battlelancer.seriesguide.dataliberation.DataLiberationActivity
import com.battlelancer.seriesguide.preferences.MoreOptionsActivity
import com.battlelancer.seriesguide.settings.AdvancedSettings
import com.battlelancer.seriesguide.settings.NotificationSettings
import com.battlelancer.seriesguide.shows.ShowsAdapter.ShowItem
import com.battlelancer.seriesguide.shows.ShowsDistillationFragment.Companion.show
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowFilters
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.OverviewActivity.Companion.intentShow
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.menus.ManualSyncMenu
import com.battlelancer.seriesguide.ui.widgets.SgFastScroller
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.startActivityWithAnimation
import com.google.android.material.snackbar.Snackbar
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

/**
 * Displays the list of shows in a users local library with sorting and filtering abilities. The
 * main view of the app.
 */
class ShowsFragment : Fragment() {

    private lateinit var adapter: ShowsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: Button
    private lateinit var emptyViewFilter: Button

    private var scheduledUpdateJob: Job? = null
    private val activityModel by viewModels<ShowsActivityViewModel>({ requireActivity() })
    private val model by viewModels<ShowsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_shows, container, false)
        recyclerView = v.findViewById(R.id.recyclerViewShows)
        SgFastScroller(requireContext(), recyclerView)
        emptyView = v.findViewById(R.id.emptyViewShows)
        ViewTools.setVectorDrawableTop(emptyView, R.drawable.ic_add_white_24dp)
        emptyView.setOnClickListener { navigateToAddShows() }
        emptyViewFilter = v.findViewById(R.id.emptyViewShowsFilter)
        ViewTools.setVectorDrawableTop(emptyViewFilter, R.drawable.ic_filter_white_24dp)
        emptyViewFilter.setOnClickListener {
            activityModel.showsDistillationSettings.saveFilters(ShowFilters.default())
            // Note: not removing watch provider filters as it is ensured they always have matches
        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.setHasFixedSize(true)
        val layoutManager =
            AutoGridLayoutManager(
                context,
                R.dimen.show_grid_column_width, 1, 1
            )
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter.getItemViewType(position) == ShowsAdapter.VIEW_TYPE_FIRST_RUN) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
        recyclerView.layoutManager = layoutManager

        activityModel.scrollTabToTopLiveData.observe(viewLifecycleOwner) { tabPosition: Int? ->
            if (tabPosition != null
                && tabPosition == ShowsActivityImpl.Tab.SHOWS.index) {
                recyclerView.smoothScrollToPosition(0)
            }
        }

        // prepare view adapter
        adapter = ShowsAdapter(requireContext(), itemClickListener, firstRunClickListener)
        if (!FirstRunView.hasSeenFirstRunFragment(requireContext())) {
            adapter.displayFirstRunHeader = true
        }
        recyclerView.adapter = adapter

        model.showItemsLiveData.observe(viewLifecycleOwner) { showItems: MutableList<ShowItem>? ->
            adapter.submitList(showItems)
            // note: header is added later, but if it is shown should not treat as empty
            val isEmpty = (!adapter.displayFirstRunHeader
                    && (showItems == null || showItems.isEmpty()))
            updateEmptyView(isEmpty)

            // Once latest data loaded, schedule next query refresh.
            // Some changes to the displayed data are not based on actual (detectable) changes to
            // the underlying data, but because time has passed (e.g. relative time displays,
            // release time has passed).
            scheduledUpdateJob?.cancel()
            scheduledUpdateJob = viewLifecycleOwner.lifecycleScope.launch {
                Timber.d("Scheduled query update.")
                delay(DateUtils.MINUTE_IN_MILLIS + Random.nextLong(DateUtils.SECOND_IN_MILLIS))
                // Use STARTED as in multi-window this might be visible, but not RESUMED.
                // On the downside this runs even if the tab is not visible (tied to RESUMED).
                viewLifecycleOwner.lifecycle.withStarted {
                    updateShowsQuery()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            model.uiState.collectLatest {
                // refresh filter menu icon state
                requireActivity().invalidateOptionsMenu()
            }
        }

        // Note: do not collect the current values, they are set when initializing UiState
        // watch for sort order changes
        viewLifecycleOwner.lifecycleScope.launch {
            activityModel.showsDistillationSettings.sortOrder.drop(1).collect {
                model.uiState.value = model.uiState.value.copy(showSortOrder = it)
                model.updateQuery()
            }
        }
        // watch for filter changes
        viewLifecycleOwner.lifecycleScope.launch {
            activityModel.showsDistillationSettings.showFilters.drop(1).collect {
                model.uiState.value = model.uiState.value.copy(showFilters = it)
                model.updateQuery()
            }
        }

        // Run initial query and refresh query immediately each time when STARTED.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateShowsQuery()
            }
        }

        // listen for some settings changes
        PreferenceManager
            .getDefaultSharedPreferences(requireActivity())
            .registerOnSharedPreferenceChangeListener(onPreferenceChangeListener)

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun updateShowsQuery() {
        model.updateQuery()
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isEmpty) {
            if (model.uiState.value.isFiltersActive) {
                emptyViewFilter.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            } else {
                emptyView.visibility = View.VISIBLE
                emptyViewFilter.visibility = View.GONE
            }
        } else {
            emptyView.visibility = View.GONE
            emptyViewFilter.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        // Query update already repeated when STARTED, prevent scheduled update from also running.
        scheduledUpdateJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.unregisterOnSharedPreferenceChangeListener(onPreferenceChangeListener)
    }

    private val optionsMenuProvider by lazy {
        object : ManualSyncMenu(requireContext(), R.menu.shows_menu) {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                super.onCreateMenu(menu, menuInflater)

                // set filter icon state
                menu.findItem(R.id.menu_action_shows_filter)
                    .setIcon(
                        if (model.uiState.value.isFiltersActive) {
                            R.drawable.ic_filter_selected_white_24dp
                        } else {
                            R.drawable.ic_filter_white_24dp
                        }
                    )
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_action_shows_search -> {
                        startActivity(Intent(requireContext(), SearchActivity::class.java))
                        true
                    }

                    R.id.menu_action_shows_add -> {
                        navigateToAddShows()
                        true
                    }

                    R.id.menu_action_shows_filter -> {
                        show(parentFragmentManager)
                        true
                    }

                    else -> super.onMenuItemSelected(menuItem)
                }
            }

        }
    }

    private fun navigateToAddShows() {
        activityModel.selectDiscoverTab()
    }

    private val itemClickListener: ShowsViewHolder.ItemClickListener =
        object : ShowsViewHolder.ItemClickListener {
            override fun onItemClick(anchor: View, showRowId: Long) {
                // display overview for this show
                val intent = intentShow(requireContext(), showRowId)
                requireContext().startActivityWithAnimation(intent, anchor)
            }

            override fun onMoreOptionsClick(anchor: View, show: ShowItem) {
                val popupMenu = PopupMenu(anchor.context, anchor)
                popupMenu.inflate(R.menu.shows_popup_menu)

                // show/hide some menu items depending on show properties
                val menu = popupMenu.menu
                menu.findItem(R.id.menu_action_shows_favorites_add).isVisible = !show.isFavorite
                menu.findItem(R.id.menu_action_shows_favorites_remove).isVisible = show.isFavorite
                menu.findItem(R.id.menu_action_shows_hide).isVisible = !show.isHidden
                menu.findItem(R.id.menu_action_shows_unhide).isVisible = show.isHidden
                popupMenu.setOnMenuItemClickListener(
                    ShowMenuItemClickListener(
                        requireContext(), parentFragmentManager,
                        show.rowId, show.nextEpisodeId
                    )
                )
                popupMenu.show()
            }

            override fun onSetWatchedClick(show: ShowItem) {
                EpisodeTools.episodeWatchedIfNotZero(requireContext(), show.nextEpisodeId)
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                adapter.refreshFirstRunHeader()
            } else {
                (activity as BaseMessageActivity?)
                    ?.makeSnackbar(R.string.notifications_allow_reason, Snackbar.LENGTH_LONG)
                    ?.show()
            }
        }

    private val requestPreciseNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            // Regardless if granted or not, refresh to reflect current state.
            adapter.refreshFirstRunHeader()
        }

    private val firstRunClickListener = object : FirstRunView.FirstRunClickListener {
        override fun onAddShowClicked() {
            navigateToAddShows()
        }

        override fun onSignInClicked() {
            startActivity(Intent(requireActivity(), MoreOptionsActivity::class.java))
            // For UPSIDE_DOWN_CAKE+ using overrideActivityTransition() in BaseTopActivity
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Launching a top activity, adjust animation to match.
                @Suppress("DEPRECATION")
                requireActivity().overridePendingTransition(
                    R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg
                )
            }
        }

        override fun onRestoreBackupClicked() {
            startActivity(DataLiberationActivity.intent(requireActivity()))
        }

        override fun onRestoreAutoBackupClicked() {
            startActivity(DataLiberationActivity.intentToShowAutoBackup(requireActivity()))
        }

        override fun onAllowNotificationsClicked() {
            if (AndroidUtils.isAtLeastTiramisu) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        override fun onAllowPreciseNotificationsClicked() {
            if (AndroidUtils.isAtLeastS) {
                requestPreciseNotificationPermissionLauncher.launch(
                    NotificationSettings.buildRequestExactAlarmSettingsIntent(requireContext())
                )
            }
        }

        override fun onDismissClicked() {
            adapter.displayFirstRunHeader = false
            updateShowsQuery()
        }

    }

    private val onPreferenceChangeListener =
        OnSharedPreferenceChangeListener { _: SharedPreferences?, key: String? ->
            if (key == AdvancedSettings.KEY_UPCOMING_LIMIT) {
                updateShowsQuery()
                // refresh all list widgets
                notifyDataChanged(requireContext())
            }
        }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewShows
    }
}