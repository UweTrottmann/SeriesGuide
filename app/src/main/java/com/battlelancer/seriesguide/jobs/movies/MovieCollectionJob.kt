package com.battlelancer.seriesguide.jobs.movies

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.ui.movies.MovieTools

/**
 * Adds or removes a movie from the collection.
 */
class MovieCollectionJob(
    movieTmdbId: Int,
    private val isInCollection: Boolean
) : MovieJob(
    if (isInCollection) JobAction.MOVIE_COLLECTION_ADD else JobAction.MOVIE_COLLECTION_REMOVE,
    movieTmdbId,
    0 /* Does not change plays. */
) {

    override fun applyDatabaseUpdate(context: Context, movieTmdbId: Int): Boolean {
        val movieTools = getServicesComponent(context).movieTools()
        return if (isInCollection) {
            movieTools.addToList(movieTmdbId, MovieTools.Lists.COLLECTION)
        } else {
            MovieTools.removeFromList(context, movieTmdbId, MovieTools.Lists.COLLECTION)
        }
    }

    override fun getConfirmationText(context: Context): String {
        return context.getString(
            if (isInCollection) {
                R.string.action_collection_add
            } else {
                R.string.action_collection_remove
            }
        )
    }
}