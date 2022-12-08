package com.battlelancer.seriesguide.movies

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridView
import android.widget.TextView
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentMoviesWatchlistBinding
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies

/**
 * Displays the users movie watchlist.
 */
class MoviesWatchListFragment : MoviesBaseFragment() {

    override val loaderId: Int = MoviesActivityImpl.WATCHLIST_LOADER_ID

    override val emptyViewTextResId = R.string.movies_watchlist_empty

    override val gridView: GridView
        get() = binding!!.gridViewMoviesWatchlist

    override val emptyView: TextView
        get() = binding!!.textViewMoviesWatchlistEmpty

    private var binding: FragmentMoviesWatchlistBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentMoviesWatchlistBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
    }

    override fun onCreateLoader(loaderId: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            requireContext(), Movies.CONTENT_URI,
            MoviesCursorAdapter.MoviesQuery.PROJECTION, Movies.SELECTION_WATCHLIST, null,
            MoviesDistillationSettings.getSortQuery(context)
        )
    }

    override fun getTabPosition(showingNowTab: Boolean): Int {
        return if (showingNowTab) {
            MoviesActivityImpl.TAB_POSITION_WATCHLIST_WITH_NOW
        } else {
            MoviesActivityImpl.TAB_POSITION_WATCHLIST_DEFAULT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {
        const val liftOnScrollTargetViewId = R.id.gridViewMoviesWatchlist
    }
}
