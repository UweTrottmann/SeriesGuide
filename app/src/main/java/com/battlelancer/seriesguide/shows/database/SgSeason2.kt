// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.shows.overview.SeasonsViewModel

@Entity(
    tableName = "sg_season",
    foreignKeys = [ForeignKey(
        entity = SgShow2::class,
        parentColumns = [SgShow2Columns._ID],
        childColumns = [SgShow2Columns.REF_SHOW_ID]
    )],
    indices = [Index(SgShow2Columns.REF_SHOW_ID)]
)
data class SgSeason2(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = SgSeason2Columns._ID) val id: Long = 0,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = SgSeason2Columns.TMDB_ID) val tmdbId: String?,
    @ColumnInfo(name = SgSeason2Columns.TVDB_ID) val tvdbId: Int? = null,
    @ColumnInfo(name = SgSeason2Columns.COMBINED) val numberOrNull: Int?,
    @ColumnInfo(name = SgSeason2Columns.NAME) val name: String?,
    @ColumnInfo(name = SgSeason2Columns.ORDER) val order: Int,
    /**
     * Deprecated. Stats are now calculated dynamically in [SeasonsViewModel].
     */
    @ColumnInfo(name = SgSeason2Columns.WATCHCOUNT) val notWatchedReleasedOrNull: Int? = 0,
    /**
     * Deprecated. Stats are now calculated dynamically in [SeasonsViewModel].
     */
    @ColumnInfo(name = SgSeason2Columns.UNAIREDCOUNT) val notWatchedToBeReleasedOrNull: Int? = 0,
    /**
     * Deprecated. Stats are now calculated dynamically in [SeasonsViewModel].
     */
    @ColumnInfo(name = SgSeason2Columns.NOAIRDATECOUNT) val notWatchedNoReleaseOrNull: Int? = 0,
    /**
     * Deprecated. Stats are now calculated dynamically in [SeasonsViewModel].
     */
    @ColumnInfo(name = SgSeason2Columns.TOTALCOUNT) val totalOrNull: Int? = 0,
    /**
     * Deprecated. Stats are now calculated dynamically in [SeasonsViewModel].
     */
    @ColumnInfo(name = SgSeason2Columns.TAGS) val tags: String? = ""
) {
    val number: Int
        get() = numberOrNull ?: 0 // == Specials, but should ignore seasons without number.
}
