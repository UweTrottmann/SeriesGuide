// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2024 Uwe Trottmann
// Copyright 2013 Andrew Neal

package com.battlelancer.seriesguide.shows

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.MenuProvider
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
import com.battlelancer.seriesguide.shows.ShowsDistillationSettings.ShowFilter
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.shows.search.discover.ShowsDiscoverPagingActivity
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.BaseMessageActivity
import com.battlelancer.seriesguide.ui.OverviewActivity.Companion.intentShow
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.widgets.SgFastScroller
import com.battlelancer.seriesguide.util.ViewTools
import com.google.android.material.snackbar.Snackbar
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
        emptyView.setOnClickListener { startActivityAddShows() }
        emptyViewFilter = v.findViewById(R.id.emptyViewShowsFilter)
        ViewTools.setVectorDrawableTop(emptyViewFilter, R.drawable.ic_filter_white_24dp)
        emptyViewFilter.setOnClickListener {
            ShowsDistillationSettings.saveFilter(requireContext(), ShowFilter.default())
            // Note: not removing watch provider filters as it is ensured they always have matches
        }
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView.setHasFixedSize(true)
        val layoutManager =
            AutoGridLayoutManager(
                context,
                R.dimen.showgrid_columnWidth, 1, 1
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
        adapter = ShowsAdapter(requireContext(), onItemClickListener, firstRunClickListener)
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

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.shows_menu, menu)

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
                    startActivityAddShows()
                    true
                }

                R.id.menu_action_shows_filter -> {
                    show(parentFragmentManager)
                    true
                }

                R.id.menu_action_shows_update -> {
                    SgSyncAdapter.requestSyncDeltaImmediate(requireContext(), true)
                    true
                }
                R.id.menu_action_shows_redownload -> {
                    SgSyncAdapter.requestSyncFullImmediate(requireContext(), true)
                    true
                }

                else -> false
            }
        }
    }

    private fun startActivityAddShows() {
        startActivity(
            ShowsDiscoverPagingActivity.intentSearch(requireContext())
        )
    }

    private val onItemClickListener: ShowsAdapter.OnItemClickListener =
        object : ShowsAdapter.OnItemClickListener {
            override fun onItemClick(anchor: View, showRowId: Long) {
                // display overview for this show
                val intent = intentShow(requireContext(), showRowId)
                ActivityCompat.startActivity(
                    requireContext(), intent,
                    ActivityOptionsCompat.makeScaleUpAnimation(
                        anchor, 0, 0, anchor.width,
                        anchor.height
                    ).toBundle()
                )
            }

            override fun onItemMenuClick(anchor: View, show: ShowItem) {
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

            override fun onItemSetWatchedClick(show: ShowItem) {
                EpisodeTools.episodeWatchedIfNotZero(context, show.nextEpisodeId)
            }
        }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                adapter.refreshFirstRunHeader()
            } else {
                (activity as BaseMessageActivity?)?.snackbarParentView
                    ?.let {
                        Snackbar
                            .make(it, R.string.notifications_allow_reason, Snackbar.LENGTH_LONG)
                            .show()
                    }
            }
        }

    private val requestPreciseNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            // Regardless if granted or not, refresh to reflect current state.
            adapter.refreshFirstRunHeader()
        }

    private val firstRunClickListener = object : FirstRunView.FirstRunClickListener {
        override fun onAddShowClicked() {
            startActivityAddShows()
        }

        override fun onSignInClicked() {
            startActivity(Intent(requireActivity(), MoreOptionsActivity::class.java))
            // Launching a top activity, adjust animation to match.
            @Suppress("DEPRECATION") // just deprecated for predictive back
            requireActivity().overridePendingTransition(
                R.anim.activity_fade_enter_sg, R.anim.activity_fade_exit_sg
            )
        }

        override fun onRestoreBackupClicked() {
            startActivity(Intent(requireActivity(), DataLiberationActivity::class.java))
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