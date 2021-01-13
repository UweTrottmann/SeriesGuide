package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns

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
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = SgSeason2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = SgSeason2Columns.TMDB_ID) val tmdbId: String?,
    @ColumnInfo(name = SgSeason2Columns.TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SgSeason2Columns.COMBINED) val number: Int?,
    @ColumnInfo(name = SgSeason2Columns.NAME) val name: String?,
    @ColumnInfo(name = SgSeason2Columns.ORDER) val order: Int,
    @ColumnInfo(name = SgSeason2Columns.WATCHCOUNT) val watchCount: Int? = 0,
    @ColumnInfo(name = SgSeason2Columns.UNAIREDCOUNT) val notReleasedCount: Int? = 0,
    @ColumnInfo(name = SgSeason2Columns.NOAIRDATECOUNT) val noReleaseDateCount: Int? = 0,
    @ColumnInfo(name = SgSeason2Columns.TOTALCOUNT) val totalCount: Int? = 0,
    @ColumnInfo(name = SgSeason2Columns.TAGS) val tags: String? = ""
)
