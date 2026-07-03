// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2020 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.movies.database

import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.movies.tools.MovieDetails
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies
import com.battlelancer.seriesguide.tmdbapi.TmdbTools
import com.battlelancer.seriesguide.util.TextTools

/**
 * Data Access Object for the movies table.
 */
@Dao
interface MovieHelper {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovie(movie: SgMovie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMovies(movie: List<SgMovie>)

    @Query("SELECT * FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getMovie(tmdbId: Int): SgMovie?

    @Query("SELECT _id FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getMovieId(tmdbId: Int): Int?

    @Query("SELECT * FROM movies ORDER BY movies_title COLLATE UNICODE ASC")
    fun getMoviesForExport(): List<SgMovie>

    @Query("SELECT COUNT(movies_tmdbid) FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getCount(tmdbId: Int): Int

    @Query(
        """SELECT _id, movies_tmdbid FROM movies WHERE
            (movies_incollection=1 OR movies_inwatchlist=1 OR movies_watched=1)
            AND (
            movies_last_updated IS NULL
            OR (movies_released > :releasedAfter AND movies_last_updated < :updatedBeforeForReleasedAfter)
            OR 
            movies_last_updated < :updatedBeforeAllOthers
            )"""
    )
    fun getMoviesToUpdate(
        releasedAfter: Long,
        updatedBeforeForReleasedAfter: Long,
        updatedBeforeAllOthers: Long
    ): List<SgMovieIds>

    @RawQuery(observedEntities = [SgMovie::class])
    fun getMovies(query: SupportSQLiteQuery): PagingSource<Int, SgMovie>

    @Query(
        """SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched, movies_plays
            FROM movies WHERE movies_incollection=1 OR movies_inwatchlist=1 OR movies_watched=1"""
    )
    fun getMoviesOnListsOrWatched(): List<SgMovieFlags>

    // Note: use "SELECT 1" to just return 1 if there is a matching row as EXISTS only checks if a
    // row is returned, not what row is returned.
    @Query(
        "SELECT movies_tmdbid FROM movies " +
                "WHERE movies_incollection=0 AND movies_inwatchlist=0 AND movies_watched=0 " +
                "AND NOT EXISTS (SELECT 1 FROM listitems WHERE listitems.item_ref_id = movies.movies_tmdbid)"
    )
    fun getTmdbIdsOfMoviesNotOnAnyList(): List<Int>

    @Query("SELECT movies_tmdbid FROM movies WHERE movies_imdbid=:imdbId")
    fun getTmdbIdByImdbId(imdbId: String): Int?

    @Query("SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched, movies_plays FROM movies")
    fun getMovieFlags(): List<SgMovieFlags>

    @Query(
        """SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched, movies_plays
            FROM movies WHERE movies_tmdbid=:tmdbId"""
    )
    fun getMovieFlags(tmdbId: Int): SgMovieFlags?

    @Query("SELECT COUNT(_id) FROM movies")
    fun countMovies(): Int

    @Query("SELECT COUNT(_id) as count, SUM(movies_runtime) as runtime FROM movies WHERE movies_inwatchlist = 1")
    fun getStatsInWatchlist(): MovieStats?

    @Query("SELECT COUNT(_id) as count, SUM(movies_runtime) as runtime FROM movies WHERE movies_incollection = 1")
    fun getStatsInCollection(): MovieStats?

    @Query("SELECT COUNT(_id) as count, SUM(movies_runtime) as runtime FROM movies WHERE movies_watched = 1")
    fun getStatsWatched(): MovieStats?

    @Query("SELECT movies_title FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getMovieTitle(tmdbId: Int): String?

    @Query("SELECT movies_trailer FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getMovieTrailer(tmdbId: Int): String?

    @Query("UPDATE movies SET movies_watched = 0, movies_plays = 0 WHERE movies_tmdbid=:tmdbId")
    fun setNotWatchedAndRemovePlays(tmdbId: Int): Int

    @Query("UPDATE movies SET movies_watched = 1, movies_plays = movies_plays + 1 WHERE movies_tmdbid=:tmdbId")
    fun setWatchedAndAddPlay(tmdbId: Int): Int

    @Update(entity = SgMovie::class)
    fun update(movie: SgMovieTmdbUpdate): Int

    @Update(entity = SgMovie::class)
    fun update(movie: SgMovieTraktUpdate): Int

    @Query("UPDATE movies SET movies_incollection = :inCollection WHERE movies_tmdbid=:tmdbId")
    fun updateInCollection(tmdbId: Int, inCollection: Boolean): Int

    @Query("UPDATE movies SET movies_inwatchlist = :inWatchlist WHERE movies_tmdbid=:tmdbId")
    fun updateInWatchlist(tmdbId: Int, inWatchlist: Boolean): Int

    @Query("UPDATE movies SET movies_rating_user = :userRating WHERE movies_tmdbid=:tmdbId")
    fun updateUserRating(tmdbId: Int, userRating: Int): Int

    @Query("UPDATE movies SET movies_trailer = :trailer WHERE movies_tmdbid=:tmdbId")
    fun updateMovieTrailer(tmdbId: Int, trailer: String)

    @Query("UPDATE movies SET movies_traktid = :traktId, movies_slug = :slug WHERE _id=:rowId")
    fun updateTraktIdAndSlug(rowId: Int, traktId: Int?, slug: String?)

    @Query("DELETE FROM movies WHERE movies_tmdbid=:tmdbId")
    fun deleteMovie(tmdbId: Int): Int

    @Transaction
    fun deleteMovies(tmdbIds: List<Int>) {
        for (tmdbId in tmdbIds) {
            deleteMovie(tmdbId)
        }
    }

    @Query("DELETE FROM movies")
    fun deleteAllMovies()

    /**
     * For testing.
     */
    @Query("SELECT * FROM movies")
    fun getAllMovies(): List<SgMovie>
}

data class SgMovieIds(
    @ColumnInfo(name = Movies._ID)
    val id: Int,
    @ColumnInfo(name = Movies.TMDB_ID)
    val tmdbId: Int
)

data class MovieStats(
    val count: Int,
    val runtime: Long
)

/**
 * Extracts properties from [tmdbMovie] and if successfully loaded from [traktIds] and
 * [traktRatings].
 *
 * Sets [SgMovie.lastUpdated] to the current time.
 */
fun MovieDetails.toSgMovieForInsert(
    tmdbId: Int,
    inCollection: Boolean,
    inWatchlist: Boolean,
    isWatched: Boolean,
    plays: Int
): SgMovie {
    val tmdbUpdate = toSgMovieTmdbUpdate(0)

    val traktId: Int?
    val traktSlug: String?
    val traktIds = traktIds
    if (traktIds is MovieDetails.TraktIds.Success) {
        traktId = traktIds.traktId
        traktSlug = traktIds.traktSlug
    } else {
        traktId = null
        traktSlug = null
    }

    return SgMovie(
        tmdbId = tmdbId,
        inCollection = inCollection,
        inWatchlist = inWatchlist,
        plays = plays,
        watched = isWatched,

        imdbId = tmdbUpdate.imdbId,
        traktId = traktId,
        slug = traktSlug,
        title = tmdbUpdate.title,
        titleNoArticle = tmdbUpdate.titleNoArticle,
        poster = tmdbUpdate.poster,
        genres = tmdbUpdate.genres,
        overview = tmdbUpdate.overview,
        runtimeMin = tmdbUpdate.runtimeMin,
        releasedMs = tmdbUpdate.releasedMs,
        ratingTmdb = tmdbUpdate.ratingTmdb,
        ratingVotesTmdb = tmdbUpdate.ratingVotesTmdb,
        lastUpdated = tmdbUpdate.lastUpdated,
    ).let {
        val traktRatings = toSgMovieTraktUpdate(0)
            ?: return@let it // Keep default values

        it.copy(
            ratingTrakt = traktRatings.ratingTrakt,
            ratingVotesTrakt = traktRatings.ratingVotesTrakt
        )
    }
}

data class SgMovieTmdbUpdate(
    @ColumnInfo(name = Movies._ID)
    val id: Int,

    @ColumnInfo(name = Movies.IMDB_ID)
    val imdbId: String?,
    @ColumnInfo(name = Movies.TITLE)
    val title: String?,
    @ColumnInfo(name = Movies.TITLE_NOARTICLE)
    val titleNoArticle: String?,
    @ColumnInfo(name = Movies.POSTER)
    val poster: String?,
    @ColumnInfo(name = Movies.GENRES)
    val genres: String?,
    @ColumnInfo(name = Movies.OVERVIEW)
    val overview: String?,
    @ColumnInfo(name = Movies.RELEASED_UTC_MS)
    val releasedMs: Long?,
    @ColumnInfo(name = Movies.RUNTIME_MIN)
    val runtimeMin: Int?,
    @ColumnInfo(name = Movies.RATING_TMDB)
    val ratingTmdb: Double?,
    @ColumnInfo(name = Movies.RATING_VOTES_TMDB)
    val ratingVotesTmdb: Int?,

    @ColumnInfo(name = Movies.LAST_UPDATED)
    val lastUpdated: Long?
)

/**
 * Also sets [SgMovie.lastUpdated] to the current time.
 */
fun MovieDetails.toSgMovieTmdbUpdate(movieId: Int): SgMovieTmdbUpdate {
    return SgMovieTmdbUpdate(
        id = movieId,
        imdbId = tmdbMovie.imdb_id,
        title = tmdbMovie.title,
        titleNoArticle = TextTools.trimLeadingArticle(tmdbMovie.title),
        poster = tmdbMovie.poster_path,
        genres = TmdbTools.buildGenresString(tmdbMovie.genres),
        overview = tmdbMovie.overview,
        runtimeMin = tmdbMovie.runtime ?: 0,
        releasedMs = tmdbMovie.release_date?.time ?: SgMovie.RELEASED_MS_UNKNOWN,
        ratingTmdb = tmdbMovie.vote_average ?: 0.0,
        ratingVotesTmdb = tmdbMovie.vote_count ?: 0,
        lastUpdated = System.currentTimeMillis()
    )
}

data class SgMovieTraktUpdate(
    @ColumnInfo(name = Movies._ID)
    val id: Int,

    @ColumnInfo(name = Movies.RATING_TRAKT)
    val ratingTrakt: Double?,
    @ColumnInfo(name = Movies.RATING_VOTES_TRAKT)
    val ratingVotesTrakt: Int?,
)

/**
 * Note: this does not update [SgMovie.lastUpdated] as it is reserved for TMDB data updates.
 */
fun MovieDetails.toSgMovieTraktUpdate(movieId: Int): SgMovieTraktUpdate? {
    val traktRatings = traktRatings ?: return null

    return SgMovieTraktUpdate(
        id = movieId,
        ratingTrakt = traktRatings.rating ?: 0.0,
        ratingVotesTrakt = traktRatings.votes ?: 0
    )
}
