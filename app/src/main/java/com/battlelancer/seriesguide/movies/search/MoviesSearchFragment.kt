// SPDX-License-Identifier: Apache-2.0
// Copyright 2020-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesSearchBinding
import com.battlelancer.seriesguide.movies.MovieClickListenerImpl
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Displays movies from [MoviesSearchViewModel], supports empty view and swipe to refresh,
 * also refreshes on [MovieLocalizationDialogFragment.LocalizationChangedEvent].
 */
class MoviesSearchFragment : Fragment() {

    private var _binding: FragmentMoviesSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MoviesSearchAdapter

    private val activityModel: MoviesSearchViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewMoviesSearch)
        ThemeUtils.applyBottomMarginForNavigationBar(binding.textViewPoweredByMoviesSearch)

        binding.swipeRefreshLayoutMoviesSearch.also {
            it.setSwipeableChildren(R.id.scrollViewMoviesSearch, R.id.recyclerViewMoviesSearch)
            it.setOnRefreshListener { refreshList() }
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, it)
        }

        // setup empty view button
        binding.emptyViewMoviesSearch.apply {
            setButtonText(R.string.action_try_again)
            setButtonClickListener { refreshList() }
            // do not show error message when initially loading
            isGone = true
        }

        // setup grid view
        binding.recyclerViewMoviesSearch.apply {
            setHasFixedSize(true)
            layoutManager =
                AutoGridLayoutManager(
                    context,
                    R.dimen.movie_grid_column_width,
                    1,
                    1
                )
        }

        adapter = MoviesSearchAdapter(requireContext(), MovieClickListenerImpl(requireContext()))
        binding.recyclerViewMoviesSearch.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .collectLatest { loadStates ->
                    Timber.d("loadStates=$loadStates")
                    val refresh = loadStates.refresh
                    binding.swipeRefreshLayoutMoviesSearch.isRefreshing =
                        refresh is LoadState.Loading
                    if (refresh is LoadState.Error) {
                        binding.emptyViewMoviesSearch.apply {
                            setMessage(refresh.error.message)
                            setButtonGone(false)
                            isVisible = true
                        }
                        binding.recyclerViewMoviesSearch.isGone = true
                    } else if (refresh is LoadState.NotLoading
                        && adapter.itemCount == 0
                        // Only no results if displaying link or has a search query
                        && (activityModel.link != null || activityModel.queryString.value.isNotEmpty())) {
                        binding.emptyViewMoviesSearch.apply {
                            setMessage(R.string.empty_no_results)
                            // No point in refreshing if there are no results
                            setButtonGone(true)
                            isVisible = true
                        }
                        binding.recyclerViewMoviesSearch.isGone = true
                    } else {
                        binding.emptyViewMoviesSearch.isGone = true
                        binding.recyclerViewMoviesSearch.isVisible = true
                    }
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            activityModel.items.collectLatest {
                adapter.submitData(it)
            }
        }
    }

    private fun refreshList() {
        adapter.refresh()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventLanguageChanged(
        @Suppress("UNUSED_PARAMETER")
        event: MovieLocalizationDialogFragment.LocalizationChangedEvent?
    ) {
        refreshList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewMoviesSearch
    }
}