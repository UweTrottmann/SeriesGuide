// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools.getFileNameFromUriOrLastPathSegment
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.Export
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * A [BaseDataLiberationViewModel] that also can run and report progress for an export task and
 * keeps track of import files.
 */
class DataLiberationViewModel(application: Application) : BaseDataLiberationViewModel(application),
    JsonExportTask.OnTaskProgressListener {

    data class ImportFiles(
        val fileNameShows: String?,
        val fileNameLists: String?,
        val fileNameMovies: String?,
        val placeholderText: String
    )

    data class ExportProgressUiState(val total: Int, val completed: Int)

    val exportProgressState = MutableStateFlow(ExportProgressUiState(0, 0))
    val importFiles = MutableStateFlow(ImportFiles("", "", "", ""))

    fun updateImportFileNames() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val showsFileUri = BackupSettings.getImportFileUriOrExportFileUri(
                context,
                Export.Shows
            )
            val listsFileUri = BackupSettings.getImportFileUriOrExportFileUri(
                context,
                Export.Lists
            )
            val moviesFileUri = BackupSettings.getImportFileUriOrExportFileUri(
                context,
                Export.Movies
            )

            importFiles.value = ImportFiles(
                fileNameShows = showsFileUri?.getFileNameFromUriOrLastPathSegment(context),
                fileNameLists = listsFileUri?.getFileNameFromUriOrLastPathSegment(context),
                fileNameMovies = moviesFileUri?.getFileNameFromUriOrLastPathSegment(context),
                placeholderText = context.getString(R.string.no_file_selected)
            )
        }
    }

    fun runExportTask(export: Export, isFullDump: Boolean) {
        setInProgress(true)

        val context: Context = getApplication()
        val exportTask = JsonExportTask(
            context,
            this,
            isFullDump
        )
        viewModelScope.launch(Dispatchers.Default) {
            exportTask.run(export)
        }
    }

    override fun onProgressUpdate(total: Int, completed: Int) {
        exportProgressState.value = ExportProgressUiState(total, completed)
    }

    fun runImportTask(
        importShows: Boolean,
        importLists: Boolean,
        importMovies: Boolean
    ) {
        val context: Context = getApplication()
        runImportTask {
            JsonImportTask(
                context,
                importShows,
                importLists,
                importMovies
            )
        }
    }

}