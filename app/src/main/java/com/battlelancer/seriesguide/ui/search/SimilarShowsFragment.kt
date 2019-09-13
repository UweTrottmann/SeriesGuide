package com.battlelancer.seriesguide.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.widgets.EmptyView
import com.uwetrottmann.seriesguide.widgets.EmptyViewSwipeRefreshLayout
import timber.log.Timber

class SimilarShowsFragment: Fragment() {

    private var showTvdbId: Int = 0

    private val similarShowsViewModel: SimilarShowsViewModel by viewModels {
        SimilarShowsViewModelFactory(activity!!.application, showTvdbId)
    }

    private lateinit var swipeRefreshLayout: EmptyViewSwipeRefreshLayout
    private lateinit var emptyView: EmptyView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showTvdbId = arguments!!.getInt(ARG_SHOW_TVDB_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_shows_similar, container, false)
        swipeRefreshLayout = v.findViewById(R.id.swipeRefreshLayoutShowsSimilar)
        emptyView = v.findViewById(R.id.emptyViewShowsSimilar)
        recyclerView = v.findViewById(R.id.recyclerViewShowsSimilar)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipeRefreshLayout.apply {
            ViewTools.setSwipeRefreshLayoutColors(activity!!.theme, this)
            setOnRefreshListener { similarShowsViewModel.loadSimilarShows(showTvdbId) }
        }
        swipeRefreshLayout.isRefreshing = true
        emptyView.isGone = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        similarShowsViewModel.resultsLiveData.observe(this, Observer {
            Timber.i("Found %s similar shows!", it.size)
            swipeRefreshLayout.isRefreshing = false
        })
        similarShowsViewModel.errorLiveData.observe(this, Observer { message ->
            recyclerView.isGone = message != null
            emptyView.isGone = message == null
            emptyView.setMessage(message)
        })
    }

    companion object {
        const val ARG_SHOW_TVDB_ID = "ARG_SHOW_TVDB_ID"

        fun newInstance(showTvdbId: Int): SimilarShowsFragment {
            return SimilarShowsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SHOW_TVDB_ID, showTvdbId)
                }
            }
        }
    }
}