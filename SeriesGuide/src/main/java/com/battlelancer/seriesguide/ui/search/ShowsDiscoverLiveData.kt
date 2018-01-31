package com.battlelancer.seriesguide.ui.search

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.os.AsyncTask
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp

class ShowsDiscoverLiveData(val context: Context) : LiveData<ShowsDiscoverLiveData.Result>() {

    data class Result(
            val searchResults: List<SearchResult>,
            val emptyText: String
    )

    private var task: AsyncTask<Void, Void, Result>? = null
    private var language: String = context.getString(R.string.language_code_any)

    /**
     * Schedules loading, give two letter ISO 639-1 [language] code or 'xx' meaning any language.
     * Set [forceLoad] to load new set of results even if language has not changed.
     */
    fun load(language: String, forceLoad: Boolean) {
        if (forceLoad || this.language != language || task == null) {
            this.language = language

            if (task?.status != AsyncTask.Status.FINISHED) {
                task?.cancel(true)
            }
            task = WorkTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class WorkTask : AsyncTask<Void, Void, Result>() {

        override fun doInBackground(vararg params: Void?): Result {
            // no query? load a list of shows with new episodes in the last 7 days
            // TODO ut might have to replace xx with en or something else
            val tmdbShowLoader = TmdbShowLoader(context, SgApp.getServicesComponent(context).tmdb(),
                    language)
            return tmdbShowLoader.getShowsWithNewEpisodes()
        }

        override fun onPostExecute(result: Result) {
            value = result
        }

    }

}