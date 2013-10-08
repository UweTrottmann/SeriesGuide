
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

public class ListItem {

    @SerializedName("list_item_id")
    public String listItemId;

    @SerializedName("tvdb_id")
    public int tvdbId;

    public String type;

}
