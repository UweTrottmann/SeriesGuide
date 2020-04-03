package com.battlelancer.seriesguide.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ViewTools
import com.google.android.material.snackbar.Snackbar

/**
 * Displays a list of popular shows to add that are paged in from the network.
 */
class ShowsPopularFragment : BaseAddShowsFragment() {

    @BindView(R.id.swipeRefreshLayoutShowsPopular)
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    @BindView(R.id.recyclerViewShowsPopular)
    lateinit var recyclerView: RecyclerView

    private lateinit var snackbar: Snackbar

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
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, this)
            setOnRefreshListener { model.refresh() }
        }

        snackbar = Snackbar.make(swipeRefreshLayout, "", Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.action_try_again) { model.refresh() }

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = AutoGridLayoutManager(context, R.dimen.showgrid_columnWidth, 1, 1)
        }

        adapter = ShowsPopularAdapter(itemClickListener)
        recyclerView.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model = ViewModelProvider(this).get(ShowsPopularViewModel::class.java)
        model.items.observe(viewLifecycleOwner, Observer {
            adapter.submitList(it)
        })
        model.networkState.observe(viewLifecycleOwner, Observer {
            swipeRefreshLayout.isRefreshing = it == NetworkState.LOADING
            if (it?.status == Status.ERROR) {
                snackbar.setText(it.message!!)
                if (!snackbar.isShownOrQueued) snackbar.show()
            } else {
                if (snackbar.isShownOrQueued) snackbar.dismiss()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun setAllPendingNotAdded() {
        adapter.setAllPendingNotAdded()
    }

    override fun setStateForTvdbId(showTvdbId: Int, newState: Int) {
        adapter.setStateForTvdbId(showTvdbId, newState)
    }

}