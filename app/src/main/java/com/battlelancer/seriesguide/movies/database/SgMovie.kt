// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2018 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.movies.database.SgMovie.Companion.RELEASED_MS_UNKNOWN
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(
    tableName = Tables.MOVIES,
    indices = [Index(value = [Movies.TMDB_ID], unique = true)]
)
data class SgMovie(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Movies._ID)
    val id: Int? = null,

    @ColumnInfo(name = Movies.TMDB_ID)
    val tmdbId: Int,

    @ColumnInfo(name = Movies.IMDB_ID)
    val imdbId: String? = null,

    /**
     * The Trakt slug for this movie to build URLs. May be null or empty.
     *
     * Added in [SgRoomDatabase.VERSION_55_MOVIE_SLUG_DOUBLE_RATING].
     */
    @ColumnInfo(name = Movies.SLUG)
    val slug: String? = null,

    @ColumnInfo(name = Movies.TITLE)
    val title: String? = null,

    @ColumnInfo(name = Movies.TITLE_NOARTICLE)
    val titleNoArticle: String? = null,

    /**
     * A poster path. Needs to be prefixed with the poster server URL.
     */
    @ColumnInfo(name = Movies.POSTER)
    val poster: String? = null,

    /**
     * A localized, comma-separated list of genres.
     *
     * Example: "Action, Science Fiction"
     */
    @ColumnInfo(name = Movies.GENRES)
    val genres: String? = null,

    @ColumnInfo(name = Movies.OVERVIEW)
    val overview: String? = null,

    /**
     * Get safely via [releasedMsOrDefault].
     */
    @ColumnInfo(name = Movies.RELEASED_UTC_MS)
    val releasedMs: Long? = null,

    /**
     * Running time of this movie in minutes.
     */
    @ColumnInfo(name = Movies.RUNTIME_MIN)
    val runtimeMin: Int? = 0,

    /**
     * A localized (defaults to English) YouTube video ID.
     */
    @ColumnInfo(name = Movies.TRAILER)
    val trailer: String? = null,

    /**
     * Currently unused as certifications vary by region.
     */
    @ColumnInfo(name = Movies.CERTIFICATION)
    val certification: String? = null,

    @ColumnInfo(name = Movies.IN_COLLECTION)
    val inCollection: Boolean? = false,

    @ColumnInfo(name = Movies.IN_WATCHLIST)
    val inWatchlist: Boolean? = false,

    @ColumnInfo(name = Movies.PLAYS)
    val plays: Int? = 0,

    @ColumnInfo(name = Movies.WATCHED)
    val watched: Boolean? = false,

    /**
     * TMDB rating. Encoded as double.
     * ```
     * Range:   0.0-10.0
     * Default: 0.0
     * ```
     */
    @ColumnInfo(name = Movies.RATING_TMDB)
    val ratingTmdb: Double? = 0.0,

    @ColumnInfo(name = Movies.RATING_VOTES_TMDB)
    val ratingVotesTmdb: Int? = 0,

    /**
     * Trakt rating. Encoded as double.
     * ```
     * Range:   0.0-10.0
     * Default: 0.0
     * ```
     */
    @ColumnInfo(name = Movies.RATING_TRAKT)
    val ratingTrakt: Double? = 0.0,

    @ColumnInfo(name = Movies.RATING_VOTES_TRAKT)
    val ratingVotesTrakt: Int? = 0,

    @ColumnInfo(name = Movies.RATING_USER)
    val ratingUser: Int? = null,

    @ColumnInfo(name = Movies.LAST_UPDATED)
    val lastUpdated: Long? = null
) {

    /**
     * Release date in milliseconds. [RELEASED_MS_UNKNOWN] if unknown, assuming the true date is
     * likely in the future (also helps to correctly sort movies by release date).
     */
    val releasedMsOrDefault: Long
        get() = releasedMs ?: RELEASED_MS_UNKNOWN

    val runtimeMinOrDefault: Int
        get() = runtimeMin ?: 0

    val inCollectionOrDefault: Boolean
        get() = inCollection ?: false

    val inWatchlistOrDefault: Boolean
        get() = inWatchlist ?: false

    val playsOrDefault: Int
        get() = plays ?: 0

    val watchedOrDefault: Boolean
        get() = watched ?: false

    val lastUpdatedOrDefault: Long
        get() = lastUpdated ?: 0

    companion object {
        /**
         * If there is no release date, store max value as it is most likely not known, yet. This
         * will then order this release date after known ones, which is likely expected most of the
         * time.
         */
        const val RELEASED_MS_UNKNOWN = Long.MAX_VALUE
    }
}