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
 * Displays a users collection of movies in a grid.
 */
class MoviesCollectionFragment : MoviesBaseFragment() {

    override val loaderId: Int
        get() = MoviesActivity.COLLECTION_LOADER_ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        emptyView.setText(R.string.movies_collection_empty)

        return v
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        return CursorLoader(
            requireContext(), Movies.CONTENT_URI,
            MoviesCursorAdapter.MoviesQuery.PROJECTION, Movies.SELECTION_COLLECTION, null,
            MoviesDistillationSettings.getSortQuery(context)
        )
    }

    override fun getTabPosition(showingNowTab: Boolean): Int {
        return if (showingNowTab) {
            MoviesActivity.TAB_POSITION_COLLECTION_WITH_NOW
        } else {
            MoviesActivity.TAB_POSITION_COLLECTION_DEFAULT
        }
    }
}
