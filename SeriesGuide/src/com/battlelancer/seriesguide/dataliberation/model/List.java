
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.myjson.annotations.SerializedName;

public class List {

    @SerializedName("list_id")
    public String listId;

    public String name;

    public java.util.List<ListItem> items;

}
