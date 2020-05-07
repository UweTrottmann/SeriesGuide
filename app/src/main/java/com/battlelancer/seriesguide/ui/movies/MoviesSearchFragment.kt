package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesSearchBinding
import com.battlelancer.seriesguide.ui.MoviesActivity
import com.battlelancer.seriesguide.util.ViewTools

/**
 * Integrates with a search interface and displays movies based on query results. Can pre-populate
 * the displayed movies based on a sent link.
 */
class MoviesSearchFragment : Fragment() {

    internal interface OnSearchClickListener {
        fun onSearchClick()
    }

    private var _binding: FragmentMoviesSearchBinding? = null
    private val binding get() = _binding!!

    private var link: MoviesDiscoverLink? = null
    private lateinit var searchClickListener: OnSearchClickListener
    private lateinit var adapter: MoviesAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)

        searchClickListener = try {
            context as OnSearchClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnSearchClickListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        link = MoviesDiscoverLink.fromId(requireArguments().getInt(ARG_ID_LINK))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMoviesSearchBinding.inflate(inflater, container, false)

        binding.swipeRefreshLayoutMoviesSearch.also {
            it.setSwipeableChildren(R.id.scrollViewMoviesSearch, R.id.recyclerViewMoviesSearch)
            it.setOnRefreshListener(onRefreshListener)
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, it)
        }

        // setup grid view
        val layoutManager = AutoGridLayoutManager(
            context, R.dimen.movie_grid_columnWidth, 1, 1
        )
        binding.recyclerViewMoviesSearch.also {
            it.setHasFixedSize(true)
            it.layoutManager = layoutManager
        }

        // setup empty view button
        binding.emptyViewMoviesSearch.setButtonClickListener { searchClickListener.onSearchClick() }

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // setup adapter
        adapter = MoviesAdapter(requireContext(), MovieClickListener(requireContext()))
        binding.recyclerViewMoviesSearch.adapter = adapter

        binding.swipeRefreshLayoutMoviesSearch.isRefreshing = true
        LoaderManager.getInstance(this)
            .initLoader(
                MoviesActivity.SEARCH_LOADER_ID, buildLoaderArgs(null),
                searchLoaderCallbacks
            )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun buildLoaderArgs(query: String?): Bundle {
        val args = Bundle()
        args.putInt(ARG_ID_LINK, link?.id ?: -1)
        args.putString(ARG_SEARCH_QUERY, query)
        return args
    }

    fun search(query: String?) {
        if (!binding.swipeRefreshLayoutMoviesSearch.isRefreshing) {
            binding.swipeRefreshLayoutMoviesSearch.isRefreshing = true
        }
        LoaderManager.getInstance(this)
            .restartLoader(
                MoviesActivity.SEARCH_LOADER_ID, buildLoaderArgs(query),
                searchLoaderCallbacks
            )
    }

    private val searchLoaderCallbacks: LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TmdbMoviesLoader.Result> {

            override fun onCreateLoader(
                id: Int,
                args: Bundle?
            ): Loader<TmdbMoviesLoader.Result> {
                var link = MoviesDiscoverAdapter.DISCOVER_LINK_DEFAULT
                var query: String? = null
                if (args != null) {
                    link = MoviesDiscoverLink.fromId(args.getInt(ARG_ID_LINK))
                    query = args.getString(ARG_SEARCH_QUERY)
                }
                return TmdbMoviesLoader(requireContext(), link, query)
            }

            override fun onLoadFinished(
                loader: Loader<TmdbMoviesLoader.Result>,
                data: TmdbMoviesLoader.Result
            ) {
                if (!isAdded) {
                    return
                }
                val results = data.results
                val hasNoResults = results == null || results.isEmpty()

                binding.emptyViewMoviesSearch.setMessage(data.emptyText)
                binding.emptyViewMoviesSearch.isGone = !hasNoResults
                binding.recyclerViewMoviesSearch.isGone = hasNoResults
                binding.swipeRefreshLayoutMoviesSearch.isRefreshing = false

                adapter.updateMovies(results)
            }

            override fun onLoaderReset(loader: Loader<TmdbMoviesLoader.Result>) {
                adapter.updateMovies(null)
            }
        }

    private val onRefreshListener = OnRefreshListener { searchClickListener.onSearchClick() }

    companion object {

        private const val ARG_SEARCH_QUERY = "search_query"
        private const val ARG_ID_LINK = "linkId"

        @JvmStatic
        fun newInstance(link: MoviesDiscoverLink): MoviesSearchFragment {
            val f = MoviesSearchFragment()
            val args = Bundle()
            args.putInt(ARG_ID_LINK, link.id)
            f.arguments = args
            return f
        }
    }
}