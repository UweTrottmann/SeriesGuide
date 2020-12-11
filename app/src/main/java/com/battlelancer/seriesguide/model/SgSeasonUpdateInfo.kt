package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons

data class SgSeasonUpdateInfo(
    @ColumnInfo(name = Seasons._ID)
    val id: Long,
    @ColumnInfo(name = Seasons.TVDB_ID)
    val tvdbId: Int
)
