// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2020 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.net.Uri
import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment.LiberationResultEvent
import com.battlelancer.seriesguide.util.Errors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.Closeable
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class AutoBackupException(message: String) : IOException(message)

/**
 * A [JsonExportTask] that backs up shows, lists and movies to timestamped files
 * in a [Context.getExternalFilesDir] subdirectory.
 * This directory is included in Android's full auto backup (app data backup).
 *
 * If the last backup attempt failed an error is recorded that can be shown in UI.
 *
 * If the user has specified auto backup files, copies the latest backups to them.
 * If the user specified files do not exist, their URI is purged from prefs.
 */
@Suppress("BlockingMethodInNonBlockingContext")
class AutoBackupTask(
    private val context: Context
) : JsonExportTask(context, null, isFullDump = false) {

    suspend fun runAutoBackup() {
        withContext(Dispatchers.IO) {
            try {
                run(this)
                BackupSettings.setAutoBackupErrorOrNull(context, null)
            } catch (e: Exception) {
                Errors.logAndReport("Unable to auto backup.", e)
                BackupSettings.setAutoBackupErrorOrNull(
                    context,
                    e.javaClass.simpleName + ": " + e.message
                )
            }

            EventBus.getDefault().post(LiberationResultEvent())
        }
    }

    private fun getBackupFile(export: Export, timestamp: String, backupDirectory: File): File {
        return File(backupDirectory, DataLiberationTools.createExportFileName(export, timestamp))
    }

    @Throws(AutoBackupException::class)
    private suspend fun run(coroutineScope: CoroutineScope) {
        Timber.i("Creating auto backup.")

        val backupDirectory = AutoBackupTools.getBackupDirectory(context)

        if (!backupDirectory.exists()) {
            if (!backupDirectory.mkdirs()) {
                throw AutoBackupException("Unable to create backup directory.")
            }
        }

        val timestamp = DataLiberationTools.createExportFileTimestamp()

        // Create backup.
        val backupFileShows = Export.Shows.let { type ->
            getBackupFile(type, timestamp, backupDirectory)
                .also { backup(coroutineScope, type, it) }
        }

        val backupFileLists = Export.Lists.let { type ->
            getBackupFile(type, timestamp, backupDirectory)
                .also { backup(coroutineScope, type, it) }
        }

        val backupFileMovies = Export.Movies.let { type ->
            getBackupFile(type, timestamp, backupDirectory)
                .also { backup(coroutineScope, type, it) }
        }

        if (BackupSettings.isCreateCopyOfAutoBackup(context)) {
            // Copy to user files.
            copyBackupToUserFile(Export.Shows, backupFileShows)
            copyBackupToUserFile(Export.Lists, backupFileLists)
            copyBackupToUserFile(Export.Movies, backupFileMovies)
        }

        AutoBackupTools.deleteOldBackups(context)

        BackupSettings.setLastAutoBackupTimeToNow(context)
    }

    @Throws(AutoBackupException::class)
    private suspend fun backup(
        coroutineScope: CoroutineScope,
        type: Export,
        backupFile: File
    ) {
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(backupFile)

            when (type) {
                Export.Shows -> writeJsonStreamShows(coroutineScope, out)
                Export.Lists -> writeJsonStreamLists(coroutineScope, out)
                Export.Movies -> writeJsonStreamMovies(coroutineScope, out)
            }
        } catch (e: Exception) {
            // This also handles a coroutine CancellationException

            // Try to close the output stream before trying to delete the file
            out?.closeFinally()
            // Delete the potentially broken file to avoid it getting imported
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

    @Throws(AutoBackupException::class)
    private fun copyBackupToUserFile(export: Export, sourceFile: File) {
        // Skip if no custom backup file configured.
        val outFileUri: Uri =
            BackupSettings.getExportFileUri(context, export, isAutoBackup = true)
                ?: return

        Timber.i("Copying ${export.name} backup to user file.")

        // Do not guard against FileNotFoundException, backup file should exist.
        FileInputStream(sourceFile)
            .use { source ->
                try {
                    val outFile = context.contentResolver
                        .openFileDescriptor(outFileUri, "w")
                        ?: throw AutoBackupException("Unable to open user backup file.")

                    outFile.use {
                        FileOutputStream(outFile.fileDescriptor).use {
                            // Even though using streams and FileOutputStream does not append by
                            // default, using Storage Access Framework just overwrites existing
                            // bytes, potentially leaving old bytes hanging over:
                            // so truncate the file first.
                            it.channel.truncate(0)
                            source.copyTo(it)
                        }
                    }
                } catch (e: FileNotFoundException) {
                    Timber.e("Backup file not found, removing from prefs.")
                    removeExportFileUri(export)
                    throw e
                } catch (e: SecurityException) {
                    Timber.e("Backup file not writable, removing from prefs.")
                    removeExportFileUri(export)
                    throw e
                }
            }
    }

    private fun removeExportFileUri(export: Export) {
        BackupSettings.storeExportFileUri(context, export, null, isAutoBackup = true)
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