
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

import java.util.List;

public class Season {

    @SerializedName("tvdb_id")
    public int tvdbId;

    public int season;

    public List<Episode> episodes;

}
