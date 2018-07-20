package com.battlelancer.seriesguide.ui.episodes

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.os.AsyncTask
import com.battlelancer.seriesguide.util.DBUtils

class EpisodeCountLiveData(val context: Context) : LiveData<EpisodeCountLiveData.Result>() {

    data class Result(
            val unwatchedEpisodes: Int,
            val uncollectedEpisodes: Int
    )

    private var task: CountTask? = null
    private var seasonTvdbId: Int = 0

    fun load(seasonTvdbId: Int) {
        this.seasonTvdbId = seasonTvdbId
        if (seasonTvdbId > 0 && (task == null || task?.status == AsyncTask.Status.FINISHED)) {
            task = CountTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR) as CountTask
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class CountTask : AsyncTask<Void, Void, Result>() {

        override fun doInBackground(vararg params: Void): Result {
            val unwatchedEpisodes = DBUtils.getUnwatchedEpisodesOfSeason(context, seasonTvdbId)
            val uncollectedEpisodes = DBUtils.getUncollectedEpisodesOfSeason(context, seasonTvdbId)
            return Result(unwatchedEpisodes, uncollectedEpisodes)
        }

        override fun onPostExecute(result: Result) {
            value = result
        }
    }

}