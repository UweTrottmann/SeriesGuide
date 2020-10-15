package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies

data class SgMovieFlags(
    @ColumnInfo(name = Movies.TMDB_ID) val tmdbId: Int = 0,
    @ColumnInfo(name = Movies.IN_COLLECTION) val inCollection: Boolean = false,
    @ColumnInfo(name = Movies.IN_WATCHLIST) val inWatchlist: Boolean = false,
    @ColumnInfo(name = Movies.WATCHED) val watched: Boolean = false,
    @ColumnInfo(name = Movies.PLAYS) val plays: Int = 0
)