package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import android.text.format.DateUtils
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * View model that checks for available backup files.
 */
class AutoBackupViewModel(application: Application) : AndroidViewModel(application) {

    /** Time string of the available backup, or null if no backup is available. */
    val availableBackupLiveData = MutableLiveData<String?>()

    fun updateAvailableBackupData() = viewModelScope.launch(Dispatchers.IO) {
        val backupShows = AutoBackupTools.getLatestBackupOrNull(
            JsonExportTask.BACKUP_SHOWS, getApplication()
        )
        val backupLists = AutoBackupTools.getLatestBackupOrNull(
            JsonExportTask.BACKUP_LISTS, getApplication()
        )
        val backupMovies = AutoBackupTools.getLatestBackupOrNull(
            JsonExportTask.BACKUP_MOVIES, getApplication()
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

}