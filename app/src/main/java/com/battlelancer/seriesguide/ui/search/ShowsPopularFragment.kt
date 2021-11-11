package com.battlelancer.seriesguide.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentShowsPopularBinding
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
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

        binding.swipeRefreshLayoutShowsPopular.apply {
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, this)
            setOnRefreshListener { adapter.refresh() }
        }

        snackbar =
            Snackbar.make(binding.swipeRefreshLayoutShowsPopular, "", Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.action_try_again) { adapter.refresh() }

        binding.recyclerViewShowsPopular.apply {
            setHasFixedSize(true)
            layoutManager = AutoGridLayoutManager(context, R.dimen.showgrid_columnWidth, 1, 1)
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

}