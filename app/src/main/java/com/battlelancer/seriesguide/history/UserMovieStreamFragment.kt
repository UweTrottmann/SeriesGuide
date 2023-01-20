package com.battlelancer.seriesguide.history

import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.movies.details.MovieDetailsActivity
import com.uwetrottmann.trakt5.entities.HistoryEntry

/**
 * Displays a stream of movies the user has recently watched on Trakt.
 */
class UserMovieStreamFragment : StreamFragment() {

    private var adapter: MovieHistoryAdapter? = null

    override val listAdapter: BaseHistoryAdapter
        get() {
            if (adapter == null) {
                adapter = MovieHistoryAdapter(requireContext(), itemClickListener)
            }
            return adapter!!
        }

    override fun initializeStream() {
        LoaderManager.getInstance(this).initLoader(
            HistoryActivity.MOVIES_LOADER_ID, null,
            activityLoaderCallbacks
        )
    }

    override fun refreshStream() {
        LoaderManager.getInstance(this).restartLoader(
            HistoryActivity.MOVIES_LOADER_ID, null,
            activityLoaderCallbacks
        )
    }

    private val itemClickListener = object : BaseHistoryAdapter.OnItemClickListener {
        override fun onItemClick(view: View, item: HistoryEntry) {
            // display movie details
            val tmdb = item.movie?.ids?.tmdb ?: return
            val i = MovieDetailsActivity.intentMovie(requireContext(), tmdb)
            ActivityCompat.startActivity(
                requireContext(), i, ActivityOptionsCompat
                    .makeScaleUpAnimation(view, 0, 0, view.width, view.height)
                    .toBundle()
            )
        }
    }

    private val activityLoaderCallbacks: LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result> {
            override fun onCreateLoader(
                id: Int,
                args: Bundle?
            ): Loader<TraktEpisodeHistoryLoader.Result> {
                showProgressBar(true)
                return TraktMovieHistoryLoader(requireContext())
            }

            override fun onLoadFinished(
                loader: Loader<TraktEpisodeHistoryLoader.Result>,
                data: TraktEpisodeHistoryLoader.Result
            ) {
                setListData(data.results, data.emptyText)
            }

            override fun onLoaderReset(loader: Loader<TraktEpisodeHistoryLoader.Result>) {
                // keep current data
            }
        }
}