package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.os.Environment
import com.battlelancer.seriesguide.dataliberation.AutoBackupTask.Backup
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BackupType
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Calendar

object AutoBackupTools {

    @Throws(AutoBackupException::class)
    fun getBackupDirectory(context: Context): File {
        val storage = context.getExternalFilesDir(null)
            ?: throw AutoBackupException("Storage not available.")

        if (Environment.getExternalStorageState(storage) != Environment.MEDIA_MOUNTED) {
            throw AutoBackupException("Storage not mounted.")
        }

        return File(storage, "Backups")
    }

    fun deleteOldBackups(context: Context) {
        Timber.i("Deleting old backups.")
        deleteOldBackups(Backup.Shows, context)
        deleteOldBackups(Backup.Lists, context)
        deleteOldBackups(Backup.Movies, context)
    }

    private fun deleteOldBackups(backup: Backup, context: Context) {
        val backups = try {
            getAllBackupsNewestFirst(backup, context)
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
        return getLatestBackupOrNull(JsonExportTask.BACKUP_SHOWS, context) != null
    }

    @JvmStatic
    fun getLatestBackupOrNull(@BackupType type: Int, context: Context): BackupFile? {
        val backup = when (type) {
            Backup.Shows.type -> Backup.Shows
            Backup.Lists.type -> Backup.Lists
            Backup.Movies.type -> Backup.Movies
            else -> throw IllegalArgumentException("Unknown backup type $type")
        }
        try {
            getAllBackupsNewestFirst(backup, context)
                .let { return if (it.isNotEmpty()) it[0] else null }
        } catch (e: IOException) {
            Timber.e(e, "Unable to get latest backup.")
            return null
        }
    }

    data class BackupFile(val file: File, val timestamp: Long)

    @Throws(IOException::class)
    private fun getAllBackupsNewestFirst(backup: Backup, context: Context): List<BackupFile> {
        val backupDirectory = getBackupDirectory(context)
        val files = backupDirectory.listFiles()

        val backups = files?.mapNotNull { file ->
            if (file.isFile && file.name.startsWith(backup.name)) {
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