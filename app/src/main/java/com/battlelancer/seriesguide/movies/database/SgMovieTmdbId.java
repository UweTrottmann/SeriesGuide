// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: GPL-3.0-or-later

package com.battlelancer.seriesguide.movies.database;

import androidx.room.ColumnInfo;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

public class SgMovieTmdbId {

    @ColumnInfo(name = Movies.TMDB_ID)
    public int tmdbId;

}
