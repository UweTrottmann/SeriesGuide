package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes

data class SgEpisodeUpdateInfo(
    @ColumnInfo(name = Episodes._ID)
    val id: Long,
    @ColumnInfo(name = Episodes.TVDB_ID)
    val tvdbId: Int,
    @ColumnInfo(name = Episodes.LAST_UPDATED)
    val lastUpdatedSec: Long
)
