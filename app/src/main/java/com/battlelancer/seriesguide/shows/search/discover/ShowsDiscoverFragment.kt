// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.MenuProvider
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentShowsDiscoverBinding
import com.battlelancer.seriesguide.shows.ShowsActivityImpl
import com.battlelancer.seriesguide.shows.ShowsActivityViewModel
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.shows.search.discover.AddFragment.OnAddingShowEvent
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsActivity
import com.battlelancer.seriesguide.shows.search.similar.SimilarShowsFragment
import com.battlelancer.seriesguide.streaming.WatchProviderFilterDialogFragment
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.ui.dialogs.LanguagePickerDialogFragment
import com.battlelancer.seriesguide.ui.dialogs.YearPickerDialogFragment
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.util.findDialog
import com.battlelancer.seriesguide.util.safeShow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Displays links to popular shows, shows with new episodes and if connected to Trakt
 * also links to Trakt lists ([TraktAddFragment]).
 *
 * Displays a limited list of shows with new episodes that can be filtered by year, language
 * and watch provider.
 */
class ShowsDiscoverFragment : BaseAddShowsFragment() {

    private var binding: FragmentShowsDiscoverBinding? = null
    private lateinit var adapter: ShowsDiscoverAdapter
    private val activityModel by activityViewModels<ShowsActivityViewModel>()
    private val model: ShowsDiscoverViewModel by viewModels()

