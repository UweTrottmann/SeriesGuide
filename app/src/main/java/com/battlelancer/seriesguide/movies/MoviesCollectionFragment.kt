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
import com.battlelancer.seriesguide.databinding.FragmentMoviesCollectionBinding
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies

/**
 * Displays the users collection of movies.
 */
class MoviesCollectionFragment : MoviesBaseFragment() {

    override val loaderId: Int = MoviesActivityImpl.COLLECTION_LOADER_ID

    override val emptyViewTextResId = R.string.movies_collection_empty

    override val gridView: GridView
        get() = binding!!.gridViewMoviesCollection

    override val emptyView: TextView
        get() = binding!!.textViewMoviesCollectionEmpty

    private var binding: FragmentMoviesCollectionBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentMoviesCollectionBinding.inflate(inflater, container, false)
            .also { binding = it }
            .root
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
            MoviesActivityImpl.TAB_POSITION_COLLECTION_WITH_NOW
        } else {
            MoviesActivityImpl.TAB_POSITION_COLLECTION_DEFAULT
        }
    }
}
