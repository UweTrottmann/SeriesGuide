// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.dataliberation.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Season {

    /**
     * Required when importing.
     * <p>
     * It is currently still supported that a {@link #tvdb_id} is set instead if a
     * {@link Show#tvdb_id} is set, like in exports created before the TMDB migration.
     */
    @Nullable public String tmdb_id;
    /**
     * May be set instead of a {@link #tmdb_id}, but only if the show has a {@link Show#tvdb_id}
     * set.
     * <p>
     * May be exported for shows that were added before the TMDB migration.
     */
    @SerializedName("tvdb_id")
    @Nullable
    public Integer tvdbId;

    /**
     * The number of a season. Starting from 0 for Special Episodes.
     */
    public int season;

    public List<Episode> episodes;
}
