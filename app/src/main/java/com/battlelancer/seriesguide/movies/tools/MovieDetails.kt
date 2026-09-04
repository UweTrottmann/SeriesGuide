// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.tools

import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.trakt5.entities.Ratings
import java.util.Date

/**
 * Holder object for Trakt and TMDb entities related to a movie.
 */
class MovieDetails(
    val tmdbMovie: Movie
) {

    var releaseDateForRegion: Date? = null

    sealed interface TraktIds {
        data class Success(
            /**
             * May be `null` if movie not found on Trakt.
             */
            val traktId: Int?,
            /**
             * May be `null` if movie not found on Trakt.
             */
            val traktSlug: String?
        ) : TraktIds

        object Error : TraktIds
    }

    var traktIds: TraktIds? = null

    /**
     * This might be null if not loaded or if the API call failed.
     */
    var traktRatings: Ratings? = null

}