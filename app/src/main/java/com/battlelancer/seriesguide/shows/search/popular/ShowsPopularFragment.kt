// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.shows.search.popular

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentShowsPopularBinding
import com.battlelancer.seriesguide.shows.search.discover.BaseAddShowsFragment
import com.battlelancer.seriesguide.streaming.DiscoverFilterFragment
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Displays a list of popular shows to add that are paged in from the network.
 */
class ShowsPopularFragment : BaseAddShowsFragment() {

    private var binding: FragmentShowsPopularBinding? = null

    private lateinit var snackbar: Snackbar

    private val model: ShowsPopularViewModel by viewModels()
    private lateinit var adapter: ShowsPopularAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentShowsPopularBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return

        ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewShowsPopular)
        ThemeUtils.applyBottomMarginForNavigationBar(binding.textViewPoweredByShowsPopular)

        binding.swipeRefreshLayoutShowsPopular.apply {
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, this)
            setOnRefreshListener { adapter.refresh() }
        }

        snackbar =
            Snackbar.make(binding.swipeRefreshLayoutShowsPopular, "", Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.action_try_again) { adapter.refresh() }

        binding.recyclerViewShowsPopular.apply {
            setHasFixedSize(true)
            layoutManager =
                AutoGridLayoutManager(
                    context,
                    R.dimen.showgrid_columnWidth,
                    1,
                    1
                )
        }

        adapter = ShowsPopularAdapter(itemClickListener)
        binding.recyclerViewShowsPopular.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            model.items.collectLatest {
                adapter.submitData(it)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .collectLatest { loadStates ->
                Timber.d("loadStates=$loadStates")
                val refresh = loadStates.refresh
                binding.swipeRefreshLayoutShowsPopular.isRefreshing = refresh is LoadState.Loading
                if (refresh is LoadState.Error) {
                    snackbar.setText(refresh.error.message!!)
                    if (!snackbar.isShownOrQueued) snackbar.show()
                } else {
                    if (snackbar.isShownOrQueued) snackbar.dismiss()
                }
            }
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.shows_popular_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_action_shows_popular_filter -> {
                    DiscoverFilterFragment.showForShows(parentFragmentManager)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun setAllPendingNotAdded() {
        adapter.setAllPendingNotAdded()
    }

    override fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        adapter.setStateForTmdbId(showTmdbId, newState)
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewShowsPopular
    }

}