package com.battlelancer.seriesguide.movies.similar

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.recyclerview.widget.RecyclerView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.movies.search.MoviesSearchActivity
import com.battlelancer.seriesguide.ui.AutoGridLayoutManager
import com.battlelancer.seriesguide.ui.widgets.EmptyView
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.ViewTools
import com.uwetrottmann.seriesguide.widgets.EmptyViewSwipeRefreshLayout

class SimilarMoviesFragment : Fragment() {

    private var tmdbId: Int = 0
    private var title: String? = null

    private val viewModel: SimilarMoviesViewModel by viewModels(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                set(
                    SimilarMoviesViewModel.KEY_TMDB_ID_MOVIE,
                    requireArguments().getInt(ARG_TMDB_ID)
                )
            }
        },
        factoryProducer = { SimilarMoviesViewModel.Factory }
    )

    private lateinit var swipeRefreshLayout: EmptyViewSwipeRefreshLayout
    private lateinit var emptyView: EmptyView
    private lateinit var recyclerView: RecyclerView

    private lateinit var adapter: SimilarMoviesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tmdbId = requireArguments().getInt(ARG_TMDB_ID)
        title = requireArguments().getString(ARG_TITLE)

        (activity as AppCompatActivity).supportActionBar?.subtitle = title
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
        ThemeUtils.applyBottomPaddingForNavigationBar(recyclerView)
        ThemeUtils.applyBottomMarginForNavigationBar(view.findViewById(R.id.textViewPoweredByShowsSimilar))

        swipeRefreshLayout.apply {
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, this)
            setOnRefreshListener { load() }
        }
        emptyView.setButtonClickListener {
            swipeRefreshLayout.isRefreshing = true
            load()
        }

        swipeRefreshLayout.isRefreshing = true
        emptyView.isGone = true

        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager =
                AutoGridLayoutManager(
                    context,
                    R.dimen.movie_grid_columnWidth,
                    1,
                    1
                )
        }

        adapter = SimilarMoviesAdapter(requireContext())
        recyclerView.adapter = adapter

        viewModel.resultLiveData.observe(viewLifecycleOwner) {
            adapter.submitList(it.results)
            emptyView.setMessage(it.emptyMessage)
            recyclerView.isGone = it.results.isNullOrEmpty()
            emptyView.isGone = !it.results.isNullOrEmpty()
            swipeRefreshLayout.isRefreshing = false
        }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun load() {
        viewModel.loadSimilarMovies(tmdbId)
    }

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menu.add(0, MENU_ITEM_SEARCH_ID, 0, R.string.search).apply {
                setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                setIcon(R.drawable.ic_search_white_24dp)
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                MENU_ITEM_SEARCH_ID -> {
                    startActivity(Intent(requireContext(), MoviesSearchActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    companion object {
        val liftOnScrollTargetViewId = R.id.recyclerViewShowsSimilar

        private const val ARG_TMDB_ID = "ARG_TMDB_ID"
        private const val ARG_TITLE = "ARG_TITLE"
        private const val MENU_ITEM_SEARCH_ID = 1

        fun newInstance(tmdbId: Int, title: String?): SimilarMoviesFragment {
            return SimilarMoviesFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TMDB_ID, tmdbId)
                    putString(ARG_TITLE, title)
                }
            }
        }
    }
}