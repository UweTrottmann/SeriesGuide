package com.battlelancer.seriesguide.provider;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.battlelancer.seriesguide.model.SgShow;
import com.battlelancer.seriesguide.model.SgShowMinimal;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;
import java.util.List;

/**
 * Data Access Object for the series table.
 */
@Dao
public interface ShowHelper {

    /**
     * For testing: Get the first show from the table.
     */
    @Query("SELECT * FROM " + Tables.SHOWS + " LIMIT 1")
    SgShow getShow();

    /**
     * Returns a {@link SgShowMinimal} object with only title and poster populated.
     * Might return {@code null} if there is no show with that TVDb id.
     */
    @Nullable
    @Query("SELECT seriestitle, series_poster_small FROM series WHERE _id = :tvdbId LIMIT 1")
    SgShowMinimal getShowMinimal(long tvdbId);

    @Nullable
    @Query("SELECT seriestitle FROM series WHERE _id = :tvdbId")
    String getShowTitle(long tvdbId);

    @RawQuery(observedEntities = SgShow.class)
    LiveData<List<SgShow>> queryShows(SupportSQLiteQuery query);

}
