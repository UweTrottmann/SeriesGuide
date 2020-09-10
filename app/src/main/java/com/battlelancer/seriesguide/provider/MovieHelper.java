package com.battlelancer.seriesguide.provider;

import androidx.annotation.Nullable;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.battlelancer.seriesguide.model.SgMovie;
import com.battlelancer.seriesguide.model.SgMovieFlags;
import com.battlelancer.seriesguide.model.SgMovieTmdbId;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import java.util.List;

/**
 * Data Access Object for the movies table.
 */
@Dao
public interface MovieHelper {

    @Nullable
    @Query("SELECT * FROM movies WHERE movies_tmdbid=:tmdbId")
    SgMovie getMovie(int tmdbId);

    @Query("SELECT COUNT(movies_tmdbid) FROM movies WHERE movies_tmdbid=:tmdbId")
    int getCount(int tmdbId);

    @Query("SELECT movies_tmdbid FROM movies WHERE "
            + "(movies_incollection=1 OR movies_inwatchlist=1 OR movies_watched=1)"
            + " AND ("
            + "movies_last_updated IS NULL"
            + " OR "
            + "(movies_released > :releasedAfter AND movies_last_updated < :updatedBeforeForReleasedAfter)"
            + " OR "
            + "movies_last_updated < :updatedBeforeAllOthers"
            + ")")
    List<SgMovieTmdbId> getMoviesToUpdate(long releasedAfter, long updatedBeforeForReleasedAfter,
            long updatedBeforeAllOthers);

    @RawQuery(observedEntities = SgMovie.class)
    DataSource.Factory<Integer, SgMovie> getWatchedMovies(SupportSQLiteQuery query);

    @Query("SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched "
            + "FROM movies WHERE movies_incollection=1 OR movies_inwatchlist=1 OR movies_watched=1")
    List<SgMovieFlags> getMoviesOnListsOrWatched();

    @Query("SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched "
            + "FROM movies")
    List<SgMovieFlags> getMovieFlags();

    @Query("SELECT movies_tmdbid, movies_incollection, movies_inwatchlist, movies_watched "
            + "FROM movies WHERE movies_tmdbid=:tmdbId")
    SgMovieFlags getMovieFlags(int tmdbId);

    @Nullable
    @Query("SELECT movies_title FROM movies WHERE movies_tmdbid=:tmdbId")
    String getMovieTitle(int tmdbId);

    @Query("UPDATE movies SET movies_watched = 0, movies_plays = 0 WHERE movies_tmdbid=:tmdbId")
    int setNotWatchedAndRemovePlays(int tmdbId);

    @Query("UPDATE movies SET movies_watched = 1, movies_plays = movies_plays + 1 WHERE movies_tmdbid=:tmdbId")
    int setWatchedAndAddPlay(int tmdbId);

    @Query("UPDATE movies SET movies_incollection = :inCollection WHERE movies_tmdbid=:tmdbId")
    int updateInCollection(int tmdbId, boolean inCollection);

    @Query("UPDATE movies SET movies_inwatchlist = :inWatchlist WHERE movies_tmdbid=:tmdbId")
    int updateInWatchlist(int tmdbId, boolean inWatchlist);

    @Query("DELETE FROM movies WHERE movies_tmdbid=:tmdbId")
    int deleteMovie(int tmdbId);

    /** For testing. */
    @Query("SELECT * FROM " + Tables.MOVIES)
    List<SgMovie> getAllMovies();
}
