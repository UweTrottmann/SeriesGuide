package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract

data class SgEpisodeSeasonAndShow(
    @ColumnInfo(name = SeriesGuideContract.SeasonsColumns.REF_SEASON_ID)
    val seasonTvdbId: Int? = 0,
    @ColumnInfo(name = SeriesGuideContract.Episodes.SEASON)
    val seasonNumber: Int? = 0,
    @ColumnInfo(name = SeriesGuideContract.ShowsColumns.REF_SHOW_ID)
    val showTvdbId: Int? = 0
)