package com.battlelancer.seriesguide.provider;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Query;
import com.battlelancer.seriesguide.model.SgSeason;
import com.battlelancer.seriesguide.model.SgSeasonMinimal;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Data Access Object for the seasons table.
 */
@Dao
public interface SeasonHelper {

    /**
     * For testing: Get the first season from the table.
     */
    @Query("SELECT * FROM " + Tables.SEASONS + " LIMIT 1")
    SgSeason getSeason();

    @Nullable
    @Query("SELECT combinednr, series_id FROM seasons WHERE _id=:seasonTvdbId")
    SgSeasonMinimal getSeasonMinimal(int seasonTvdbId);

}
