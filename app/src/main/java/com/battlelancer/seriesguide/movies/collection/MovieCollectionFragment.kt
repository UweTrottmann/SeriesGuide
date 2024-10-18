// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentShowsSimilarBinding
import com.battlelancer.seriesguide.movies.base.BaseMovieListAdapter
import com.battlelancer.seriesguide.movies.base.SearchMenuProvider
import com.battlelancer.seriesguide.movies.collection.MovieCollectionViewModel.MoviesListUiState
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Displays movies of a TMDB movie collection.
 *
 * Re-uses [FragmentShowsSimilarBinding] layout.
 */
class MovieCollectionFragment : Fragment() {

    private val viewModel: MovieCollectionViewModel by viewModels(
        extrasProducer = {
            MovieCollectionViewModel.creationExtras(
                defaultViewModelCreationExtras,
                requireArguments().getInt(ARG_COLLECTION_ID)
            )
        },
        factoryProducer = { MovieCollectionViewModel.Factory }
    )
    private var binding: FragmentShowsSimilarBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentShowsSimilarBinding.inflate(layoutInflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding!!
        ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewShowsSimilar)
        ThemeUtils.applyBottomMarginForNavigationBar(binding.textViewPoweredByShowsSimilar)

        binding.swipeRefreshLayoutShowsSimilar.apply {
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, this)
            setOnRefreshListener { viewModel.refresh() }
        }
        binding.emptyViewShowsSimilar.setButtonClickListener {
            binding.swipeRefreshLayoutShowsSimilar.isRefreshing = true
            viewModel.refresh()
        }

        binding.swipeRefreshLayoutShowsSimilar.isRefreshing = true
        binding.emptyViewShowsSimilar.isGone = true

        val adapter = BaseMovieListAdapter(requireContext())
        binding.recyclerViewShowsSimilar.also {
            it.setHasFixedSize(true)
            it.layoutManager =
                AutoGridLayoutManager(it.context, R.dimen.movie_grid_column_width, 1, 1)
            it.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.movies.collectLatest {
                binding.swipeRefreshLayoutShowsSimilar.isRefreshing = false
                when (it) {
                    is MoviesListUiState.Error -> {
                        binding.recyclerViewShowsSimilar.isGone = true
                        binding.emptyViewShowsSimilar.apply {
                            isVisible = true
                            setMessage(it.message)
                        }
                    }

                    is MoviesListUiState.Success -> {
                        // Note: no need to handle empty state, collection should never be empty
                        adapter.submitList(it.movies)
                        binding.recyclerViewShowsSimilar.isVisible = true
                        binding.emptyViewShowsSimilar.isGone = true
                    }
                }
            }
        }

        requireActivity().addMenuProvider(
            SearchMenuProvider(requireContext()),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewShowsSimilar

        private const val ARG_COLLECTION_ID = "COLLECTION_ID"

        fun buildArgs(collectionId: Int): Bundle = bundleOf(
            ARG_COLLECTION_ID to collectionId
        )
    }

}
