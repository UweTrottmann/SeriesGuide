// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.history

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ActivityColumns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables

/**
 * Episode watched activity. Uses stable TMDB IDs to work when a show is removed and re-added.
 *
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(
    tableName = Tables.ACTIVITY,
    indices = [Index(value = [ActivityColumns.EPISODE_TVDB_OR_TMDB_ID, "activity_type"], unique = true)]
)
data class SgActivity (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = ActivityColumns._ID) val id: Long?,
    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = ActivityColumns.EPISODE_TVDB_OR_TMDB_ID) val episodeTvdbOrTmdbId: String,
    @ColumnInfo(name = ActivityColumns.SHOW_TVDB_OR_TMDB_ID) val showTvdbOrTmdbId: String,
    @ColumnInfo(name = ActivityColumns.TIMESTAMP_MS) val timestampMs: Long,
    /**
     * One of [ActivityType].
     */
    val activity_type: Int
)

object ActivityType {
    /**
     * Only used for reading, new entries only added if a TMDB ID exists.
     */
    const val TVDB_ID = 1
    const val TMDB_ID = 2
}
