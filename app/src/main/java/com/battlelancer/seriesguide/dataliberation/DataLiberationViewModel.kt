// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools.getFileNameFromUriOrLastPathSegment
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
                fileNameShows = showsFileUri?.getFileNameFromUriOrLastPathSegment(context),
                fileNameLists = listsFileUri?.getFileNameFromUriOrLastPathSegment(context),
                fileNameMovies = moviesFileUri?.getFileNameFromUriOrLastPathSegment(context),
                placeholderText = context.getString(R.string.no_file_selected)
            )
        }
    }

}