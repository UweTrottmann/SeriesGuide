// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.calendar

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.edit
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.ShowsActivityViewModel
import com.battlelancer.seriesguide.shows.database.SgEpisode2WithShow
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodeTools
import com.battlelancer.seriesguide.shows.episodes.EpisodesActivity
import com.battlelancer.seriesguide.traktapi.CheckInDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.menus.ManualSyncMenu
import com.battlelancer.seriesguide.ui.widgets.SgFastScroller
import com.battlelancer.seriesguide.util.Utils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Base class for a fragment displaying a list of episodes grouped by release date.
 */
abstract class CalendarFragment2 : Fragment() {

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var textViewEmpty: TextView
    protected val viewModel: CalendarFragment2ViewModel by viewModels()

    private lateinit var adapter: CalendarAdapter2

    abstract val tabPosition: Int

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .registerOnSharedPreferenceChangeListener(prefChangeListener)

        adapter = CalendarAdapter2(requireContext(), calendarItemClickListener)

        val layoutManager =
            AutoGridLayoutManager(
                context,
                R.dimen.show_grid_column_width, 1, 1,
                adapter
            )

        recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager = layoutManager
            it.adapter = adapter
        }
        SgFastScroller(requireContext(), recyclerView)

        ViewModelProvider(requireActivity()).get(ShowsActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) { position: Int? ->
                if (position != null) {
                    if (position == tabPosition) {
                        // The calendar list can get rather long,
                        // so do not use smooth scrolling as it can take quite some time.
                        recyclerView.scrollToPosition(0)
                    }
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.items.collectLatest {
                    adapter.submitData(it)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.onPagesUpdatedFlow.conflate().collectLatest {
                    updateEmptyView(adapter.itemCount == 0)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            // Re-build the query each time when STARTED (e.g. in multi-window this might be
            // visible, but not RESUMED) and again every minute as query conditions change based
            // on time. On the downside this runs even if the tab is not visible (tied to RESUMED).
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    updateCalendarQuery()
                    delay(DateUtils.MINUTE_IN_MILLIS + Random.nextLong(DateUtils.SECOND_IN_MILLIS))
                }
            }
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        recyclerView.isGone = isEmpty
        textViewEmpty.isGone = !isEmpty
    }

    abstract suspend fun updateCalendarQuery()

    override fun onDestroyView() {
        super.onDestroyView()
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .unregisterOnSharedPreferenceChangeListener(prefChangeListener)
    }

    private val optionsMenuProvider by lazy {
        object : ManualSyncMenu(requireContext(), R.menu.calendar_menu) {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                super.onCreateMenu(menu, menuInflater)

                // set menu items to current values
                val context = requireContext()
                menu.findItem(R.id.menu_action_calendar_onlyfavorites).isChecked =
                    CalendarSettings.isOnlyFavorites(context)
                menu.findItem(R.id.menu_action_calendar_onlypremieres).isChecked =
                    CalendarSettings.isOnlyPremieres(context)
                menu.findItem(R.id.menu_action_calendar_onlycollected).isChecked =
                    CalendarSettings.isOnlyCollected(context)
                menu.findItem(R.id.menu_action_calendar_nospecials).isChecked =
                    DisplaySettings.isHidingSpecials(context)
                menu.findItem(R.id.menu_action_calendar_nowatched).isChecked =
                    CalendarSettings.isHidingWatchedEpisodes(context)
                menu.findItem(R.id.menu_action_calendar_infinite).isChecked =
                    CalendarSettings.isInfiniteScrolling(context)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_action_calendar_search -> {
                        startActivity(Intent(requireContext(), SearchActivity::class.java))
                        true
                    }

                    R.id.menu_action_calendar_onlyfavorites -> {
                        toggleFilterSetting(menuItem, CalendarSettings.KEY_ONLY_FAVORITE_SHOWS)
                        true
                    }

                    R.id.menu_action_calendar_onlypremieres -> {
                        toggleFilterSetting(menuItem, CalendarSettings.KEY_ONLY_PREMIERES)
                        true
                    }

                    R.id.menu_action_calendar_onlycollected -> {
                        toggleFilterSetting(menuItem, CalendarSettings.KEY_ONLY_COLLECTED)
                        true
                    }

                    R.id.menu_action_calendar_nospecials -> {
                        toggleFilterSetting(menuItem, DisplaySettings.KEY_HIDE_SPECIALS)
                        true
                    }

                    R.id.menu_action_calendar_nowatched -> {
                        toggleFilterSetting(menuItem, CalendarSettings.KEY_HIDE_WATCHED_EPISODES)
                        true
                    }

                    R.id.menu_action_calendar_infinite -> {
                        toggleFilterSetting(menuItem, CalendarSettings.KEY_INFINITE_SCROLLING_2)
                        true
                    }

                    else -> super.onMenuItemSelected(menuItem)
                }
            }

        }
    }

    private fun toggleFilterSetting(item: MenuItem, key: String) {
        PreferenceManager.getDefaultSharedPreferences(requireContext()).edit {
            putBoolean(key, !item.isChecked)
        }
        // refresh filter icon state
        requireActivity().invalidateOptionsMenu()
    }

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            CalendarSettings.KEY_ONLY_FAVORITE_SHOWS,
            CalendarSettings.KEY_ONLY_PREMIERES,
            CalendarSettings.KEY_ONLY_COLLECTED,
            DisplaySettings.KEY_HIDE_SPECIALS,
            CalendarSettings.KEY_HIDE_WATCHED_EPISODES,
            CalendarSettings.KEY_INFINITE_SCROLLING_2 -> {
                viewLifecycleOwner.lifecycleScope.launch {
                    updateCalendarQuery()
                }
            }
        }
    }

    private val calendarItemClickListener = object :
        CalendarAdapter2.ItemClickListener {
        override fun onItemClick(episodeId: Long) {
            val intent = EpisodesActivity.intentEpisode(episodeId, requireContext())
            Utils.startActivityWithAnimation(activity, intent, view)
        }

        override fun onMoreOptionsClick(anchor: View, episode: SgEpisode2WithShow) {
            val context = anchor.context

            val popupMenu = PopupMenu(context, anchor)
            val menu = popupMenu.menu

            // only display the action appropriate for the items current state
            if (EpisodeTools.isWatched(episode.watched)) {
                menu.add(0, CONTEXT_FLAG_UNWATCHED_ID, 0, R.string.action_unwatched)
            } else {
                menu.add(0, CONTEXT_FLAG_WATCHED_ID, 0, R.string.action_watched)
            }
            if (episode.episode_collected) {
                menu.add(0, CONTEXT_COLLECTION_REMOVE_ID, 1, R.string.action_collection_remove)
            } else {
                menu.add(0, CONTEXT_COLLECTION_ADD_ID, 1, R.string.action_collection_add)
            }
            // display check-in if only trakt is connected
            if (TraktCredentials.get(context).hasCredentials()
                && !HexagonSettings.isEnabled(context)) {
                menu.add(0, CONTEXT_CHECKIN_ID, 2, R.string.checkin)
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    CONTEXT_CHECKIN_ID -> {
                        CheckInDialogFragment.show(
                            requireContext(),
                            parentFragmentManager,
                            episode.id
                        )
                        return@setOnMenuItemClickListener true
                    }

                    CONTEXT_FLAG_WATCHED_ID -> {
                        updateEpisodeWatchedState(episode.id, true)
                        return@setOnMenuItemClickListener true
                    }

                    CONTEXT_FLAG_UNWATCHED_ID -> {
                        updateEpisodeWatchedState(episode.id, false)
                        return@setOnMenuItemClickListener true
                    }

                    CONTEXT_COLLECTION_ADD_ID -> {
                        updateEpisodeCollectionState(episode.id, true)
                        return@setOnMenuItemClickListener true
                    }

                    CONTEXT_COLLECTION_REMOVE_ID -> {
                        updateEpisodeCollectionState(episode.id, false)
                        return@setOnMenuItemClickListener true
                    }
                }
                false
            }

            popupMenu.show()
        }

        override fun onWatchedBoxClick(episode: SgEpisode2WithShow, isWatched: Boolean) {
            updateEpisodeWatchedState(episode.id, !isWatched)
        }
    }

    private fun updateEpisodeCollectionState(episodeId: Long, addToCollection: Boolean) {
        EpisodeTools.episodeCollected(requireContext(), episodeId, addToCollection)
    }

    private fun updateEpisodeWatchedState(episodeId: Long, isWatched: Boolean) {
        EpisodeTools.episodeWatched(
            requireContext(),
            episodeId,
            if (isWatched) EpisodeFlags.WATCHED else EpisodeFlags.UNWATCHED
        )
    }

    companion object {
        private const val CONTEXT_FLAG_WATCHED_ID = 0
        private const val CONTEXT_FLAG_UNWATCHED_ID = 1
        private const val CONTEXT_CHECKIN_ID = 2
        private const val CONTEXT_COLLECTION_ADD_ID = 3
        private const val CONTEXT_COLLECTION_REMOVE_ID = 4
    }

}
