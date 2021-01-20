package com.battlelancer.seriesguide.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Activity
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(
    tableName = SeriesGuideDatabase.Tables.ACTIVITY,
    indices = [Index(value = [Activity.EPISODE_TVDB_OR_TMDB_ID], unique = true)]
)
data class SgActivity (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = Activity._ID) val id: Long?,
    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = Activity.EPISODE_TVDB_OR_TMDB_ID) val episodeTvdbOrTmdbId: String,
    @ColumnInfo(name = Activity.SHOW_TVDB_OR_TMDB_ID) val showTvdbOrTmdbId: String,
    @ColumnInfo(name = Activity.TIMESTAMP_MS) val timestampMs: Long
)