// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.lists.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(
    tableName = Tables.LIST_ITEMS,
    foreignKeys = [
        ForeignKey(
            entity = SgList::class,
            parentColumns = [Lists.LIST_ID], childColumns = [Lists.LIST_ID]
        )
    ],
    indices = [
        Index(value = [ListItems.LIST_ITEM_ID], unique = true),
        Index(Lists.LIST_ID)
    ]
)
data class SgListItem(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ListItems._ID)
    val id: Long? = 0,

    @ColumnInfo(name = ListItems.LIST_ITEM_ID)
    var listItemId: String,

    @ColumnInfo(name = ListItems.ITEM_REF_ID)
    var itemRefId: String,

    @ColumnInfo(name = ListItems.TYPE)
    var type: Int,

    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = Lists.LIST_ID)
    var listId: String?
) {
    constructor(itemRefId: Int, type: Int, listId: String) : this(
        listItemId = ListItems.generateListItemId(itemRefId, type, listId),
        itemRefId = itemRefId.toString(),
        type = type,
        listId = listId
    )
}
