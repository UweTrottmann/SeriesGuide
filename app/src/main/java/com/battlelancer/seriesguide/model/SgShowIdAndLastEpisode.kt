package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows

data class SgShowIdAndLastEpisode(
    @ColumnInfo(name = Shows._ID) val id: Long,
    @ColumnInfo(name = Shows.LASTWATCHEDID) val lastWatchedEpisodeId: Int,
    @ColumnInfo(name = Episodes.SEASON) val seasonNumber: Int,
    @ColumnInfo(name = Episodes.NUMBER) val episodeNumber: Int,
    @ColumnInfo(name = Episodes.FIRSTAIREDMS) val episodeFirstAiredMs: Long,
)
