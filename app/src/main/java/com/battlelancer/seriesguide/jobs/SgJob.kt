// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.jobs;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Jobs;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(tableName = Tables.JOBS,
        indices = {@Index(value = Jobs.CREATED_MS, unique = true)})
public class SgJob {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Jobs._ID)
    public Integer id;

    @ColumnInfo(name = Jobs.CREATED_MS)
    public Long createdMs;

    @ColumnInfo(name = Jobs.TYPE)
    public Integer type;

    @ColumnInfo(name = Jobs.EXTRAS)
    public byte[] extras;
}
