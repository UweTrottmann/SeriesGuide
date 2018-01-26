package com.battlelancer.seriesguide.ui.search

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.os.AsyncTask

class ShowsDiscoverLiveData : LiveData<ShowsDiscoverLiveData.Result>() {

    data class Result(
            val searchResults: List<SearchResult>,
            val emptyText: String
    )

    private var task: AsyncTask<Void, Void, Result>? = null

    fun load() {
        if (task == null || task?.status == AsyncTask.Status.FINISHED) {
            task = WorkTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class WorkTask : AsyncTask<Void, Void, Result>() {

        override fun doInBackground(vararg params: Void?): Result {
            return Result(listOf(), "I am empty")
        }

        override fun onPostExecute(result: Result?) {
            value = result
        }

    }

}