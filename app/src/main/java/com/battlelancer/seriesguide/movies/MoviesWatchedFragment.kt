// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesWatchedBinding
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.widgets.SgFastScroller
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MoviesWatchedFragment : Fragment() {

    private var binding: FragmentMoviesWatchedBinding? = null

    private val model by viewModels<MoviesWatchedViewModel>()
    private lateinit var adapter: MoviesWatchedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // note: fragment is in static view pager tab so will never be destroyed if swiped away
        EventBus.getDefault().register(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMoviesWatchedBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MoviesWatchedAdapter(requireContext(), MovieClickListenerImpl(requireContext()))

        binding!!.recyclerViewMoviesWatched.also {
            it.setHasFixedSize(true)
            it.layoutManager =
                AutoGridLayoutManager(
                    context, R.dimen.movie_grid_columnWidth, 1, 3
                )
            it.adapter = adapter
            SgFastScroller(requireContext(), it)
        }

        requireActivity().addMenuProvider(
            MoviesOptionsMenu(requireActivity()),
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )

        ViewModelProvider(requireActivity()).get(MoviesActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) {
                if (it != null) {
                    val positionOfThisTab = if (it.isShowingNowTab) {
                        MoviesActivityImpl.TAB_POSITION_WATCHED_WITH_NOW
                    } else {
                        MoviesActivityImpl.TAB_POSITION_WATCHED_DEFAULT
                    }
                    if (it.tabPosition == positionOfThisTab) {
                        binding?.recyclerViewMoviesWatched?.scrollToPosition(0)
                    }
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.onPagesUpdatedFlow.conflate().collectLatest {
                binding?.textViewEmptyMoviesWatched?.isGone = adapter.itemCount > 0
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            model.items.collectLatest {
                adapter.submitData(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: MoviesDistillationSettings.MoviesSortOrderChangedEvent) {
        model.updateQueryString()
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewMoviesWatched

        fun newInstance() = MoviesWatchedFragment()
    }

}
