// SPDX-License-Identifier: Apache-2.0
// Copyright 2014-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContract
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.Export
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import com.battlelancer.seriesguide.util.TimeTools
import org.threeten.bp.Instant
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.util.Locale

object DataLiberationTools {

    /**
     * Transform a string representation of [ShowStatusExport]
     * to a [Status] to be stored in the database.
     *
     * Falls back to [ShowStatus.UNKNOWN].
     */
    fun encodeShowStatus(status: String?): Int {
        return if (status == null) {
            ShowStatus.UNKNOWN
        } else when (status) {
            ShowStatusExport.IN_PRODUCTION -> ShowStatus.IN_PRODUCTION
            ShowStatusExport.PILOT -> ShowStatus.PILOT
            ShowStatusExport.CANCELED -> ShowStatus.CANCELED
            ShowStatusExport.UPCOMING -> ShowStatus.PLANNED
            ShowStatusExport.CONTINUING -> ShowStatus.RETURNING
            ShowStatusExport.ENDED -> ShowStatus.ENDED
            else -> ShowStatus.UNKNOWN
        }
    }

    /**
     * Transform an int representation of [Status]
     * to a [ShowStatusExport] to
     * be used for exporting data.
     *
     * @param encodedStatus Detection based on [Status].
     */
    fun decodeShowStatus(encodedStatus: Int): String {
        return when (encodedStatus) {
            ShowStatus.IN_PRODUCTION -> ShowStatusExport.IN_PRODUCTION
            ShowStatus.PILOT -> ShowStatusExport.PILOT
            ShowStatus.CANCELED -> ShowStatusExport.CANCELED
            ShowStatus.PLANNED -> ShowStatusExport.UPCOMING
            ShowStatus.RETURNING -> ShowStatusExport.CONTINUING
            ShowStatus.ENDED -> ShowStatusExport.ENDED
            else -> ShowStatusExport.UNKNOWN
        }
    }

    /**
     * Try to persist read and write permission for given URI across device reboots.
     */
    fun tryToPersistUri(context: Context, uri: Uri) {
        try {
            context.contentResolver
                .takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
        } catch (e: SecurityException) {
            Timber.e(e, "Could not persist r/w permission for backup file URI.")
        }
    }

    /**
     * Input is used as suggested file name.
     */
    class CreateExportFileContract : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                // Filter to only show results that can be "opened", such as
                // a file (as opposed to a list of contacts or timezones).
                .addCategory(Intent.CATEGORY_OPENABLE)
                // do NOT use the probably correct application/json as it would prevent selecting existing
                // backup files on Android, which re-classifies them as application/octet-stream.
                // also do NOT use application/octet-stream as it prevents selecting backup files from
                // providers where the correct application/json mime type is used, *sigh*
                // so, use application/* and let the provider decide
                .setType("application/*")
                .putExtra(Intent.EXTRA_TITLE, input)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if (resultCode != Activity.RESULT_OK) {
                return null
            }
            return intent?.data
        }
    }

    class SelectImportFileContract : ActivityResultContract<Unit?, Uri?>() {
        override fun createIntent(context: Context, input: Unit?): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT)
                // Filter to only show results that can be "opened", such as a
                // file (as opposed to a list of contacts or timezones)
                .addCategory(Intent.CATEGORY_OPENABLE)
                // json files might have mime type of "application/octet-stream"
                // but we are going to store them as "application/json"
                // so filter to show all application files
                .setType("application/*")
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if (resultCode != Activity.RESULT_OK) {
                return null
            }
            return intent?.data
        }

    }

    fun createExportFileTimestamp(): String {
        return Instant.now()
            .atZone(TimeTools.safeSystemDefaultZoneId())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.US))
    }

    fun createExportFileName(
        export: Export,
        timestamp: String = createExportFileTimestamp()
    ): String {
        return "${export.name}-$timestamp.json"
    }

    fun Uri.getFileNameFromUriOrLastPathSegment(context: Context): String? {
        // For the external storage documents provider, return the last path segment, it should
        // contain the file path and be more helpful.
        // content://com.android.externalstorage.documents/document/primary%3ADocuments%2Fseriesguide-shows-backup.json
        val isExternalStorage = authority == "com.android.externalstorage.documents"
        if (isExternalStorage) {
            return "$lastPathSegment"
        }

        // For all other providers return the authority and file name, or if not available last part
        // of the URI.
        val authority = authority ?: return null
        val fileName = getFileName(context) ?: lastPathSegment ?: return null
        return "$authority $fileName"
    }

    private fun Uri.getFileName(context: Context): String? {
        val cursor = try {
            context.contentResolver
                .query(this, null, null, null, null)
        } catch (e: Exception) {
            // Permission to access or the URI itself may have expired
            Timber.w(e, "Failed to get file name")
            null
        }
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    return it.getString(nameIndex)
                }
            }
        }
        return null
    }
}