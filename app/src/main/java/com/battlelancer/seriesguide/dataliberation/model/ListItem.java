
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.gson.annotations.SerializedName;

public class ListItem {

    @SerializedName("list_item_id")
    public String listItemId;

    /**
     * Used in legacy backup files.
     */
    @SerializedName("tvdb_id")
    public int tvdbId;

    /**
     * TMDB ID for new list items, TVDB ID for legacy list items.
     */
    public String externalId;

    public String type;

}
