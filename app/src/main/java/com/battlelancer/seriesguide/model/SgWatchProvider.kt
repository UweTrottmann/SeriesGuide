package com.battlelancer.seriesguide.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sg_watch_provider",
    indices = [
        Index("provider_id", unique = true),
        Index("provider_name"), // ordered by
        Index("display_priority"), // ordered by
        Index("enabled") // filtered by
    ]
)
data class SgWatchProvider(
    // Just in case a ContentProvider needs to be used, use special _id column name.
    @PrimaryKey(autoGenerate = true) val _id: Int = 0,
    val provider_id: Int,
    val provider_name: String,
    val display_priority: Int,
    val logo_path: String,
    val enabled: Boolean
)
