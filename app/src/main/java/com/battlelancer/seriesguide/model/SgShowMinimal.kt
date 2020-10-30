package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows

data class SgShowMinimal(
    @ColumnInfo(name = Shows.TITLE) val title: String = "",
    @ColumnInfo(name = Shows.POSTER_SMALL) val posterSmall: String = ""
)