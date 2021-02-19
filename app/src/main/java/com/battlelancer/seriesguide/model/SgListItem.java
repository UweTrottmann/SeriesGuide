package com.battlelancer.seriesguide.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(tableName = Tables.LIST_ITEMS,
        foreignKeys = @ForeignKey(entity = SgList.class,
                parentColumns = Lists.LIST_ID, childColumns = Lists.LIST_ID),
        indices = {
                @Index(value = ListItems.LIST_ITEM_ID, unique = true),
                @Index(Lists.LIST_ID)
        }
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

    public SgListItem() {
    }

    public SgListItem(int itemRefId, int type, @NonNull String listId) {
        this.listItemId = ListItems.generateListItemId(itemRefId, type, listId);
        this.itemRefId = String.valueOf(itemRefId);
        this.type = type;
        this.listId = listId;
    }
}
