package com.battlelancer.seriesguide.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(tableName = Tables.LISTS,
        indices = {@Index(value = Lists.LIST_ID, unique = true)})
public class SgList {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Lists._ID)
    public Integer id;

    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = Lists.LIST_ID)
    @NonNull
    public String listId;

    @ColumnInfo(name = Lists.NAME)
    @NonNull
    public String name;

    /**
     * Helps determine list order in addition to the list name. Integer.
     * <pre>
     * Range: 0 to MAX INT
     * Default: 0
     * </pre>
     */
    @ColumnInfo(name = Lists.ORDER)
    public Integer order = 0;

    public int getOrderOrDefault() {
        return order != null ? order : 0;
    }
}
