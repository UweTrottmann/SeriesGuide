package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.os.Environment
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BACKUP_LISTS
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BACKUP_MOVIES
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BACKUP_SHOWS
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BackupType
import com.battlelancer.seriesguide.settings.AdvancedSettings
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Backs up shows, lists and movies to timestamped files
 * in a [Context.getExternalFilesDir] subdirectory.
 * This directory is included in Google's full auto backup.
 *
 * Failures are silent.
 *
 * If the user has specified auto backup files, copies the latest backups to them.
 * If the user specified files are not writable, will trigger a user notification.
 */
class AutoBackupTask(
    private val jsonExportTask: JsonExportTask,
    private val context: Context
) {

    sealed class Result {
        object Success : Result()
        data class Error(val reason: String) : Result()
    }

    sealed class Backup(val name: String, @BackupType val type: Int) {
        object Shows : Backup("seriesguide-shows", BACKUP_SHOWS)
        object Lists : Backup("seriesguide-lists", BACKUP_LISTS)
        object Movies : Backup("seriesguide-movies", BACKUP_MOVIES)
    }

    @Throws(IOException::class)
    private fun getBackupDirectory(): File {
        val storage = context.getExternalFilesDir(null)
            ?: throw IOException("Storage not available.")

        if (Environment.getExternalStorageState(storage) != Environment.MEDIA_MOUNTED) {
            throw IOException("Storage not mounted.")
        }

        return File(storage, "Backups")
    }

    fun run(): Result {
        Timber.i("Creating auto backup.")

        val backupDirectory = try {
            getBackupDirectory()
        } catch (e: IOException) {
            return Result.Error(e.message!!)
        }

        if (!backupDirectory.exists()) {
            if (!backupDirectory.mkdirs()) {
                return Result.Error("Unable to create backup directory.")
            }
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())

        backup(Backup.Shows, backupDirectory, timestamp)
            .let { if (it is Result.Error) return it }

        backup(Backup.Lists, backupDirectory, timestamp)
            .let { if (it is Result.Error) return it }

        backup(Backup.Movies, backupDirectory, timestamp)
            .let { if (it is Result.Error) return it }

        // TODO Copy to user backup files.

        deleteOldBackups()

        AdvancedSettings.setLastAutoBackupTimeToNow(context)

        return Result.Success
    }

    private fun backup(
        backup: Backup,
        backupDirectory: File,
        timestamp: String
    ): Result {
        val dataCursor = jsonExportTask.getDataCursor(backup.type)
            ?: return Result.Error("Querying for data failed.")

        // If there is no data, do nothing.
        if (dataCursor.count == 0) {
            dataCursor.close()
            return Result.Success
        }

        val fileName = "${backup.name}-$timestamp.json"
        val backupFile = File(backupDirectory, fileName)
        val out = FileOutputStream(backupFile)

        try {
            when (backup) {
                Backup.Shows -> jsonExportTask.writeJsonStreamShows(out, dataCursor)
                Backup.Lists -> jsonExportTask.writeJsonStreamLists(out, dataCursor)
                Backup.Movies -> jsonExportTask.writeJsonStreamMovies(out, dataCursor)
            }
        } catch (e: Exception) {
            if (backupFile.delete()) {
                Timber.e(e, "Backup failed, deleted backup file.")
            } else {
                Timber.e(e, "Backup failed, failed to delete backup file.")
            }
            return Result.Error(e.message ?: "Backup failed for unknown reason.")
        } finally {
            out.closeFinally()
            dataCursor.closeFinally()
        }

        return Result.Success
    }

    private fun deleteOldBackups() {
        Timber.i("Deleting old backups.")
        deleteOldBackups(Backup.Shows)
        deleteOldBackups(Backup.Lists)
        deleteOldBackups(Backup.Movies)
    }

    private fun deleteOldBackups(backup: Backup) {
        val backups = try {
            getAllBackupsNewestFirst(backup)
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

    data class BackupFile(val file: File, val timestamp: Long)

    private fun getAllBackupsNewestFirst(backup: Backup): List<BackupFile> {
        val backupDirectory = getBackupDirectory()
        val files = backupDirectory.listFiles()

        val backups = files.mapNotNull { file ->
            if (file.isFile && file.name.startsWith(backup.name)) {
                getBackupTimestamp(file)
                    ?.let { return@mapNotNull BackupFile(file, it) }
            }
            return@mapNotNull null
        }

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
                    cal.set(Calendar.MONTH, nameParts[3].toInt())
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

    private fun Closeable.closeFinally() {
        try {
            close()
        } catch (runtimeException: RuntimeException) {
            throw runtimeException
        } catch (checkedException: Exception) {
            // Ignored.
        }
    }

}