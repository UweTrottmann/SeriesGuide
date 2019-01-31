package com.battlelancer.seriesguide.jobs.movies

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.ui.movies.MovieTools

class MovieWatchedJob(
    movieTmdbId: Int,
    private val isWatched: Boolean
) : MovieJob(
    if (isWatched) JobAction.MOVIE_WATCHED_SET else JobAction.MOVIE_WATCHED_REMOVE,
    movieTmdbId
) {

    override fun applyDatabaseUpdate(context: Context, movieTmdbId: Int): Boolean {
        return MovieTools.setWatchedFlag(context, movieTmdbId, isWatched)
    }

    override fun getConfirmationText(context: Context): String {
        return context.getString(
            if (isWatched) R.string.action_watched else R.string.action_unwatched
        )
    }
}
