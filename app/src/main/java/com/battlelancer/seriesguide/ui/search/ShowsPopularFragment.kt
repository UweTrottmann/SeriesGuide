package com.battlelancer.seriesguide.ui.search

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
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

class ShowsPopularFragment : Fragment() {

    @BindView(R.id.swipeRefreshLayoutShowsPopular)
    lateinit var swipeRefreshLayout: EmptyViewSwipeRefreshLayout

    @BindView(R.id.recyclerViewShowsPopular)
    lateinit var recyclerView: RecyclerView

    @BindView(R.id.emptyViewShowsPopular)
    lateinit var emptyView: EmptyView

    private lateinit var unbinder: Unbinder
    private lateinit var model: ShowsPopularViewModel
    private lateinit var adapter: ShowsPopularAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shows_popular, container, false).also {
            unbinder = ButterKnife.bind(this, it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout.apply {
            ViewTools.setSwipeRefreshLayoutColors(activity!!.theme, this)
            setSwipeableChildren(R.id.scrollViewShowsPopular, R.id.recyclerViewShowsPopular)
            setOnRefreshListener { model.refresh() }
        }

        emptyView.visibility = View.GONE
        emptyView.setButtonClickListener { model.refresh() }

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = AutoGridLayoutManager(context, R.dimen.showgrid_columnWidth, 1, 1)
        }

        adapter = ShowsPopularAdapter()
        recyclerView.adapter = adapter
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model = ViewModelProviders.of(this).get(ShowsPopularViewModel::class.java)
        model.items.observe(this, Observer {
            adapter.submitList(it)
        })
        model.networkState.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
            if (it?.status == Status.ERROR) {
                emptyView.setMessage(it.message)
                emptyView.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

}