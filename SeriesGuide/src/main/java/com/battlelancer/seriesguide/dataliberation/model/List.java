
package com.battlelancer.seriesguide.dataliberation.model;

import com.google.gson.annotations.SerializedName;

public class List {

    @SerializedName("list_id")
    public String listId;
    public String name;
    public int order;

    public java.util.List<ListItem> items;

}
