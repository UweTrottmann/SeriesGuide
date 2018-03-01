package com.battlelancer.seriesguide.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;

@Entity(tableName = SeriesGuideDatabase.Tables.LISTS,
        indices = {@Index(value = Lists.LIST_ID, unique = true)})
public class List {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Lists._ID)
    public int id;

    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = Lists.LIST_ID)
    public String listId;

    @ColumnInfo(name = Lists.NAME)
    public String name;

    /**
     * Helps determine list order in addition to the list name. Integer.
     * <pre>
     * Range: 0 to MAX INT
     * Default: 0
     * </pre>
     */
    @ColumnInfo(name = Lists.ORDER)
    public int order;

}
