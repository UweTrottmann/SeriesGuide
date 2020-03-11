package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BACKUP_LISTS
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BACKUP_MOVIES
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BACKUP_SHOWS
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.BackupType
import com.battlelancer.seriesguide.settings.AdvancedSettings
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
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
 * If the user specified files do not exist, their URI is purged from prefs.
 */
class AutoBackupTask(
    private val jsonExportTask: JsonExportTask,
    private val context: Context
) {

    class AutoBackupException(message: String) : IOException(message)

    sealed class Backup(val name: String, @BackupType val type: Int) {
        object Shows : Backup("seriesguide-shows", BACKUP_SHOWS)
        object Lists : Backup("seriesguide-lists", BACKUP_LISTS)
        object Movies : Backup("seriesguide-movies", BACKUP_MOVIES)
    }

    @Throws(AutoBackupException::class)
    private fun getBackupDirectory(): File {
        val storage = context.getExternalFilesDir(null)
            ?: throw AutoBackupException("Storage not available.")

        if (Environment.getExternalStorageState(storage) != Environment.MEDIA_MOUNTED) {
            throw AutoBackupException("Storage not mounted.")
        }

        return File(storage, "Backups")
    }

    private fun getBackupFile(backup: Backup, timestamp: String, backupDirectory: File): File {
        val fileName = "${backup.name}-$timestamp.json"
        return File(backupDirectory, fileName)
    }

    @Throws(AutoBackupException::class)
    fun run() {
        Timber.i("Creating auto backup.")

        val backupDirectory = getBackupDirectory()

        if (!backupDirectory.exists()) {
            if (!backupDirectory.mkdirs()) {
                throw AutoBackupException("Unable to create backup directory.")
            }
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(Date())

        // Create backup.
        val backupFileShows = Backup.Shows.let { type ->
            getBackupFile(type, timestamp, backupDirectory)
                .also { backup(type, it) }
        }

        val backupFileLists = Backup.Lists.let { type ->
            getBackupFile(type, timestamp, backupDirectory)
                .also { backup(type, it) }
        }

        val backupFileMovies = Backup.Movies.let { type ->
            getBackupFile(type, timestamp, backupDirectory)
                .also { backup(type, it) }
        }

        // Copy to user files.
        copyBackupToUserFile(Backup.Shows, backupFileShows)
        copyBackupToUserFile(Backup.Lists, backupFileLists)
        copyBackupToUserFile(Backup.Movies, backupFileMovies)

        deleteOldBackups()

        AdvancedSettings.setLastAutoBackupTimeToNow(context)
    }

    @Throws(AutoBackupException::class)
    private fun backup(
        backup: Backup,
        backupFile: File
    ) {
        val dataCursor = jsonExportTask.getDataCursor(backup.type)
            ?: throw AutoBackupException("Querying for data failed.")

        dataCursor.use {
            // If there is no data, create an empty file.
            if (dataCursor.count == 0) {
                backupFile.createNewFile()
                return
            }

            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(backupFile)

                when (backup) {
                    Backup.Shows -> jsonExportTask.writeJsonStreamShows(out, dataCursor)
                    Backup.Lists -> jsonExportTask.writeJsonStreamLists(out, dataCursor)
                    Backup.Movies -> jsonExportTask.writeJsonStreamMovies(out, dataCursor)
                }
            } catch (e: Exception) {
                if (backupFile.delete()) {
                    Timber.e("Backup failed, deleted backup file.")
                } else {
                    Timber.e("Backup failed, failed to delete backup file.")
                }
                throw e
            } finally {
                out?.closeFinally()
            }
        }
    }

    @Throws(AutoBackupException::class)
    private fun copyBackupToUserFile(backup: Backup, sourceFile: File) {
        // Skip if no custom backup file configured.
        val outFileUri: Uri = jsonExportTask.getDataBackupFile(backup.type)
            ?: return

        Timber.i("Copying ${backup.name} backup to user file.")

        // Do not guard against FileNotFoundException, backup file should exist.
        FileInputStream(sourceFile)
            .use { source ->
                try {
                    val outFile = context.contentResolver
                        .openFileDescriptor(outFileUri, "w")
                        ?: throw AutoBackupException("Unable to open user backup file.")

                    outFile.use {
                        FileOutputStream(outFile.fileDescriptor)
                            .use { source.copyTo(it) }
                    }
                } catch (e: FileNotFoundException) {
                    Timber.e("Backup file not found, removing from prefs.")
                    jsonExportTask.removeBackupFileUri(backup.type)
                    throw e
                } catch (e: SecurityException) {
                    Timber.e("Backup file not writable, removing from prefs.")
                    jsonExportTask.removeBackupFileUri(backup.type)
                    throw e
                }
            }
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