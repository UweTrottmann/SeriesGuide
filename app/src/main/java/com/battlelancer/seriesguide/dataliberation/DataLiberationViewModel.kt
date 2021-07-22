package com.battlelancer.seriesguide.dataliberation

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.Job

/**
 * Try to keep the backup tasks on config changes so they do not have to be finished.
 */
class DataLiberationViewModel(application: Application) : AndroidViewModel(application) {

    var dataLibTask: AsyncTask<Void, Int, Int>? = null
    var dataLibJob: Job? = null

    val isDataLibTaskNotCompleted: Boolean
        get() {
            val dataLibTask = dataLibTask
            val dataLibJob = dataLibJob
            return (dataLibTask != null && dataLibTask.status != AsyncTask.Status.FINISHED
                    || dataLibJob != null && !dataLibJob.isCompleted)
        }

    override fun onCleared() {
        if (isDataLibTaskNotCompleted) {
            dataLibTask?.cancel(true)
            dataLibJob?.cancel(null)
        }
        dataLibTask = null
        dataLibJob = null
    }

}