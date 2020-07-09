package com.battlelancer.seriesguide.ui.streams

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentStreamBinding
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.streams.TraktEpisodeHistoryLoader.HistoryItem
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.widgets.SgFastScroller
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.trakt5.entities.HistoryEntry

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

        // TODO recycler view
        val layoutManager = AutoGridLayoutManager(
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
        // TODO empty view

        // set initial view states
        showProgressBar(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ViewTools.setSwipeRefreshLayoutColors(
            requireActivity().theme,
            binding.swipeRefreshLayoutStream
        )

        initializeStream()

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.stream_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_action_stream_refresh -> {
                refreshStreamWithNetworkCheck()
                true
            }
            R.id.menu_action_stream_web -> {
                Utils.launchWebsite(context, TRAKT_HISTORY_URL)
                true
            }
            else -> super.onOptionsItemSelected(item)
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
     * Starts an activity to display the given episode.
     */
    protected fun showDetails(view: View, episodeId: Int) {
        val intent = Intent(requireActivity(), EpisodesActivity::class.java)
            .putExtra(EpisodesActivity.EXTRA_EPISODE_TVDBID, episodeId)

        ActivityCompat.startActivity(
            requireActivity(), intent,
            ActivityOptionsCompat
                .makeScaleUpAnimation(view, 0, 0, view.width, view.height)
                .toBundle()
        )
    }

    /**
     * Show or hide the progress bar of the [SwipeRefreshLayout]
     * wrapping the stream view.
     */
    protected fun showProgressBar(isShowing: Boolean) {
        binding.swipeRefreshLayoutStream.isRefreshing = isShowing
    }

    companion object {
        private const val TRAKT_HISTORY_URL = "https://trakt.tv/users/me/history/"
    }
}
