
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

import java.util.List;

public class Show {
    @SerializedName("tvdb_id")
    public int tvdbId;

    public String title;

    public List<Season> seasons;

    public boolean favorite;

    public boolean hidden;
}
