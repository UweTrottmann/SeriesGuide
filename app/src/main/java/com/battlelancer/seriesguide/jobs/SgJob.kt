// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.jobs

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Jobs
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(
    tableName = Tables.JOBS,
    indices = [Index(value = [Jobs.CREATED_MS], unique = true)]
)
data class SgJob(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Jobs._ID)
    val id: Int? = null,

    @ColumnInfo(name = Jobs.CREATED_MS)
    val createdMs: Long? = null,

    @ColumnInfo(name = Jobs.TYPE)
    val type: Int? = null,

    @ColumnInfo(name = Jobs.EXTRAS)
    val extras: ByteArray? = null
)
