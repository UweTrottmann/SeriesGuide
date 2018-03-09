
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.google.gson.annotations.SerializedName;

public class List {

    @SerializedName("list_id")
    public String listId;
    public String name;
    public int order;

    public java.util.List<ListItem> items;

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(Lists.LIST_ID, listId);
        values.put(Lists.NAME, name);
        values.put(Lists.ORDER, order);
        return values;
    }
}
