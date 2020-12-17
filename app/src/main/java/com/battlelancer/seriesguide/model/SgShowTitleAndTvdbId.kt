package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows

data class SgShowTitleAndTvdbId(
    @ColumnInfo(name = Shows.TITLE) val title: String? = "",
    @ColumnInfo(name = Shows.TVDB_ID) val tvdbId: Int
)