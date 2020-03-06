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
import java.text.SimpleDateFormat
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
        object Shows : Backup("shows", BACKUP_SHOWS)
        object Lists : Backup("lists", BACKUP_LISTS)
        object Movies : Backup("movies", BACKUP_MOVIES)
    }

    fun run(): Result {
        val storage = context.getExternalFilesDir(null)
            ?: return Result.Error("Storage not available.")

        if (Environment.getExternalStorageState(storage) != Environment.MEDIA_MOUNTED) {
            return Result.Error("Storage not mounted.")
        }

        val backupDirectory = File(storage, "Backups")

        if (!backupDirectory.exists()) {
            if (!backupDirectory.mkdirs()) {
                return Result.Error("Unable to create backup directory.")
            }
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())

        backup(Backup.Shows, backupDirectory, timestamp).let {
            if (it is Result.Error) return it
        }
        backup(Backup.Lists, backupDirectory, timestamp).let {
            if (it is Result.Error) return it
        }
        backup(Backup.Movies, backupDirectory, timestamp).let {
            if (it is Result.Error) return it
        }

        // TODO Copy to user backup files.

        // TODO Clean up old backups.

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

        val fileName = "backup-${backup.name}-$timestamp.json"
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