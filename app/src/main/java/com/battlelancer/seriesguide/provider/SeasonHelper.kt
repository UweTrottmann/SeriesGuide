package com.battlelancer.seriesguide.provider

import androidx.room.Dao
import androidx.room.Query
import com.battlelancer.seriesguide.model.SgSeason
import com.battlelancer.seriesguide.model.SgSeasonMinimal
import com.battlelancer.seriesguide.model.SgSeasonUpdateInfo

/**
 * Data Access Object for the seasons table.
 */
@Dao
interface SeasonHelper {
    /**
     * For testing: Get the first season from the table.
     */
    @Query("SELECT * FROM seasons LIMIT 1")
    fun getSeason(): SgSeason?

    @Query("SELECT season_number, series_id FROM seasons WHERE _id=:seasonTvdbId")
    fun getSeasonMinimal(seasonTvdbId: Int): SgSeasonMinimal?

    @Query("SELECT _id, season_tvdb_id FROM seasons WHERE series_id=:showId")
    fun getSeasonTvdbIds(showId: Long): List<SgSeasonUpdateInfo>
}