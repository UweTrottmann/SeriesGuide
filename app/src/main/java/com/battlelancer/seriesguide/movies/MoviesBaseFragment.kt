// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.MoviesActivity
import com.battlelancer.seriesguide.ui.widgets.SgFastScroller
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Base class for a fragment displaying movies.
 */
abstract class MoviesBaseFragment : Fragment() {

    private lateinit var adapter: MoviesAdapter

    abstract val model: MoviesWatchedViewModel

    @get:StringRes
    abstract val emptyViewTextResId: Int

    abstract val recyclerView: RecyclerView

    abstract val emptyView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // note: fragment is in static view pager tab so will never be destroyed if swiped away
        EventBus.getDefault().register(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        emptyView.setText(emptyViewTextResId)

        adapter = MoviesAdapter(requireContext(), MovieClickListenerImpl(requireContext()))

        recyclerView.also {
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

        ViewModelProvider(requireActivity())[MoviesActivityViewModel::class.java]
            .scrollTabToTopLiveData
            .observe(viewLifecycleOwner) {
                if (it != null) {
                    if (it.tabPosition == getTabPosition(it.isShowingNowTab)) {
                        recyclerView.scrollToPosition(0)
                    }
                }
            }

        viewLifecycleOwner.lifecycleScope.launch {
            adapter.onPagesUpdatedFlow.conflate().collectLatest {
                emptyView.isGone = adapter.itemCount > 0
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            model.items.collectLatest {
                adapter.submitData(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: MoviesDistillationSettings.MoviesSortOrderChangedEvent) {
        model.updateQueryString()
    }

    /**
     * @return The current position in the tab strip.
     * @see MoviesActivity
     */
    internal abstract fun getTabPosition(showingNowTab: Boolean): Int

}
