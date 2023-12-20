// SPDX-License-Identifier: Apache-2.0
// Copyright 2019, 2020, 2022, 2023 Uwe Trottmann

package com.battlelancer.seriesguide.jobs.movies

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.jobs.episodes.JobAction
import com.battlelancer.seriesguide.movies.tools.MovieTools

class MovieWatchedJob(
    movieTmdbId: Int,
    private val isWatched: Boolean,
    currentPlays: Int
) : MovieJob(
    if (isWatched) JobAction.MOVIE_WATCHED_SET else JobAction.MOVIE_WATCHED_REMOVE,
    movieTmdbId,
    if (isWatched) currentPlays + 1 else 0
) {

    override fun applyDatabaseUpdate(context: Context, movieTmdbId: Int): Boolean {
        return if (isWatched) {
            val movieTools = SgApp.getServicesComponent(context).movieTools()
            movieTools.addToList(movieTmdbId, MovieTools.Lists.WATCHED)
        } else {
            MovieTools.removeFromList(context, movieTmdbId, MovieTools.Lists.WATCHED)
        }
    }
}
