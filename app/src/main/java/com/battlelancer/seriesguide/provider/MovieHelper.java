package com.battlelancer.seriesguide.provider;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import com.battlelancer.seriesguide.model.SgMovie;
import com.battlelancer.seriesguide.model.SgMovieTmdbId;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import java.util.List;

/**
 * Data Access Object for the movies table.
 */
@Dao
public interface MovieHelper {

    @Query("SELECT movies_tmdbid FROM movies WHERE "
            + "(movies_incollection=1 OR movies_inwatchlist=1)"
            + " AND ("
            + "movies_last_updated IS NULL"
            + " OR "
            + "(movies_released > :releasedAfter AND movies_last_updated < :updatedBeforeForReleasedAfter)"
            + " OR "
            + "movies_last_updated < :updatedBeforeAllOthers"
            + ")")
    List<SgMovieTmdbId> getMoviesToUpdate(long releasedAfter, long updatedBeforeForReleasedAfter,
            long updatedBeforeAllOthers);

    /** For testing. */
    @Query("SELECT * FROM " + Tables.MOVIES)
    List<SgMovie> getAllMovies();
}
