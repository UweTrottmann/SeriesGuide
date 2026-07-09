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
 * Try to keep the backup tasks on config changes so they do not have to be finished.
 */
class DataLiberationViewModel(application: Application) : BaseDataLiberationViewModel(application) {

    data class ImportFiles(
        val fileNameShows: String?,
        val fileNameLists: String?,
        val fileNameMovies: String?,
        val placeholderText: String
    )

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