package com.battlelancer.seriesguide.ui.search

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.widgets.EmptyView
import com.battlelancer.seriesguide.widgets.EmptyViewSwipeRefreshLayout

/**
 * Displays a list of shows with new episodes with links to popular shows and if connected to trakt
 * also with links to recommendations, watched and collected shows. If a search event is received,
 * displays search results. If a link is clicked launches a new activity to display them.
 */
class ShowsDiscoverFragment : Fragment() {

    @BindView(R.id.swipeRefreshLayoutShowsDiscover)
    lateinit var swipeRefreshLayout: EmptyViewSwipeRefreshLayout

    @BindView(R.id.recyclerViewShowsDiscover)
    lateinit var recyclerView: RecyclerView

    @BindView(R.id.emptyViewShowsDiscover)
    lateinit var emptyView: EmptyView

    private lateinit var unbinder: Unbinder

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shows_discover, container, false).also {
            unbinder = ButterKnife.bind(this, it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout.setSwipeableChildren(R.id.scrollViewShowsDiscover,
                R.id.recyclerViewShowsDiscover)
        swipeRefreshLayout.setOnRefreshListener { TODO("Refresh") }

        emptyView.setButtonClickListener { TODO("Refresh") }

        val layoutManager = AutoGridLayoutManager(context, R.dimen.showgrid_columnWidth,
                1, 1).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return 1
                }
            }
        }

        recyclerView.apply {
            setHasFixedSize(true)
            this.layoutManager = layoutManager
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // when switching tabs while still showing refresh animation, old content remains stuck
        // so force clear the drawing cache and animation: http://stackoverflow.com/a/27073879
        if (swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.apply {
                isRefreshing = false
                destroyDrawingCache()
                clearAnimation()
            }
        }

        unbinder.unbind()
    }

}