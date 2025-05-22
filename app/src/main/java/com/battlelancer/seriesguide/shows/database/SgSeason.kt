// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ShowsColumns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Legacy season entity kept for migration of legacy data. See [SgSeason2].
 */
@Entity(
    tableName = Tables.SEASONS,
    foreignKeys = [
        ForeignKey(
            entity = SgShow::class,
            parentColumns = [Shows._ID], childColumns = [ShowsColumns.REF_SHOW_ID]
        )
    ],
    indices = [
        Index(ShowsColumns.REF_SHOW_ID)
    ]
)
data class SgSeason(
    @PrimaryKey
    @ColumnInfo(name = Seasons._ID)
    var tvdbId: Int? = null,

    @ColumnInfo(name = Seasons.COMBINED)
    var number: Int? = null,

    @ColumnInfo(name = ShowsColumns.REF_SHOW_ID)
    var showTvdbId: String? = null,

    @ColumnInfo(name = Seasons.WATCHCOUNT)
    var watchCount: Int? = 0,

    @ColumnInfo(name = Seasons.UNAIREDCOUNT)
    var notReleasedCount: Int? = 0,

    @ColumnInfo(name = Seasons.NOAIRDATECOUNT)
    var noReleaseDateCount: Int? = 0,

    @ColumnInfo(name = Seasons.TAGS)
    var tags: String? = "",

    @ColumnInfo(name = Seasons.TOTALCOUNT)
    var totalCount: Int? = 0
)
