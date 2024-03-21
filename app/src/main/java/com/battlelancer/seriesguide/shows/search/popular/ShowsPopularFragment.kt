// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2023 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.popular

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ActivityTraktShowsBinding
import com.battlelancer.seriesguide.databinding.FragmentShowsPopularBinding
import com.battlelancer.seriesguide.shows.search.discover.BaseAddShowsFragment
import com.battlelancer.seriesguide.shows.search.discover.TraktShowsActivity
import com.battlelancer.seriesguide.streaming.DiscoverFilterFragment
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.LanguageTools
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

    private lateinit var bindingActivity: ActivityTraktShowsBinding
    private var binding: FragmentShowsPopularBinding? = null

    private lateinit var snackbar: Snackbar

    private val model: ShowsPopularViewModel by viewModels()
    private lateinit var adapter: ShowsPopularAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingActivity = (requireActivity() as TraktShowsActivity).binding
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
                    binding.swipeRefreshLayoutShowsPopular.isRefreshing =
                        refresh is LoadState.Loading
                    if (refresh is LoadState.Error) {
                        snackbar.setText(refresh.error.message!!)
                        if (!snackbar.isShownOrQueued) snackbar.show()
                    } else {
                        if (snackbar.isShownOrQueued) snackbar.dismiss()
                    }
                }
        }

        bindingActivity.chipTraktShowsFirstReleaseYear.isVisible = false
        bindingActivity.chipTraktShowsOriginalLanguage.isVisible = false
//        bindingActivity.chipTraktShowsFirstReleaseYear.setOnClickListener {
//
//        }
//        bindingActivity.chipTraktShowsOriginalLanguage.setOnClickListener {
//
//        }
        bindingActivity.chipTraktShowsWatchProviders.setOnClickListener {
            DiscoverFilterFragment.showForShows(parentFragmentManager)
        }
        viewLifecycleOwner.lifecycleScope.launch {
            model.filters.collectLatest {
                bindingActivity.chipTraktShowsFirstReleaseYear.apply {
                    val hasYear = it.originalLanguage != null
                    isChipIconVisible = hasYear
                    text = if (hasYear) {
                        it.firstReleaseYear.toString()
                    } else {
                        getString(R.string.filter_year)
                    }
                }
                bindingActivity.chipTraktShowsOriginalLanguage.apply {
                    val hasLanguage = it.originalLanguage != null
                    isChipIconVisible = hasLanguage
                    text = if (hasLanguage) {
                        LanguageTools.buildLanguageDisplayName(it.originalLanguage!!)
                    } else {
                        getString(R.string.filter_language)
                    }
                }
                bindingActivity.chipTraktShowsWatchProviders.apply {
                    isChipIconVisible = it.watchProviderIds?.isNotEmpty() ?: false
                }
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