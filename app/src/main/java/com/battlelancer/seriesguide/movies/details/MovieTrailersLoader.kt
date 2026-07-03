// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.details

import android.content.Context
import com.battlelancer.seriesguide.movies.database.MovieHelper
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.androidutils.GenericSimpleLoader

/**
 * Loads the YouTube ID of a trailer from TMDB. Tries to get a local trailer, if not falls back to
 * English. If the movie is added to the database, caches the trailer in the database.
 */
class MovieTrailersLoader(
    private val tmdbId: Int,
    context: Context,
    private val movieDbHelper: MovieHelper
) : GenericSimpleLoader<String?>(context) {

    override fun loadInBackground(): String? {
        val youtubeId = TmdbTools2().getMovieTrailerYoutubeId(context, tmdbId)

        // Cache in database (if movie is added to database) to use during temporary API or network
        // issues or if offline.
        if (youtubeId != null) {
            movieDbHelper.updateMovieTrailer(tmdbId, youtubeId)
            return youtubeId
        } else {
            return movieDbHelper.getMovieTrailer(tmdbId)
        }
    }

}