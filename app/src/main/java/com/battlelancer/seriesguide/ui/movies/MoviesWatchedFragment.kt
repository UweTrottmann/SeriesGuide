package com.battlelancer.seriesguide.ui.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.MoviesActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MoviesWatchedFragment : Fragment() {

    companion object {
        fun newInstance() = MoviesWatchedFragment()
    }

    @BindView(R.id.textViewEmptyMoviesWatched)
    lateinit var textViewEmpty: View
    @BindView(R.id.recyclerViewMoviesWatched)
    lateinit var recyclerView: RecyclerView
    private lateinit var unbinder: Unbinder

    private lateinit var viewModel: MoviesWatchedViewModel
    private lateinit var adapter: MoviesWatchedAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_movies_watched, container, false)
        unbinder = ButterKnife.bind(this, v)
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MoviesWatchedAdapter(requireContext(), MovieClickListener(requireContext()))

        recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager = AutoGridLayoutManager(
                context, R.dimen.movie_grid_columnWidth, 1, 3
            )
            it.adapter = adapter
        }

        ViewModelProvider(requireActivity()).get(MoviesActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(
                viewLifecycleOwner,
                Observer {
                    if (it != null) {
                        val positionOfThisTab = if (it.isShowingNowTab) {
                            MoviesActivity.TAB_POSITION_WATCHED_WITH_NOW
                        } else {
                            MoviesActivity.TAB_POSITION_WATCHED_DEFAULT
                        }
                        if (it.tabPosition == positionOfThisTab) {
                            recyclerView.scrollToPosition(0)
                        }
                    }
                }
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // note: fragment is in static view pager tab so will never be destroyed if swiped away
        EventBus.getDefault().register(this)
    }

    // note: can not use onCreate, causes issues with ViewPager this tab is in
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MoviesWatchedViewModel::class.java)
        viewModel.movieList.observe(viewLifecycleOwner, Observer {
            textViewEmpty.isGone = it.size > 0
            adapter.submitList(it)
        })

        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        MoviesOptionsMenu(requireContext()).create(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (MoviesOptionsMenu(requireContext()).onItemSelected(item, requireActivity())) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: MoviesDistillationSettings.MoviesSortOrderChangedEvent) {
        viewModel.updateQueryString()
    }

}
