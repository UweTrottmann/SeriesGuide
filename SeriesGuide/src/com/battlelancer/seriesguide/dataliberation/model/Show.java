
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
    
    public long airtime;
    
    public String airday;

    @SerializedName("check_in_getglue_id")
    public String checkInGetGlueId;

    @SerializedName("last_watched_episode")
    public int lastWatchedEpisode;
}
