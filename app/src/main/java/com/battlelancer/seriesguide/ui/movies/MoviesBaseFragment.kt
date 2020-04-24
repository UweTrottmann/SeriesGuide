package com.battlelancer.seriesguide.ui.movies

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.ui.MoviesActivity
import com.battlelancer.seriesguide.ui.movies.MoviesDistillationSettings.MoviesSortOrderChangedEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * A shell for a fragment displaying a number of movies.
 */
abstract class MoviesBaseFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    private lateinit var gridView: GridView
    lateinit var emptyView: TextView
    private lateinit var adapter: MoviesCursorAdapter

    /**
     * Return a loader id different from any other used within [com.battlelancer.seriesguide.ui.MoviesActivity].
     */
    internal abstract val loaderId: Int

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_movies, container, false)

        gridView = v.findViewById(R.id.gridViewMovies)
        // enable app bar scrolling out of view
        ViewCompat.setNestedScrollingEnabled(gridView, true)
        emptyView = v.findViewById(R.id.textViewMoviesEmpty)
        gridView.emptyView = emptyView

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewModelProvider(requireActivity()).get(MoviesActivityViewModel::class.java)
            .scrollTabToTopLiveData
            .observe(
                viewLifecycleOwner,
                Observer {
                    if (it != null) {
                        if (it.tabPosition == getTabPosition(it.isShowingNowTab)) {
                            gridView.smoothScrollToPosition(0)
                        }
                    }
                }
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().register(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = MoviesCursorAdapter(
            context, MovieClickListener(requireContext()),
            loaderId
        )
        gridView.adapter = adapter

        LoaderManager.getInstance(this).initLoader(loaderId, null, this)

        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        super.onDestroy()

        EventBus.getDefault().unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        // guard against not attached to activity
        if (!isAdded) {
            return
        }

        MoviesOptionsMenu(requireContext()).create(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val menu = MoviesOptionsMenu(requireContext())
        return if (menu.onItemSelected(item, requireActivity())) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: MoviesSortOrderChangedEvent) {
        LoaderManager.getInstance(this).restartLoader(loaderId, null, this)
    }

    /**
     * @return The current position in the tab strip.
     * @see MoviesActivity
     */
    internal abstract fun getTabPosition(showingNowTab: Boolean): Int

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        adapter.swapCursor(data)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        adapter.swapCursor(null)
    }

}
