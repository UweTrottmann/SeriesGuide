// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

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

    override suspend fun applyDatabaseUpdate(context: Context, movieTmdbId: Int): Boolean {
        val movieTools = SgApp.getServicesComponent(context).movieTools()
        return if (isWatched) {
            movieTools.addToList(movieTmdbId, MovieTools.Lists.WATCHED)
        } else {
            movieTools.removeFromList(movieTmdbId, MovieTools.Lists.WATCHED)
        }
    }
}
