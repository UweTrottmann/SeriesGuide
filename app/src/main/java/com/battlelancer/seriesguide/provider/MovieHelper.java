package com.battlelancer.seriesguide.provider;

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
            + "FROM movies WHERE movies_tmdbid=:tmdbId")
    SgMovieFlags getMovieFlags(int tmdbId);

    @Query("DELETE FROM movies WHERE movies_tmdbid=:tmdbId")
    int deleteMovie(int tmdbId);

    /** For testing. */
    @Query("SELECT * FROM " + Tables.MOVIES)
    List<SgMovie> getAllMovies();
}
