package com.battlelancer.seriesguide.provider;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import com.battlelancer.seriesguide.model.SgShow;
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

    @RawQuery(observedEntities = SgShow.class)
    LiveData<List<SgShow>> queryShows(SupportSQLiteQuery query);

    @Query("SELECT count(_id) FROM series WHERE series_hidden=1")
    int countHiddenShows();

    @Query("UPDATE series SET series_hidden=0 WHERE series_hidden=1")
    int makeHiddenVisible();

}