    private var yearPicker: YearPickerDialogFragment? = null
    private var languagePicker: LanguagePickerDialogFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentShowsDiscoverBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding!!
        val swipeRefreshLayout = binding.swipeRefreshLayoutShowsDiscover
        swipeRefreshLayout.setOnRefreshListener { refreshData() }
        ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, swipeRefreshLayout)

        adapter = ShowsDiscoverAdapter(
            requireContext(), discoverItemClickListener,
            TraktCredentials.get(requireContext()).hasCredentials(), true
        )
        val recyclerView = binding.recyclerViewShowsDiscover
        recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager = adapter.layoutManager
            it.adapter = adapter
        }

        // observe results and loading state
        viewLifecycleOwner.lifecycleScope.launch {
            model.data.collectLatest {
                adapter.updateSearchResults(it.searchResults, it.emptyText, !it.successful)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            model.isRefreshing.collectLatest {
                binding.swipeRefreshLayoutShowsDiscover.isRefreshing = it
            }
        }

        // Re-attach listeners to any showing dialogs
        yearPicker = findDialog<YearPickerDialogFragment>(parentFragmentManager, TAG_YEAR_PICKER)
            ?.also { it.onPickedListener = firstReleaseYearPickedListener }
        languagePicker =
            findDialog<LanguagePickerDialogFragment>(parentFragmentManager, TAG_LANGUAGE_PICKER)
                ?.also { it.onPickedListener = originalLanguagePickedListener }

        activityModel.scrollTabToTopLiveData.observe(viewLifecycleOwner) { tabPosition: Int? ->
            if (tabPosition != null && tabPosition == ShowsActivityImpl.Tab.DISCOVER.index) {
                recyclerView.smoothScrollToPosition(0)
            }
        }

        SimilarShowsFragment.displaySimilarShowsEventLiveData.observe(viewLifecycleOwner) {
            startActivity(SimilarShowsActivity.intent(requireContext(), it.tmdbId, it.title))
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private val discoverItemClickListener = object : ShowsDiscoverAdapter.OnItemClickListener {
        override fun onLinkClick(anchor: View, link: DiscoverShowsLink) {
            val intent =
                when (link) {
                    DiscoverShowsLink.POPULAR,
                    DiscoverShowsLink.NEW_EPISODES -> {
                        ShowsDiscoverPagingActivity.intentLink(requireContext(), link)
                    }

                    DiscoverShowsLink.WATCHLIST,
                    DiscoverShowsLink.WATCHED,
                    DiscoverShowsLink.COLLECTION -> {
                        ShowsTraktActivity.intent(requireContext(), link)
                    }

                }
            Utils.startActivityWithAnimation(activity, intent, anchor)
        }

        override fun onHeaderButtonClick(anchor: View) {
            val popupMenu = PopupMenu(context, anchor)
            popupMenu.inflate(R.menu.new_episodes_filter_popup_menu)
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_action_new_episodes_filter_year -> {
                        YearPickerDialogFragment
                            .create(ShowsDiscoverSettings.getFirstReleaseYearRaw(requireContext()))
                            .also { yearPicker = it }
                            .apply { onPickedListener = firstReleaseYearPickedListener }
                            .safeShow(parentFragmentManager, TAG_YEAR_PICKER)
                        return@setOnMenuItemClickListener true
                    }

                    R.id.menu_action_new_episodes_filter_language -> {
                        LanguagePickerDialogFragment
                            .createForShows(ShowsDiscoverSettings.getOriginalLanguage(requireContext()))
                            .also { languagePicker = it }
                            .apply { onPickedListener = originalLanguagePickedListener }
                            .safeShow(parentFragmentManager, TAG_LANGUAGE_PICKER)
                        return@setOnMenuItemClickListener true
                    }

                    R.id.menu_action_new_episodes_filter_providers -> {
                        WatchProviderFilterDialogFragment.showForShows(parentFragmentManager)
                        return@setOnMenuItemClickListener true
                    }

                    else -> return@setOnMenuItemClickListener false
                }
            }
            popupMenu.show()
        }

        override fun onItemClick(item: SearchResult) {
            if (item.state != SearchResult.STATE_ADDING) {
                if (item.state == SearchResult.STATE_ADDED) {
                    // already in library, open it
                    startActivity(
                        OverviewActivity.intentShowByTmdbId(
                            requireContext(),
                            item.tmdbId
                        )
                    )
                } else {
                    // display more details in a dialog
                    AddShowDialogFragment.show(parentFragmentManager, item)
                }
            }
        }

        override fun onAddClick(item: SearchResult) {
            // post to let other fragments know show is getting added
            EventBus.getDefault().post(OnAddingShowEvent(item.tmdbId))
            TaskManager.getInstance().performAddTask(context, item)
        }

        override fun onMenuWatchlistClick(view: View, showTmdbId: Int) {
            PopupMenu(view.context, view).apply {
                inflate(R.menu.add_dialog_popup_menu)
                // only support adding shows to watchlist
                menu.findItem(R.id.menu_action_show_watchlist_remove).isVisible = false
                setOnMenuItemClickListener(
                    TraktAddFragment.AddItemMenuItemClickListener(requireContext(), showTmdbId)
                )
            }.show()
        }

        override fun onEmptyViewButtonClick() {
            refreshData()
        }
    }

    private fun refreshData() {
        model.refreshData()
    }

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.shows_discover_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_action_shows_discover_search -> {
                    startActivity(ShowsDiscoverPagingActivity.intentSearch(requireContext()))
                    true
                }

                R.id.menu_action_shows_discover_change_language -> {
                    displayLanguageSettings()
                    true
                }

                else -> false
            }
        }
    }

    private fun displayLanguageSettings() {
        L10nDialogFragment.show(
            parentFragmentManager,
            ShowsSettings.getShowsSearchLanguage(requireContext()),
            L10nDialogFragment.TAG_DISCOVER
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: L10nDialogFragment.LanguageChangedEvent) {
        if (L10nDialogFragment.TAG_DISCOVER != event.tag) {
            return
        }
        model.changeResultsLanguage(event.selectedLanguageCode)
    }

    private val firstReleaseYearPickedListener =
        object : YearPickerDialogFragment.OnPickedListener {
            override fun onPicked(year: Int?) {
                model.changeFirstReleaseYear(year)
            }
        }

    private val originalLanguagePickedListener =
        object : LanguagePickerDialogFragment.OnPickedListener {
            override fun onPicked(languageCode: String?) {
                model.changeOriginalLanguage(languageCode)
            }
        }

    override fun setAllPendingNotAdded() {
        adapter.setAllPendingNotAdded()
    }

    override fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        adapter.setStateForTmdbId(showTmdbId, newState)
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewShowsDiscover
        private const val TAG_YEAR_PICKER = "yearPicker"
        private const val TAG_LANGUAGE_PICKER = "languagePicker"
    }

}