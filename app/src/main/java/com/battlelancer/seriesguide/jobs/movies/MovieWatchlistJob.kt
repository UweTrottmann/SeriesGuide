// SPDX-License-Identifier: Apache-2.0
// Copyright 2020, 2022, 2023 Uwe Trottmann

package com.battlelancer.seriesguide.jobs.movies

import android.content.Context
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.movies.tools.MovieTools

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
}