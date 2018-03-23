package com.battlelancer.seriesguide.ui.search

import android.annotation.SuppressLint
import android.arch.lifecycle.LiveData
import android.content.Context
import android.os.AsyncTask
import android.support.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbException
import com.battlelancer.seriesguide.tmdbapi.SgTmdb
import com.battlelancer.seriesguide.traktapi.SgTrakt
import com.battlelancer.seriesguide.ui.shows.ShowTools
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.LinkedList

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
                // no query: load a list of shows with new episodes in the last 7 days
                getShowsWithNewEpisodes()
            } else {
                // have a query:
                if (DisplaySettings.LANGUAGE_EN == language) {
                    // search trakt (has better search) when using English
                    searchShowsOnTrakt()
                } else {
                    // use TheTVDB search for all other (or any) languages
                    searchShowsOnTvdb()
                }
            }
        }

        private fun getShowsWithNewEpisodes(): Result? {
            val languageActual = if (language == languageCodeAny) {
                // TMDB falls back to English if sending 'xx', so set to English beforehand
                DisplaySettings.LANGUAGE_EN
            } else {
                language
            }

            val tmdb = SgApp.getServicesComponent(context).tmdb()
            val call = tmdb.discoverTv()
                    .air_date_lte(dateNow)
                    .air_date_gte(dateOneWeekAgo)
                    .language(languageActual)
                    .build()

            val action = "get shows w new episodes"
            val results = try {
                val response = call.execute()
                if (response.isSuccessful) {
                    response.body() ?: return buildResultFailure(R.string.tmdb, false)
                } else {
                    SgTmdb.trackFailedRequest(context, action, response)
                    return buildResultFailure(R.string.tmdb, false)
                }
            } catch (e: Exception) {
                SgTmdb.trackFailedRequest(context, action, e)
                return buildResultFailure(R.string.tmdb, false)
            }

            val tvService = tmdb.tvService()
            val searchResults = results.results.mapNotNull {
                if (isCancelled) {
                    return null // do not bother fetching ids for remaining results
                }

                val idResponse = try {
                    tvService.externalIds(it.id, null).execute()
                } catch (e: Exception) {
                    null
                }

                val externalIds = idResponse?.body()
                if (idResponse == null || !idResponse.isSuccessful ||
                        externalIds == null || externalIds.tvdb_id == null) {
                    null // just ignore this show
                } else {
                    SearchResult().apply {
                        tvdbid = externalIds.tvdb_id
                        title = it.name
                        overview = it.overview
                        language = languageActual
                    }
                }
            }
            markLocalShows(searchResults)
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
                markLocalShows(results)
                return buildResultSuccess(results, R.string.no_results, true)
            } catch (e: TvdbException) {
                Timber.e(e, "Searching show failed")
            }

            return buildResultFailure(R.string.tvdb, true)
        }

        private fun markLocalShows(results: List<SearchResult>?) {
            val existingPosterPaths = ShowTools.getShowTvdbIdsAndPosters(context)
            if (existingPosterPaths == null || results == null) {
                return
            }

            for (result in results) {
                result.overview = String.format("(%s) %s", result.language, result.overview)

                if (existingPosterPaths.indexOfKey(result.tvdbid) >= 0) {
                    // is already in local database
                    result.state = SearchResult.STATE_ADDED
                    // use the poster we fetched for it (or null if there is none)
                    result.posterPath = existingPosterPaths[result.tvdbid]
                }
            }
        }

        private fun searchShowsOnTrakt(): Result? {
            val traktSearch = SgApp.getServicesComponent(context).traktSearch()

            val searchResults = SgTrakt.executeCall<List<com.uwetrottmann.trakt5.entities.SearchResult>>(
                    context,
                    traktSearch.textQueryShow(query,
                            null, null,
                            null, null,
                            null, null, null, null, null,
                            Extended.FULL,
                            1, 30),
                    "search shows"
            )
            return if (searchResults != null) {
                val shows = searchResults
                        .filter {
                            // skip shows without required TVDB id
                            it.show != null && it.show.ids != null && it.show.ids.tvdb != null
                        }
                        .mapTo(LinkedList<Show>()) { it.show }

                // manually set the language to English
                val results = TraktAddLoader.parseTraktShowsToSearchResults(context,
                        shows, DisplaySettings.LANGUAGE_EN)
                buildResultSuccess(results, R.string.no_results, true)
            } else {
                buildResultFailure(R.string.trakt, true)
            }
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