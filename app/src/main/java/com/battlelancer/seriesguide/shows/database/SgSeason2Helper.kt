// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import kotlinx.coroutines.flow.Flow

@Dao
interface SgSeason2Helper {

    @Insert
    fun insertSeason(season: SgSeason2): Long

    @Insert
    fun insertSeasons(seasons: List<SgSeason2>): LongArray

    @Update(entity = SgSeason2::class)
    fun updateSeasons(seasons: List<SgSeason2Update>): Int

    @Update(entity = SgSeason2::class)
    fun updateTmdbIds(seasons: List<SgSeason2TmdbIdUpdate>): Int

    @Query("DELETE FROM sg_season")
    fun deleteAllSeasons()

    @Query("DELETE FROM sg_season WHERE _id = :seasonId")
    fun deleteSeason(seasonId: Long)

    @Transaction
    fun deleteSeasons(seasonIds: List<Long>) {
        seasonIds.forEach {
            deleteSeason(it)
        }
    }

    @Query("DELETE FROM sg_season WHERE series_id = :showId AND season_tmdb_id IS NULL")
    fun deleteSeasonsWithoutTmdbId(showId: Long)

    @Query("SELECT * FROM sg_season WHERE _id = :seasonId")
    fun getSeason(seasonId: Long): SgSeason2?

    /**
     * Get IDs of seasons of a show, sorted by most recent one.
     */
    @Query("SELECT _id FROM sg_season WHERE series_id = :showId ORDER BY season_number DESC")
    fun getSeasonIdsOfShow(showId: Long): List<Long>

    @Query("SELECT _id, series_id, season_tmdb_id, season_tvdb_id, season_number FROM sg_season WHERE _id = :seasonId")
    fun getSeasonNumbers(seasonId: Long): SgSeason2Numbers?

    @Query("SELECT _id, series_id, season_tmdb_id, season_tvdb_id, season_number FROM sg_season WHERE season_tvdb_id = :seasonTvdbId")
    fun getSeasonNumbersByTvdbId(seasonTvdbId: Int): SgSeason2Numbers?

    @Query("SELECT _id, series_id, season_tmdb_id, season_tvdb_id, season_number FROM sg_season WHERE series_id = :showId ORDER BY season_number")
    fun getSeasonNumbersOfShow(showId: Long): List<SgSeason2Numbers>

    /**
     * Excludes seasons where total episode count is 0.
     */
    @Query("SELECT * FROM sg_season WHERE series_id = :showId ORDER BY season_number DESC")
    fun getSeasonsOfShowLatestFirst(showId: Long): Flow<List<SgSeason2>>

    /**
     * Excludes seasons where total episode count is 0.
     */
    @Query("SELECT * FROM sg_season WHERE series_id = :showId ORDER BY season_number ASC")
    fun getSeasonsOfShowOldestFirst(showId: Long): Flow<List<SgSeason2>>

    @Query("SELECT * FROM sg_season WHERE series_id = :showId ORDER BY season_number ASC")
    fun getSeasonsForExport(showId: Long): List<SgSeason2>

    @Query("DELETE FROM sg_season WHERE series_id = :showId")
    suspend fun deleteSeasonsOfShow(showId: Long): Int
}

data class SgSeason2Numbers(
    @ColumnInfo(name = SgSeason2Columns._ID) val id: Long,
    @ColumnInfo(name = SeriesGuideContract.SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = SgSeason2Columns.TMDB_ID) val tmdbId: String?,
    @ColumnInfo(name = SgSeason2Columns.TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SgSeason2Columns.COMBINED) val numberOrNull: Int?
) {
    val number: Int
        get() = numberOrNull ?: 0 // == Specials, but should ignore seasons without number.
}

data class SgSeason2Update(
    @ColumnInfo(name = SgSeason2Columns._ID) val id: Long,
    @ColumnInfo(name = SgSeason2Columns.COMBINED) val number: Int,
    @ColumnInfo(name = SgSeason2Columns.ORDER) val order: Int,
    @ColumnInfo(name = SgSeason2Columns.NAME) val name: String?
)

data class SgSeason2TmdbIdUpdate(
    @ColumnInfo(name = SgSeason2Columns._ID) val id: Long,
    @ColumnInfo(name = SgSeason2Columns.TMDB_ID) val tmdbId: String,
)
