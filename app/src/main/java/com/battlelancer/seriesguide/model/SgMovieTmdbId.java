package com.battlelancer.seriesguide.model;

import android.arch.persistence.room.ColumnInfo;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

public class SgMovieTmdbId {

    @ColumnInfo(name = Movies.TMDB_ID)
    public int tmdbId;

}
