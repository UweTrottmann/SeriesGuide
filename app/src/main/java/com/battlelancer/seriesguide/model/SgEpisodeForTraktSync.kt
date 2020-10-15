package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes

data class SgEpisodeForTraktSync(
    @ColumnInfo(name = Episodes._ID)
    val tvdbId: Int,
    @ColumnInfo(name = Episodes.NUMBER)
    var number: Int,
    @ColumnInfo(name = Episodes.WATCHED)
    val watched: Int,
    @ColumnInfo(name = Episodes.PLAYS)
    val plays: Int?,
    @ColumnInfo(name = Episodes.COLLECTED)
    val collected: Boolean
)