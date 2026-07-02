// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.details

import android.content.ContentValues
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.util.DBUtils
import com.battlelancer.seriesguide.util.TextTools
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

    /**
     * Extracts ratings from trakt, all other properties from TMDb data.
     *
     * If either movie data is null, will still extract the properties of others.
     *
     * Does not add TMDB id or collection and watchlist flag.
     */
    fun toContentValuesUpdate(): ContentValues {
        val values = ContentValues()

        // data from trakt
        val traktRatings = traktRatings
        if (traktRatings != null) {
            values.put(Movies.RATING_TRAKT, traktRatings.rating ?: 0.0)
            values.put(Movies.RATING_VOTES_TRAKT, traktRatings.votes ?: 0)
        }

        // data from TMDb
        val tmdbMovie = tmdbMovie
        if (tmdbMovie != null) {
            values.put(Movies.IMDB_ID, tmdbMovie.imdb_id)
            values.put(Movies.TITLE, tmdbMovie.title)
            values.put(Movies.TITLE_NOARTICLE, TextTools.trimLeadingArticle(tmdbMovie.title))
            values.put(Movies.OVERVIEW, tmdbMovie.overview)
            values.put(Movies.POSTER, tmdbMovie.poster_path)
            values.put(Movies.RUNTIME_MIN, tmdbMovie.runtime ?: 0)
            values.put(Movies.RATING_TMDB, tmdbMovie.vote_average ?: 0.0)
            values.put(Movies.RATING_VOTES_TMDB, tmdbMovie.vote_count ?: 0)
            // if there is no release date, store Long.MAX as it is likely in the future
            // also helps correctly sorting movies by release date
            val releaseDate = tmdbMovie.release_date
            values.put(Movies.RELEASED_UTC_MS,
                releaseDate?.getTime() ?: SgMovie.RELEASED_MS_UNKNOWN)
        }

        return values
    }

    /**
     * Like [toContentValuesUpdate], but adds TMDB id and adds values for collection,
     * watchlist and watched status and plays.
     */
    fun toContentValuesInsert(): ContentValues {
        val values = toContentValuesUpdate()
        values.put(Movies.TMDB_ID, tmdbMovie!!.id)
        values.put(Movies.IN_COLLECTION, DBUtils.convertBooleanToInt(isInCollection))
        values.put(Movies.IN_WATCHLIST, DBUtils.convertBooleanToInt(isInWatchlist))
        values.put(Movies.WATCHED, DBUtils.convertBooleanToInt(isWatched))
        values.put(Movies.PLAYS, plays)
        // set default values
        values.put(Movies.RATING_TMDB, 0)
        values.put(Movies.RATING_VOTES_TMDB, 0)
        values.put(Movies.RATING_TRAKT, 0)
        values.put(Movies.RATING_VOTES_TRAKT, 0)
        values.put(Movies.LAST_UPDATED, System.currentTimeMillis())
        return values
    }
}
