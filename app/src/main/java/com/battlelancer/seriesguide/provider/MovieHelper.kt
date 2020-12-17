package com.battlelancer.seriesguide.provider

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.model.SgMovie
import com.battlelancer.seriesguide.model.SgMovieFlags
import com.battlelancer.seriesguide.model.SgMovieTmdbId

/**
 * Data Access Object for the movies table.
 */
@Dao
interface MovieHelper {

    @Query("SELECT * FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getMovie(tmdbId: Int): SgMovie?

    @Query("SELECT COUNT(movies_tmdbid) FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getCount(tmdbId: Int): Int

    @Query(
        "SELECT movies_tmdbid FROM movies WHERE "
                + "(movies_incollection=1 OR movies_inwatchlist=1 OR movies_watched=1)"
                + " AND ("
                + "movies_last_updated IS NULL"
                + " OR "
                + "(movies_released > :releasedAfter AND movies_last_updated < :updatedBeforeForReleasedAfter)"
                + " OR "
                + "movies_last_updated < :updatedBeforeAllOthers"
                + ")"
    )
    fun getMoviesToUpdate(
        releasedAfter: Long,
        updatedBeforeForReleasedAfter: Long,
        updatedBeforeAllOthers: Long
    ): List<SgMovieTmdbId>

    @RawQuery(observedEntities = [SgMovie::class])
    fun getWatchedMovies(query: SupportSQLiteQuery): DataSource.Factory<Int, SgMovie>

    @Query(
        "SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched, movies_plays "
                + "FROM movies WHERE movies_incollection=1 OR movies_inwatchlist=1 OR movies_watched=1"
    )
    fun getMoviesOnListsOrWatched(): List<SgMovieFlags>

    @Query(
        "SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched, movies_plays "
                + "FROM movies"
    )
    fun getMovieFlags(): List<SgMovieFlags>

    @Query(
        "SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched, movies_plays "
                + "FROM movies WHERE movies_tmdbid=:tmdbId"
    )
    fun getMovieFlags(tmdbId: Int): SgMovieFlags?

    @Query("SELECT movies_title FROM movies WHERE movies_tmdbid=:tmdbId")
    fun getMovieTitle(tmdbId: Int): String?

    @Query("UPDATE movies SET movies_watched = 0, movies_plays = 0 WHERE movies_tmdbid=:tmdbId")
    fun setNotWatchedAndRemovePlays(tmdbId: Int): Int

    @Query("UPDATE movies SET movies_watched = 1, movies_plays = movies_plays + 1 WHERE movies_tmdbid=:tmdbId")
    fun setWatchedAndAddPlay(tmdbId: Int): Int

    @Query("UPDATE movies SET movies_incollection = :inCollection WHERE movies_tmdbid=:tmdbId")
    fun updateInCollection(tmdbId: Int, inCollection: Boolean): Int

    @Query("UPDATE movies SET movies_inwatchlist = :inWatchlist WHERE movies_tmdbid=:tmdbId")
    fun updateInWatchlist(tmdbId: Int, inWatchlist: Boolean): Int

    @Query("DELETE FROM movies WHERE movies_tmdbid=:tmdbId")
    fun deleteMovie(tmdbId: Int): Int

    /** For testing.  */
    @Query("SELECT * FROM movies")
    fun getAllMovies(): List<SgMovie>
}