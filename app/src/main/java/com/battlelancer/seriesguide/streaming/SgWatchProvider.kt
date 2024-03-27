// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.streaming

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sg_watch_provider",
    indices = [
        Index(value = ["provider_id", "type"], unique = true),
        Index("provider_name"), // ordered by
        Index("display_priority"), // ordered by
        Index("enabled"), // filtered by
        Index("type"), // filtered by
    ]
)
data class SgWatchProvider(
    // Just in case a ContentProvider needs to be used, use special _id column name.
    @PrimaryKey(autoGenerate = true) val _id: Int = 0,
    val provider_id: Int,
    val provider_name: String,
    val display_priority: Int,
    val logo_path: String,
    /**
     * Type 1 is shows, type 2 is movies. This splits the providers by type which may lead
     * to duplicates, but allows a different enabled set for shows than for movies.
     */
    val type: Int,
    /**
     * If discover results should be filtered by this provider.
     */
    val enabled: Boolean,
    /**
     * If local shows or movies should be filtered by this provider.
     *
     * Added with [com.battlelancer.seriesguide.provider.SgRoomDatabase.VERSION_52_WATCH_PROVIDER_FILTERS].
     */
    @ColumnInfo(defaultValue = "false")
    val filter_local: Boolean = false
) {
    enum class Type(val id: Int) {
        SHOWS(1),
        MOVIES(2)
    }
}
