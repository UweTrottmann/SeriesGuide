// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2020 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.jobs.movies

import android.content.Context
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.movies.tools.MovieTools

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

    override suspend fun applyDatabaseUpdate(context: Context, movieTmdbId: Int): Boolean {
        val movieTools = getServicesComponent(context).movieTools()
        return if (isInCollection) {
            movieTools.addToList(movieTmdbId, MovieTools.Lists.COLLECTION)
        } else {
            movieTools.removeFromList(movieTmdbId, MovieTools.Lists.COLLECTION)
        }
    }
}