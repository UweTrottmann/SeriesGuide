package com.battlelancer.seriesguide.dataliberation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.ShowStatusExport
import com.battlelancer.seriesguide.ui.shows.ShowTools.Status
import timber.log.Timber

object DataLiberationTools {

    /**
     * Transform a string representation of [ShowStatusExport]
     * to a [Status] to be stored in the database.
     *
     * Falls back to [Status.UNKNOWN].
     */
    fun encodeShowStatus(status: String?): Int {
        return if (status == null) {
            Status.UNKNOWN
        } else when (status) {
            ShowStatusExport.IN_PRODUCTION -> Status.IN_PRODUCTION
            ShowStatusExport.PILOT -> Status.PILOT
            ShowStatusExport.CANCELED -> Status.CANCELED
            ShowStatusExport.UPCOMING -> Status.PLANNED
            ShowStatusExport.CONTINUING -> Status.RETURNING
            ShowStatusExport.ENDED -> Status.ENDED
            else -> Status.UNKNOWN
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
            Status.IN_PRODUCTION -> ShowStatusExport.IN_PRODUCTION
            Status.PILOT -> ShowStatusExport.PILOT
            Status.CANCELED -> ShowStatusExport.CANCELED
            Status.PLANNED -> ShowStatusExport.UPCOMING
            Status.RETURNING -> ShowStatusExport.CONTINUING
            Status.ENDED -> ShowStatusExport.ENDED
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

    class CreateExportFileContract : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, suggestedFileName: String?): Intent {
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
                .putExtra(Intent.EXTRA_TITLE, suggestedFileName)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if (resultCode != Activity.RESULT_OK) {
                return null
            }
            return intent?.data
        }
    }

    class SelectImportFileContract : ActivityResultContract<Unit, Uri?>() {
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
}