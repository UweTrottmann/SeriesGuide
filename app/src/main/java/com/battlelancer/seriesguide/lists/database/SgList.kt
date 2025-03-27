// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.lists.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(
    tableName = Tables.LISTS,
    indices = [
        Index(value = [Lists.LIST_ID], unique = true)
    ]
)
data class SgList(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Lists._ID)
    val id: Long? = 0,

    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = Lists.LIST_ID)
    var listId: String,

    @ColumnInfo(name = Lists.NAME)
    var name: String,

    /**
     * Helps determine list order in addition to the list name. Integer.
     * <pre>
     * Range: 0 to MAX INT
     * Default: 0
     * </pre>
     */
    @ColumnInfo(name = Lists.ORDER)
    var order: Int? = 0
) {
    val orderOrDefault: Int
        get() = order ?: 0
}
