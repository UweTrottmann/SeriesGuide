// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentStreamBinding
import com.battlelancer.seriesguide.history.TraktEpisodeHistoryLoader.HistoryItem
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.widgets.SgFastScroller
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.uwetrottmann.androidutils.AndroidUtils

/**
 * Displays a stream of activities that can be refreshed by the user via a swipe gesture (or an
 * action item).
 */
abstract class StreamFragment : Fragment() {

    private var _binding: FragmentStreamBinding? = null
    private val binding get() = _binding!!

    /**
     * Implementers should create their history adapter here.
     */
    protected abstract val listAdapter: BaseHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStreamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ThemeUtils.applyBottomPaddingForNavigationBar(binding.recyclerViewStream)
        ThemeUtils.applyBottomMarginForNavigationBar(binding.floatingActionButtonStream)

        ViewTools.openUriOnClick(binding.floatingActionButtonStream, TRAKT_HISTORY_URL)

        binding.swipeRefreshLayoutStream.apply {
            setSwipeableChildren(R.id.scrollViewStream, R.id.recyclerViewStream)
            setOnRefreshListener { refreshStreamWithNetworkCheck() }
            setProgressViewOffset(
                false, resources.getDimensionPixelSize(
                    R.dimen.swipe_refresh_progress_bar_start_margin
                ),
                resources.getDimensionPixelSize(
                    R.dimen.swipe_refresh_progress_bar_end_margin
                )
            )
        }
        ViewTools.setSwipeRefreshLayoutColors(
            requireActivity().theme,
            binding.swipeRefreshLayoutStream
        )

        val layoutManager =
            AutoGridLayoutManager(
                context,
                R.dimen.showgrid_columnWidth, 1, 1,
                listAdapter
            )

        binding.recyclerViewStream.also {
            it.setHasFixedSize(true)
            it.layoutManager = layoutManager
            it.adapter = listAdapter
        }
        SgFastScroller(requireContext(), binding.recyclerViewStream)

        // set initial view states
        showProgressBar(true)

        initializeStream()

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.stream_menu, menu)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_action_stream_refresh -> {
                    refreshStreamWithNetworkCheck()
                    true
                }
                else -> false
            }
        }
    }

    private fun refreshStreamWithNetworkCheck() {
        // launch trakt connect flow if disconnected
        TraktCredentials.ensureCredentials(requireContext())

        // intercept loader call if offline to avoid replacing data with error message
        // once trakt data has proper cache headers this might become irrelevant
        if (!AndroidUtils.isNetworkConnected(requireContext())) {
            showProgressBar(false)
            setEmptyMessage(getString(R.string.offline))
            Toast.makeText(requireContext(), R.string.offline, Toast.LENGTH_SHORT).show()
            return
        }

        refreshStream()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Submits data and an sets empty message to be shown if the data list is empty.
     */
    fun setListData(data: List<HistoryItem>, emptyMessage: String) {
        listAdapter.submitList(data)
        setEmptyMessage(emptyMessage)
        showProgressBar(false)
        binding.recyclerViewStream.isGone = data.isEmpty()
        binding.emptyViewStream.isGone = data.isNotEmpty()
    }

    private fun setEmptyMessage(emptyMessage: String) {
        binding.emptyViewStream.text = emptyMessage
    }

    /**
     * Implementers should initialize the activity stream and supply the results to the grid
     * adapter.
     */
    protected abstract fun initializeStream()

    /**
     * Implementers should refresh the activity stream and replace the data of the grid adapter.
     * Once finished you should hide the progress bar with [.showProgressBar].
     */
    protected abstract fun refreshStream()

    /**
     * Show or hide the progress bar of the [SwipeRefreshLayout]
     * wrapping the stream view.
     */
    protected fun showProgressBar(isShowing: Boolean) {
        binding.swipeRefreshLayoutStream.isRefreshing = isShowing
    }

    companion object {
        private const val TRAKT_HISTORY_URL = "https://trakt.tv/users/me/history/"

        val liftOnScrollTargetViewId = R.id.recyclerViewStream
    }
}
