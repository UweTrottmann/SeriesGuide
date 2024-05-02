// SPDX-License-Identifier: Apache-2.0
// Copyright 2020-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesSearchBinding
import com.battlelancer.seriesguide.movies.MovieClickListenerImpl
import com.battlelancer.seriesguide.movies.MovieLocalizationDialogFragment
import com.battlelancer.seriesguide.movies.MoviesDiscoverLink
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Integrates with a search interface and displays movies based on query results. Can pre-populate
 * the displayed movies based on a sent link.
 */
class MoviesSearchFragment : Fragment() {

    private var _binding: FragmentMoviesSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var link: MoviesDiscoverLink
    private lateinit var adapter: MoviesSearchAdapter

    private val activityModel: MoviesSearchViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = MoviesDiscoverLink.fromId(requireArguments().getInt(ARG_ID_LINK))
    }

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
        binding.emptyViewMoviesSearch.setButtonClickListener { refreshList() }

        // setup grid view
        binding.recyclerViewMoviesSearch.apply {
            setHasFixedSize(true)
            layoutManager =
                AutoGridLayoutManager(
                    context,
                    R.dimen.movie_grid_columnWidth,
                    1,
                    1
                )
        }

        adapter = MoviesSearchAdapter(requireContext(), MovieClickListenerImpl(requireContext()))
        binding.recyclerViewMoviesSearch.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.onPagesUpdatedFlow.conflate().collectLatest {
                val hasNoResults = adapter.itemCount == 0
                binding.emptyViewMoviesSearch.isGone = !hasNoResults
                binding.recyclerViewMoviesSearch.isGone = hasNoResults
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.loadStateFlow
                .distinctUntilChangedBy { it.refresh }
                .collectLatest { loadStates ->
                    Timber.d("loadStates=$loadStates")
                    val refresh = loadStates.refresh
                    binding.swipeRefreshLayoutMoviesSearch.isRefreshing =
                        refresh is LoadState.Loading
                    if (refresh is LoadState.Error) {
                        binding.emptyViewMoviesSearch.setMessage(refresh.error.message)
                    } else {
                        binding.emptyViewMoviesSearch.setMessage(R.string.no_results)
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

        private const val ARG_ID_LINK = "linkId"

        @JvmStatic
        fun newInstance(link: MoviesDiscoverLink): MoviesSearchFragment {
            val f = MoviesSearchFragment()
            val args = Bundle()
            args.putInt(ARG_ID_LINK, link.id)
            f.arguments = args
            return f
        }
    }
}