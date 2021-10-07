package com.battlelancer.seriesguide.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.SearchActivity
import com.battlelancer.seriesguide.ui.movies.AutoGridLayoutManager
import com.battlelancer.seriesguide.util.ViewTools
import com.battlelancer.seriesguide.widgets.EmptyView
import com.uwetrottmann.seriesguide.common.SingleLiveEvent
import com.uwetrottmann.seriesguide.widgets.EmptyViewSwipeRefreshLayout

class SimilarShowsFragment : BaseAddShowsFragment() {

    private var showTmdbId: Int = 0
    private var showTitle: String? = null

    private val similarShowsViewModel: SimilarShowsViewModel by viewModels {
        SimilarShowsViewModelFactory(requireActivity().application, showTmdbId)
    }

    private lateinit var swipeRefreshLayout: EmptyViewSwipeRefreshLayout
    private lateinit var emptyView: EmptyView
    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: SimilarShowsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showTmdbId = requireArguments().getInt(ARG_SHOW_TMDB_ID)
        showTitle = requireArguments().getString(ARG_SHOW_TITLE)

        setHasOptionsMenu(true)
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
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, this)
            setOnRefreshListener { loadSimilarShows() }
        }
        emptyView.setButtonClickListener {
            swipeRefreshLayout.isRefreshing = true
            loadSimilarShows()
        }

        swipeRefreshLayout.isRefreshing = true
        emptyView.isGone = true

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = AutoGridLayoutManager(context, R.dimen.showgrid_columnWidth, 1, 1)
        }

        adapter = SimilarShowsAdapter(itemClickListener)
        recyclerView.adapter = adapter

        similarShowsViewModel.resultLiveData.observe(viewLifecycleOwner, {
            adapter.submitList(it.results)
            emptyView.setMessage(it.emptyMessage)
            recyclerView.isGone = it.results.isNullOrEmpty()
            emptyView.isGone = !it.results.isNullOrEmpty()
            swipeRefreshLayout.isRefreshing = false
        })
    }

    private fun loadSimilarShows() {
        similarShowsViewModel.loadSimilarShows(showTmdbId)
    }

    override fun onStart() {
        super.onStart()
        // Set (or restore if going back) the show title as action bar subtitle.
        (activity as AppCompatActivity).supportActionBar?.subtitle = showTitle
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.add(0, MENU_ITEM_SEARCH_ID, 0, R.string.search).apply {
            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            setIcon(R.drawable.ic_search_white_24dp)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_ITEM_SEARCH_ID -> {
                startActivity(
                    Intent(activity, SearchActivity::class.java).putExtra(
                        SearchActivity.EXTRA_DEFAULT_TAB, SearchActivity.TAB_POSITION_SEARCH
                    )
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun setAllPendingNotAdded() {
        similarShowsViewModel.setAllPendingNotAdded()
    }

    override fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        similarShowsViewModel.setStateForTmdbId(showTmdbId, newState)
    }

    companion object {
        private const val ARG_SHOW_TMDB_ID = "ARG_SHOW_TMDB_ID"
        private const val ARG_SHOW_TITLE = "ARG_SHOW_TITLE"
        private const val MENU_ITEM_SEARCH_ID = 1

        @JvmStatic
        val displaySimilarShowsEventLiveData = SingleLiveEvent<SearchResult>()

        fun newInstance(showTmdbId: Int, showTitle: String?): SimilarShowsFragment {
            return SimilarShowsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_SHOW_TMDB_ID, showTmdbId)
                    putString(ARG_SHOW_TITLE, showTitle)
                }
            }
        }
    }
}