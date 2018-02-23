package com.battlelancer.seriesguide.ui.overview

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.os.AsyncTask
import com.battlelancer.seriesguide.util.DBUtils

class RemainingCountLiveData(val context: Context) : LiveData<RemainingCountLiveData.Result>() {

    data class Result(
            val unwatchedEpisodes: Int,
            val uncollectedEpisodes: Int
    )

    private var task: RemainingUpdateTask? = null
    private var showTvdbId: Int = 0

    fun load(showTvdbId: Int) {
        this.showTvdbId = showTvdbId
        if (showTvdbId > 0 && (task == null || task?.status == AsyncTask.Status.FINISHED)) {
            task = RemainingUpdateTask().executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR) as RemainingUpdateTask
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class RemainingUpdateTask : AsyncTask<Void, Void, Result>() {

        override fun doInBackground(vararg params: Void): Result {
            val showTvdbIdStr = showTvdbId.toString()
            val unwatchedEpisodes = DBUtils.getUnwatchedEpisodesOfShow(context, showTvdbIdStr)
            val uncollectedEpisodes = DBUtils.getUncollectedEpisodesOfShow(context, showTvdbIdStr)
            return Result(unwatchedEpisodes, uncollectedEpisodes)
        }

        override fun onPostExecute(result: Result) {
            value = result
        }
    }

}