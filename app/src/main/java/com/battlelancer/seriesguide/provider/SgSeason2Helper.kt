package com.battlelancer.seriesguide.provider

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.battlelancer.seriesguide.model.SgSeason2
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns

@Dao
interface SgSeason2Helper {

    @Query("SELECT _id FROM sg_season WHERE season_tvdb_id = :seasonTvdbId")
    fun getSeasonId(seasonTvdbId: Int): Long

    /**
     * Get IDs of seasons of a show, sorted by most recent one.
     */
    @Query("SELECT _id FROM sg_season WHERE series_id = :showId ORDER BY season_number DESC")
    fun getSeasonIdsOfShow(showId: Long): List<Long>

    @Update(entity = SgSeason2::class)
    fun updateSeasonCounters(seasonCountUpdate: SgSeason2CountUpdate)

}

data class SgSeason2CountUpdate(
    @ColumnInfo(name = SgSeason2Columns._ID) val id: Long,
    @ColumnInfo(name = SgSeason2Columns.WATCHCOUNT) val notWatchedReleasedCount: Int,
    @ColumnInfo(name = SgSeason2Columns.UNAIREDCOUNT) val notWatchedToBeReleasedCount: Int,
    @ColumnInfo(name = SgSeason2Columns.NOAIRDATECOUNT) val notWatchedNoReleaseCount: Int,
    @ColumnInfo(name = SgSeason2Columns.TOTALCOUNT) val totalCount: Int,
    @ColumnInfo(name = SgSeason2Columns.TAGS) val tags: String
)
