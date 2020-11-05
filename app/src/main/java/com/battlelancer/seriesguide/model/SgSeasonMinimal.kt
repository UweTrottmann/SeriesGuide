package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract

data class SgSeasonMinimal(
    @ColumnInfo(name = SeriesGuideContract.Seasons.COMBINED)
    val number: Int? = null,

    @ColumnInfo(name = SeriesGuideContract.Shows.REF_SHOW_ID)
    val showTvdbId: String? = null
)