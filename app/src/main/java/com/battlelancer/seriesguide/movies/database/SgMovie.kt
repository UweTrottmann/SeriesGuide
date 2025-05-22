// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.movies.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
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

    @ColumnInfo(name = Movies.TITLE)
    val title: String? = null,

    @ColumnInfo(name = Movies.TITLE_NOARTICLE)
    val titleNoArticle: String? = null,

    @ColumnInfo(name = Movies.POSTER)
    val poster: String? = null,

    @ColumnInfo(name = Movies.GENRES)
    val genres: String? = null,

    @ColumnInfo(name = Movies.OVERVIEW)
    val overview: String? = null,

    @ColumnInfo(name = Movies.RELEASED_UTC_MS)
    val releasedMs: Long? = null,

    @ColumnInfo(name = Movies.RUNTIME_MIN)
    val runtimeMin: Int? = 0,

    @ColumnInfo(name = Movies.TRAILER)
    val trailer: String? = null,

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

    @ColumnInfo(name = Movies.RATING_TMDB)
    val ratingTmdb: Double? = 0.0,

    @ColumnInfo(name = Movies.RATING_VOTES_TMDB)
    val ratingVotesTmdb: Int? = 0,

    @ColumnInfo(name = Movies.RATING_TRAKT)
    val ratingTrakt: Int? = 0,

    @ColumnInfo(name = Movies.RATING_VOTES_TRAKT)
    val ratingVotesTrakt: Int? = 0,

    @ColumnInfo(name = Movies.RATING_USER)
    val ratingUser: Int? = null,

    @ColumnInfo(name = Movies.LAST_UPDATED)
    val lastUpdated: Long? = null
) {

    /**
     * If [releasedMs] is not null returns it, otherwise [Long.MAX_VALUE].
     * See [Movies.RELEASED_UTC_MS].
     */
    val releasedMsOrDefault: Long
        get() = releasedMs ?: Long.MAX_VALUE

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
}