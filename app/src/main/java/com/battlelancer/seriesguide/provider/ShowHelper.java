package com.battlelancer.seriesguide.provider;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import com.battlelancer.seriesguide.model.SgShow;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

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
}
