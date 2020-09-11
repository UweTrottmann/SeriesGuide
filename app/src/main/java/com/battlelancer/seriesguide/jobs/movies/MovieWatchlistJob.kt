package com.battlelancer.seriesguide.jobs.movies

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.ui.movies.MovieTools

/**
 * Adds or removes a movie from the watchlist.
 */
class MovieWatchlistJob(
    movieTmdbId: Int,
    private val isInWatchlist: Boolean
) : MovieJob(
    if (isInWatchlist) JobAction.MOVIE_WATCHLIST_ADD else JobAction.MOVIE_WATCHLIST_REMOVE,
    movieTmdbId,
    0 /* Does not change plays. */
) {

    override fun applyDatabaseUpdate(context: Context, movieTmdbId: Int): Boolean {
        val movieTools = getServicesComponent(context).movieTools()
        return if (isInWatchlist) {
            movieTools.addToList(movieTmdbId, MovieTools.Lists.WATCHLIST)
        } else {
            MovieTools.removeFromList(context, movieTmdbId, MovieTools.Lists.WATCHLIST)
        }
    }

    override fun getConfirmationText(context: Context): String {
        return context.getString(
            if (isInWatchlist) {
                R.string.watchlist_add
            } else {
                R.string.watchlist_remove
            }
        )
    }
}