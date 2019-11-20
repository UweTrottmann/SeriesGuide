package com.battlelancer.seriesguide.ui.search

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.thetvdbapi.TvdbException
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.TmdbDate
import timber.log.Timber
import java.util.Calendar
import java.util.Date

class ShowsDiscoverLiveData(val context: Context) : LiveData<ShowsDiscoverLiveData.Result>() {

    data class Result(
            val searchResults: List<SearchResult>,
            val emptyText: String,
            val isResultsForQuery: Boolean,
            val successful: Boolean
    )

    private var task: AsyncTask<Void, Void, Result?>? = null
    private var query: String = ""
    private var language: String = context.getString(R.string.language_code_any)
    private val languageCodeAny: String by lazy { context.getString(R.string.language_code_any) }

    /**
     * Schedules loading, give two letter ISO 639-1 [language] code or 'xx' meaning any language.
     * Set [forceLoad] to load new set of results even if language has not changed.
     * Returns if it will load.
     */
    fun load(query: String, language: String, forceLoad: Boolean): Boolean {
        return if (forceLoad || this.query != query || this.language != language || task == null) {
            this.query = query
            this.language = language

            if (task?.status != AsyncTask.Status.FINISHED) {
                task?.cancel(true)
            }
            task = WorkTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            true
        } else {
            false
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class WorkTask : AsyncTask<Void, Void, Result?>() {

        override fun doInBackground(vararg params: Void?): Result? {
            return if (query.isBlank()) {
                // No query: load a list of shows with new episodes in the last 7 days.
                getShowsWithNewEpisodes()
            } else {
                // Have a query: search using TheTVDB.
                searchShowsOnTvdb()
            }
        }

        private fun getShowsWithNewEpisodes(): Result? {
            val languageActual = TmdbTools2().getSafeLanguageCode(language, languageCodeAny)

            val tmdb = SgApp.getServicesComponent(context).tmdb()
            val call = tmdb.discoverTv()
                    .air_date_lte(dateNow)
                    .air_date_gte(dateOneWeekAgo)
                    .language(languageActual)
                    .build()

            val action = "get shows w new episodes"
            val resultsPage = try {
                val response = call.execute()
                if (response.isSuccessful) {
                    response.body() ?: return buildResultFailure(R.string.tmdb, false)
                } else {
                    Errors.logAndReport(action, response)
                    return buildResultFailure(R.string.tmdb, false)
                }
            } catch (e: Exception) {
                Errors.logAndReport(action, e)
                return buildResultFailure(R.string.tmdb, false)
            }

            val results = resultsPage.results
                ?: return buildResultFailure(R.string.tmdb, false)

            val tvService = tmdb.tvService()
            // TODO Replace with TmdbTools2.mapTvShowsToSearchResults once using coroutines.
            val searchResults = results.mapNotNull { tvShow ->
                if (isCancelled) {
                    return null // do not bother fetching ids for remaining results
                }

                val idResponse = tvShow.id?.let {
                    try {
                        tvService.externalIds(it, null).execute()
                    } catch (e: Exception) {
                        null
                    }
                }

                // On TMDB the TVDB might be 0, ignore those shows, too.
                val externalIds = idResponse?.body()
                if (idResponse == null || !idResponse.isSuccessful
                    || externalIds == null || externalIds.tvdb_id == null
                    || externalIds.tvdb_id == 0) {
                    null // just ignore this show
                } else {
                    SearchResult().apply {
                        tvdbid = externalIds.tvdb_id!!
                        title = tvShow.name
                        overview = tvShow.overview
                        language = languageActual
                    }
                }
            }
            SearchTools().markLocalShowsAsAddedAndSetPosterPath(context, searchResults)
            return buildResultSuccess(searchResults, R.string.add_empty, false)
        }

        private fun searchShowsOnTvdb(): Result? {
            val tvdbTools = SgApp.getServicesComponent(context).tvdbTools()

            try {
                val results = if (language == languageCodeAny) {
                    // use the v1 API to do an any language search not supported by v2
                    tvdbTools.searchShow(query, null)
                } else {
                    tvdbTools.searchSeries(query, language)
                }
                SearchTools().markLocalShowsAsAddedAndSetPosterPath(context, results)
                return buildResultSuccess(results, R.string.no_results, true)
            } catch (e: TvdbException) {
                Timber.e(e, "Searching show failed")
            }

            return buildResultFailure(R.string.tvdb, true)
        }

        private fun buildResultSuccess(results: List<SearchResult>?, @StringRes emptyTextResId: Int,
                isResultsForQuery: Boolean): Result {
            return Result(results ?: emptyList(), context.getString(emptyTextResId),
                    isResultsForQuery,
                    true)
        }

        private fun buildResultFailure(@StringRes serviceResId: Int,
                isResultsForQuery: Boolean): Result {
            // only check for network here to allow hitting the response cache
            val emptyText = if (AndroidUtils.isNetworkConnected(context)) {
                context.getString(R.string.api_error_generic, context.getString(serviceResId))
            } else {
                context.getString(R.string.offline)
            }
            return Result(emptyList(), emptyText, isResultsForQuery, false)
        }

        override fun onPostExecute(result: Result?) {
            if (result != null) {
                value = result
            }
        }

    }

    private val dateNow: TmdbDate
        get() = TmdbDate(Date())

    private val dateOneWeekAgo: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -7)
            return TmdbDate(calendar.time)
        }
}