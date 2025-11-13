// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2025 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.diagnostics

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DbDebugLogEntry::class],
    version = 1
)
abstract class DebugLogDatabase : RoomDatabase() {
    abstract fun debugLogHelper(): DebugLogHelper

    companion object {

        private const val DATABASE_NAME = "seriesguide_debug_log"

        fun build(context: Context): DebugLogDatabase {
            return Room
                .databaseBuilder(context, DebugLogDatabase::class.java, DATABASE_NAME)
                .build()
        }
    }
}

@Entity(tableName = "debug_log")
data class DbDebugLogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "priority") val priority: Int?,
    @ColumnInfo(name = "tag") val tag: String?,
    @ColumnInfo(name = "message") val message: String?,
    /**
     * The time in milliseconds this log entry was created at.
     */
    @ColumnInfo(name = "created_at") val createdAt: Long?
)

@Dao
interface DebugLogHelper {

    @Insert
    suspend fun insert(entry: DbDebugLogEntry)

    @Query("SELECT * FROM debug_log")
    suspend fun getAll(): List<DbDebugLogEntry>

    @Query("DELETE FROM debug_log WHERE created_at < :timeInMs")
    suspend fun deleteOlderThan(timeInMs: Long)

}
