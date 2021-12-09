package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Job

/**
 * Try to keep the backup tasks on config changes so they do not have to be finished.
 */
class DataLiberationViewModel(application: Application) : AndroidViewModel(application) {

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

}