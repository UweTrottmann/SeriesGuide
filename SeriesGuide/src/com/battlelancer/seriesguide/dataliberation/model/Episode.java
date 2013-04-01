
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

public class Episode {

    @SerializedName("tvdb_id")
    public int tvdbId;

    public int episode;

    @SerializedName("episode_absolute")
    public int episodeAbsolute;

    public String title;
    
    @SerializedName("first_aired")
    public long firstAired;

    public boolean watched;

    public boolean collected;

}
