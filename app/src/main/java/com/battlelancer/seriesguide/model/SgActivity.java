package com.battlelancer.seriesguide.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Activity;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables;

/**
 * Note: ensure to use CONFLICT_REPLACE when inserting to mimic SQLite UNIQUE x ON CONFLICT REPLACE.
 */
@Entity(tableName = Tables.ACTIVITY,
        indices = {@Index(value = Activity.EPISODE_TVDB_ID, unique = true)})
public class SgActivity {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = Activity._ID)
    public Integer id;

    /**
     * Unique string identifier.
     */
    @ColumnInfo(name = Activity.EPISODE_TVDB_ID)
    @NonNull
    public String episodeTvdbId;

    @ColumnInfo(name = Activity.SHOW_TVDB_ID)
    @NonNull
    public String showTvdbId;

    @ColumnInfo(name = Activity.TIMESTAMP_MS)
    public long timestampMs;
}
