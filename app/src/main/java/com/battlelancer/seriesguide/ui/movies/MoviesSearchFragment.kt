package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesSearchBinding
import com.battlelancer.seriesguide.ui.search.NetworkState
import com.battlelancer.seriesguide.ui.search.Status
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

    private lateinit var link: MoviesDiscoverLink
    private lateinit var searchClickListener: OnSearchClickListener
    private lateinit var adapter: MoviesSearchAdapter

    private val model: MoviesSearchViewModel by viewModels {
        MoviesSearchViewModelFactory(requireActivity().application, link)
    }

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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.swipeRefreshLayoutMoviesSearch.also {
            it.setSwipeableChildren(R.id.scrollViewMoviesSearch, R.id.recyclerViewMoviesSearch)
            it.setOnRefreshListener(onRefreshListener)
            ViewTools.setSwipeRefreshLayoutColors(requireActivity().theme, it)
        }

        // setup empty view button
        binding.emptyViewMoviesSearch.setButtonClickListener { searchClickListener.onSearchClick() }

        // setup grid view
        binding.recyclerViewMoviesSearch.apply {
            setHasFixedSize(true)
            layoutManager = AutoGridLayoutManager(context, R.dimen.movie_grid_columnWidth, 1, 1)
        }

        adapter = MoviesSearchAdapter(requireContext(), MovieClickListener(requireContext()))
        binding.recyclerViewMoviesSearch.adapter = adapter
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        model.pagedMovieList.observe(viewLifecycleOwner, Observer {
            val hasNoResults = it.size == 0
            binding.emptyViewMoviesSearch.isGone = !hasNoResults
            binding.recyclerViewMoviesSearch.isGone = hasNoResults

            adapter.submitList(it)
        })
        model.networkState.observe(viewLifecycleOwner, Observer {
            binding.swipeRefreshLayoutMoviesSearch.isRefreshing = it == NetworkState.LOADING
            // Note: empty view will not be visible if the previous page successfully loaded.
            if (it.status == Status.ERROR) {
                binding.emptyViewMoviesSearch.setMessage(it.message)
            } else {
                binding.emptyViewMoviesSearch.setMessage(R.string.no_results)
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun search(query: String?) {
        model.updateQuery(query)
    }

    private val onRefreshListener = OnRefreshListener { searchClickListener.onSearchClick() }

    companion object {

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