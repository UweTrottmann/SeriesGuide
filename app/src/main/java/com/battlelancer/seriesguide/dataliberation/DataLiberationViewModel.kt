// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Try to keep the backup tasks on config changes so they do not have to be finished.
 */
class DataLiberationViewModel(application: Application) : AndroidViewModel(application) {

    data class ImportFiles(
        val fileNameShows: String?,
        val fileNameLists: String?,
        val fileNameMovies: String?,
        val placeholderText: String
    )

    val importFiles = MutableStateFlow(ImportFiles("", "", "", ""))

    var dataLibJob: Job? = null

    val isDataLibTaskNotCompleted: Boolean
        get() {
            val dataLibJob = dataLibJob
            return (dataLibJob != null && !dataLibJob.isCompleted)
        }

    override fun onCleared() {
        if (isDataLibTaskNotCompleted) {
            dataLibJob?.cancel(null)
        }
        dataLibJob = null
    }

    fun updateImportFileNames() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val showsFileUri = BackupSettings.getImportFileUriOrExportFileUri(
                context,
                JsonExportTask.BACKUP_SHOWS
            )
            val listsFileUri = BackupSettings.getImportFileUriOrExportFileUri(
                context,
                JsonExportTask.BACKUP_LISTS
            )
            val moviesFileUri = BackupSettings.getImportFileUriOrExportFileUri(
                context,
                JsonExportTask.BACKUP_MOVIES
            )

            importFiles.value = ImportFiles(
                fileNameShows = showsFileUri?.getFileNameFromUriOrLastPathSegment(),
                fileNameLists = listsFileUri?.getFileNameFromUriOrLastPathSegment(),
                fileNameMovies = moviesFileUri?.getFileNameFromUriOrLastPathSegment(),
                placeholderText = context.getString(R.string.no_file_selected)
            )
        }
    }

    private fun Uri.getFileNameFromUriOrLastPathSegment(): String? {
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
        val fileName = getFileName() ?: lastPathSegment ?: return null
        return "$authority $fileName"
    }

    private fun Uri.getFileName(): String? {
        val cursor = getApplication<Application>().contentResolver
            .query(this, null, null, null, null)
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