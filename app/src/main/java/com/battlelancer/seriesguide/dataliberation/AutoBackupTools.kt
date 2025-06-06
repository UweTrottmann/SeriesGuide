// SPDX-License-Identifier: Apache-2.0
// Copyright 2020-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.os.Environment
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.Export
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ExportType
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Calendar

object AutoBackupTools {

    private const val BACKUP_FOLDER_NAME = "Backups"

    @Throws(AutoBackupException::class)
    fun getBackupDirectory(context: Context): File {
        val storage = context.getExternalFilesDir(null)
            ?: throw AutoBackupException("Storage not available.")

        if (Environment.getExternalStorageState(storage) != Environment.MEDIA_MOUNTED) {
            throw AutoBackupException("Storage not mounted.")
        }

        return File(storage, BACKUP_FOLDER_NAME)
    }

    fun deleteOldBackups(context: Context) {
        Timber.i("Deleting old backups.")
        deleteOldBackups(Export.Shows, context)
        deleteOldBackups(Export.Lists, context)
        deleteOldBackups(Export.Movies, context)
    }

    private fun deleteOldBackups(export: Export, context: Context) {
        val backups = try {
            getAllBackupsNewestFirst(export, context)
        } catch (e: IOException) {
            Timber.e(e, "Unable to delete old backups")
            return
        }

        // Keep last 2 backups.
        backups
            .drop(2)
            .forEach {
                if (!it.file.delete()) {
                    Timber.e("Unable to delete old backup file ${it.file}")
                }
            }
    }

    /**
     * Only checks if an auto backup file for shows is available, likely others are then, too.
     */
    fun isAutoBackupMaybeAvailable(context: Context): Boolean {
        return getLatestBackupOrNull(JsonExportTask.EXPORT_SHOWS, context) != null
    }

    @JvmStatic
    fun getLatestBackupOrNull(@ExportType type: Int, context: Context): BackupFile? {
        val export = when (type) {
            Export.Shows.type -> Export.Shows
            Export.Lists.type -> Export.Lists
            Export.Movies.type -> Export.Movies
            else -> throw IllegalArgumentException("Unknown backup type $type")
        }
        try {
            getAllBackupsNewestFirst(export, context)
                .let { return if (it.isNotEmpty()) it[0] else null }
        } catch (e: IOException) {
            Timber.e(e, "Unable to get latest backup.")
            return null
        }
    }

    data class BackupFile(val file: File, val timestamp: Long)

    @Throws(IOException::class)
    private fun getAllBackupsNewestFirst(export: Export, context: Context): List<BackupFile> {
        val backupDirectory = getBackupDirectory(context)
        val files = backupDirectory.listFiles()

        val backups = files?.mapNotNull { file ->
            if (file.isFile && file.name.startsWith(export.name)) {
                getBackupTimestamp(file)
                    ?.let { return@mapNotNull BackupFile(file, it) }
            }
            return@mapNotNull null
        } ?: emptyList()

        return backups.sortedByDescending { it.timestamp }
    }

    private fun getBackupTimestamp(file: File): Long? {
        val nameAndExtension = file.name.split(".")

        // <something>.json
        if (nameAndExtension.size == 2) {
            val nameParts = nameAndExtension[0].split("-")

            // seriesguide-<type>-yyyy-MM-dd-HH-mm-ss
            if (nameParts.size == 8) {
                val cal = Calendar.getInstance()
                try {
                    cal.set(Calendar.YEAR, nameParts[2].toInt())
                    cal.set(Calendar.MONTH, nameParts[3].toInt() - 1)
                    cal.set(Calendar.DAY_OF_MONTH, nameParts[4].toInt())
                    cal.set(Calendar.HOUR_OF_DAY, nameParts[5].toInt())
                    cal.set(Calendar.MINUTE, nameParts[6].toInt())
                    cal.set(Calendar.SECOND, nameParts[7].toInt())
                    cal.set(Calendar.MILLISECOND, 0)

                    return cal.timeInMillis
                } catch (e: NumberFormatException) {
                    // Return default value end of method.
                }
            }
        }

        return null
    }

}