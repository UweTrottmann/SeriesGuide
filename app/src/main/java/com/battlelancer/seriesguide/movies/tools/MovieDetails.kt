// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.trakt5.entities.Ratings

/**
 * Holder object for Trakt and TMDb entities related to a movie.
 */
class MovieDetails(
    val tmdbMovie: Movie? = null
) {

    var traktRatings: Ratings? = null

}