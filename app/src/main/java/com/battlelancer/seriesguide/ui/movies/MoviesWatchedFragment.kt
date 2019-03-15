package com.battlelancer.seriesguide.ui.movies

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
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

        adapter = MoviesWatchedAdapter(context!!, itemClickListener)

        recyclerView.also {
            it.setHasFixedSize(true)
            it.layoutManager = AutoGridLayoutManager(
                context, R.dimen.movie_grid_columnWidth, 1, 3
            )
            it.adapter = adapter
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MoviesWatchedViewModel::class.java)
        viewModel.movieList.observe(this, Observer {
            textViewEmpty.isGone = it.size > 0
            adapter.submitList(it)
        })
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventTabClick(event: MoviesActivity.MoviesTabClickEvent) {
        val positionOfThisTab = if (event.showingNowTab) {
            MoviesActivity.TAB_POSITION_WATCHED_WITH_NOW
        } else {
            MoviesActivity.TAB_POSITION_WATCHED_DEFAULT
        }
        if (event.position == positionOfThisTab) {
            recyclerView.smoothScrollToPosition(0)
        }
    }

    private val itemClickListener: MoviesAdapter.ItemClickListener =
        object : MoviesAdapter.ItemClickListener {
            override fun onClickMovie(movieTmdbId: Int, posterView: ImageView?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onClickMovieMoreOptions(movieTmdbId: Int, anchor: View?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

}
