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
import androidx.recyclerview.widget.GridLayoutManager
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
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.OverviewActivity
import com.battlelancer.seriesguide.ui.dialogs.L10nDialogFragment
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays links to popular shows, shows with new episodes and if connected to Trakt
 * also links to Trakt lists ([TraktAddFragment]).
 *
 * Displays a limited list of shows with new episodes that can be filtered by watch provider.
 */
class ShowsDiscoverFragment : BaseAddShowsFragment() {

    private var binding: FragmentShowsDiscoverBinding? = null
    private lateinit var adapter: ShowsDiscoverAdapter
    private val activityModel by activityViewModels<ShowsActivityViewModel>()
    private val model: ShowsDiscoverViewModel by viewModels()

    private lateinit var languageCode: String

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
        swipeRefreshLayout.setSwipeableChildren(
            R.id.scrollViewShowsDiscover,
            R.id.recyclerViewShowsDiscover
        )
        swipeRefreshLayout.setOnRefreshListener { loadResults(true) }
        ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, swipeRefreshLayout)

        val emptyView = binding.emptyViewShowsDiscover
        emptyView.visibility = View.GONE
        emptyView.setButtonClickListener {
            // Retrying, force load results again.
            loadResults(true)
        }

        val layoutManager = AutoGridLayoutManager(
            context, R.dimen.showgrid_columnWidth,
            2, 2
        ).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (adapter.getItemViewType(position)) {
                        ShowsDiscoverAdapter.VIEW_TYPE_LINK -> 1
                        ShowsDiscoverAdapter.VIEW_TYPE_HEADER -> spanCount
                        ShowsDiscoverAdapter.VIEW_TYPE_SHOW -> 2
                        else -> 0
                    }
                }
            }
        }

        val recyclerView = binding.recyclerViewShowsDiscover
        recyclerView.apply {
            setHasFixedSize(true)
            this.layoutManager = layoutManager
        }

        adapter = ShowsDiscoverAdapter(
            requireContext(), discoverItemClickListener,
            TraktCredentials.get(requireContext()).hasCredentials(), true
        )
        recyclerView.adapter = adapter

        languageCode = ShowsSettings.getShowsSearchLanguage(requireContext())

        // observe and load results
        model.data.observe(viewLifecycleOwner) { handleResultsUpdate(it) }

        // initial load after getting watch providers, reload on watch provider changes
        model.watchProviderIds.observe(viewLifecycleOwner) {
            loadResults()
        }

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
    }

    private fun loadResults(forceLoad: Boolean = false) {
        val watchProviderIds = model.watchProviderIds.value
        val willLoad = model.data.load(languageCode, watchProviderIds, forceLoad)
        if (willLoad) binding?.swipeRefreshLayoutShowsDiscover?.isRefreshing = true
    }

    private fun handleResultsUpdate(result: ShowsDiscoverLiveData.Result?) {
        result?.let {
            val binding = binding!!
            binding.swipeRefreshLayoutShowsDiscover.isRefreshing = false

            val hasResults = result.searchResults.isNotEmpty()

            val emptyView = binding.emptyViewShowsDiscover
            emptyView.setButtonText(R.string.action_try_again)
            emptyView.setMessage(result.emptyText)
            emptyView.visibility = if (hasResults) View.GONE else View.VISIBLE

            binding.recyclerViewShowsDiscover.visibility =
                if (hasResults) View.VISIBLE else View.GONE
            adapter.updateSearchResults(result.searchResults)
        }
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

                R.id.menu_action_shows_discover_filter -> {
                    WatchProviderFilterDialogFragment.showForShows(parentFragmentManager)
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
            languageCode,
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
        changeLanguage(event.selectedLanguageCode)
        loadResults()
    }

    private fun changeLanguage(languageCode: String) {
        this.languageCode = languageCode

        // save selected search language
        ShowsSettings.saveShowsSearchLanguage(requireContext(), languageCode)
        Timber.d("Set search language to %s", languageCode)
    }

    override fun setAllPendingNotAdded() {
        adapter.setAllPendingNotAdded()
    }

    override fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        adapter.setStateForTmdbId(showTmdbId, newState)
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewShowsDiscover
    }

}