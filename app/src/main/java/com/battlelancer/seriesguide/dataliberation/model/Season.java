
package com.battlelancer.seriesguide.dataliberation.model;

import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Season {

    /**
     * May be null for legacy backups.
     */
    @Nullable public String tmdb_id;
    /**
     * Is null on new backups.
     */
    @SerializedName("tvdb_id")
    @Nullable
    public Integer tvdbId;

    public int season;

    public List<Episode> episodes;
}
