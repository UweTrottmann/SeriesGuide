
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

public class ListItem {

    public String id;

    @SerializedName("tvdb_id")
    public int tvdbId;

    public String type;

}
