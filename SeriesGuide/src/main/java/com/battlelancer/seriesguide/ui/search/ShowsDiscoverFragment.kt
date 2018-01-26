package com.battlelancer.seriesguide.ui.search

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
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
import com.battlelancer.seriesguide.util.ViewTools
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
    private lateinit var adapter: ShowsDiscoverAdapter
    private lateinit var model: ShowsDiscoverViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shows_discover, container, false).also {
            unbinder = ButterKnife.bind(this, it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout.setSwipeableChildren(R.id.scrollViewShowsDiscover,
                R.id.recyclerViewShowsDiscover)
        swipeRefreshLayout.setOnRefreshListener { loadResults() }
        swipeRefreshLayout.isRefreshing = false
        ViewTools.setSwipeRefreshLayoutColors(activity!!.theme, swipeRefreshLayout)

        emptyView.setButtonClickListener { loadResults() }

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

        adapter = ShowsDiscoverAdapter()
        recyclerView.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model = ViewModelProviders.of(this).get(ShowsDiscoverViewModel::class.java)
        model.data.observe(this, Observer { handleResultsUpdate(it) })
        loadResults()
    }

    private fun loadResults() {
        model.data.load()
    }

    private fun handleResultsUpdate(result: ShowsDiscoverLiveData.Result?) {
        result?.let {
            val hasResults = result.searchResults.isNotEmpty()
            emptyView.visibility = if (hasResults) View.GONE else View.VISIBLE
            recyclerView.visibility = if (hasResults) View.VISIBLE else View.GONE
            swipeRefreshLayout.isRefreshing = false
            emptyView.setMessage(result.emptyText)
            adapter.updateSearchResults(result.searchResults)
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