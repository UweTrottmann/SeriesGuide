package com.battlelancer.seriesguide.model

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
    val enabled: Boolean
) {
    enum class Type(val id: Int) {
        SHOWS(1),
        MOVIES(2)
    }
}
