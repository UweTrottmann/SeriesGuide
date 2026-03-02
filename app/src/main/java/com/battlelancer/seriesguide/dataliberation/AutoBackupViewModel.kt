// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2020 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools.getFileNameFromUriOrLastPathSegment
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.Export
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * View model that checks for available backup files.
 */
class AutoBackupViewModel(application: Application) : AndroidViewModel(application) {

    data class CopiesFiles(
        val fileNameShows: String?,
        val fileNameLists: String?,
        val fileNameMovies: String?,
        val placeholderText: String,
        val visible: Boolean
    )

    val copiesFiles = MutableStateFlow(CopiesFiles("", "", "", "", visible = false))

    /**
     * Try to keep the import task around on config changes
     * so it does not have to be finished.
     */
    var importTask: Job? = null
    val isImportTaskNotCompleted: Boolean
        get() {
            val importTask = importTask
            return importTask != null && !importTask.isCompleted
        }

    /** Time string of the available backup, or null if no backup is available. */
    val availableBackupLiveData = MutableLiveData<String?>()

    fun updateAvailableBackupData() = viewModelScope.launch(Dispatchers.IO) {
        val backupShows = AutoBackupTools.getLatestBackupOrNull(
            Export.Shows, getApplication()
        )
        val backupLists = AutoBackupTools.getLatestBackupOrNull(
            Export.Lists, getApplication()
        )
        val backupMovies = AutoBackupTools.getLatestBackupOrNull(
            Export.Movies, getApplication()
        )

        // All three files required.
        if (backupShows == null || backupLists == null || backupMovies == null) {
            availableBackupLiveData.postValue(null)
            return@launch
        }

        // All three files must have same time stamp.
        if (backupShows.timestamp != backupLists.timestamp
            || backupShows.timestamp != backupMovies.timestamp) {
            availableBackupLiveData.postValue(null)
            return@launch
        }

        val availableBackupTimeString = DateUtils.getRelativeDateTimeString(
            getApplication(),
            backupShows.timestamp,
            DateUtils.SECOND_IN_MILLIS,
            DateUtils.DAY_IN_MILLIS,
            0
        )

        availableBackupLiveData.postValue(availableBackupTimeString.toString())
    }

    fun updateCopiesFileNames() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()

            val visible = BackupSettings.isCreateCopyOfAutoBackup(context)
            if (!visible) {
                copiesFiles.value = CopiesFiles(null, null, null, "", visible = false)
            } else {
                val showsFileUri = BackupSettings.getExportFileUri(
                    context,
                    Export.Shows,
                    isAutoBackup = true
                )
                val listsFileUri = BackupSettings.getExportFileUri(
                    context,
                    Export.Lists,
                    isAutoBackup = true
                )
                val moviesFileUri = BackupSettings.getExportFileUri(
                    context,
                    Export.Movies,
                    isAutoBackup = true
                )

                copiesFiles.value = CopiesFiles(
                    fileNameShows = showsFileUri?.getFileNameFromUriOrLastPathSegment(context),
                    fileNameLists = listsFileUri?.getFileNameFromUriOrLastPathSegment(context),
                    fileNameMovies = moviesFileUri?.getFileNameFromUriOrLastPathSegment(context),
                    placeholderText = context.getString(R.string.no_file_selected),
                    visible = true
                )
            }
        }
    }

    override fun onCleared() {
        if (isImportTaskNotCompleted) {
            importTask?.cancel(null)
        }
        importTask = null
    }

}