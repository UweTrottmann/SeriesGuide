package com.battlelancer.seriesguide.ui.streams

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.ViewTools
import com.uwetrottmann.seriesguide.widgets.EmptyViewSwipeRefreshLayout
import com.uwetrottmann.seriesguide.widgets.gridheaderview.StickyGridHeadersGridView
import com.uwetrottmann.androidutils.AndroidUtils

/**
 * Displays a stream of activities that can be refreshed by the user via a swipe gesture (or an
 * action item).
 */
abstract class StreamFragment : Fragment() {

    private lateinit var contentContainer: EmptyViewSwipeRefreshLayout
    private lateinit var gridView: StickyGridHeadersGridView
    private lateinit var emptyView: TextView

    private var adapter: ListAdapter? = null

    /**
     * Implementers should create their grid view adapter here.
     */
    protected abstract val listAdapter: ListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stream, container, false)
        contentContainer = view.findViewById(R.id.swipeRefreshLayoutStream)
        gridView = view.findViewById(R.id.gridViewStream)
        emptyView = view.findViewById(R.id.emptyViewStream)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        contentContainer.apply {
            setSwipeableChildren(R.id.scrollViewStream, R.id.gridViewStream)
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

        gridView.also {
            it.emptyView = emptyView
            it.setAreHeadersSticky(false)
        }

        // set initial view states
        showProgressBar(true)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        ViewTools.setSwipeRefreshLayoutColors(activity!!.theme, contentContainer)

        if (adapter == null) {
            adapter = listAdapter
        }
        gridView.adapter = adapter

        initializeStream()

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
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
        TraktCredentials.ensureCredentials(activity)

        // intercept loader call if offline to avoid replacing data with error message
        // once trakt data has proper cache headers this might become irrelevant
        if (!AndroidUtils.isNetworkConnected(activity!!)) {
            showProgressBar(false)
            setEmptyMessage(getString(R.string.offline))
            Toast.makeText(activity, R.string.offline, Toast.LENGTH_SHORT).show()
            return
        }

        refreshStream()
    }

    /**
     * Changes the empty message.
     */
    protected fun setEmptyMessage(emptyMessage: String) {
        emptyView.text = emptyMessage
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
        val intent = Intent(activity!!, EpisodesActivity::class.java)
            .putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId)

        ActivityCompat.startActivity(
            activity!!, intent,
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
        contentContainer.isRefreshing = isShowing
    }

    companion object {
        private const val TRAKT_HISTORY_URL = "https://trakt.tv/users/me/history/"
    }
}
