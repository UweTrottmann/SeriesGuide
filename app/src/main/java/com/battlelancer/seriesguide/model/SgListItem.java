package com.battlelancer.seriesguide.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(tableName = Tables.LIST_ITEMS,
        foreignKeys = @ForeignKey(entity = SgList.class,
                parentColumns = Lists.LIST_ID, childColumns = Lists.LIST_ID),
        indices = @Index(value = ListItems.LIST_ITEM_ID, unique = true)
)
public class SgListItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ListItems._ID)
    public Integer id;

    @ColumnInfo(name = ListItems.LIST_ITEM_ID)
    @NonNull
    public String listItemId;

    @ColumnInfo(name = ListItems.ITEM_REF_ID)
    @NonNull
    public String itemRefId;

    @ColumnInfo(name = ListItems.TYPE)
    public int type;

    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = Lists.LIST_ID)
    public String listId;
}
