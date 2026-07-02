// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.details

import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.trakt5.entities.Ratings

/**
 * Holder object for Trakt and TMDb entities related to a movie.
 */
class MovieDetails {

    private var traktRatings: Ratings? = null
    private var tmdbMovie: Movie? = null

    var isInCollection: Boolean = false
    var isInWatchlist: Boolean = false
    var isWatched: Boolean = false
    var plays: Int = 0

    var userRating: Int = 0

    var lastUpdatedMillis: Long = 0

    fun traktRatings(): Ratings? {
        return traktRatings
    }

    fun traktRatings(traktRatings: Ratings?) {
        this.traktRatings = traktRatings
    }

    fun tmdbMovie(): Movie? {
        return tmdbMovie
    }

    fun tmdbMovie(movie: Movie?) {
        tmdbMovie = movie
    }

}
