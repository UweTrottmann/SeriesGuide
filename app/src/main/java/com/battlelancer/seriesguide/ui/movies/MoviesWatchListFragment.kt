package com.battlelancer.seriesguide.ui.movies

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.ui.MoviesActivity

/**
 * Loads and displays the users trakt movie watchlist.
 */
class MoviesWatchListFragment : MoviesBaseFragment() {

    override val loaderId: Int
        get() = MoviesActivity.WATCHLIST_LOADER_ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        emptyView.setText(R.string.movies_watchlist_empty)

        return v
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
            MoviesActivity.TAB_POSITION_WATCHLIST_WITH_NOW
        } else {
            MoviesActivity.TAB_POSITION_WATCHLIST_DEFAULT
        }
    }
}
