package com.battlelancer.seriesguide.ui.search

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.thetvdbapi.TvdbException
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.TmdbDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.Date

/**
 * Gets search results for the given query, or if the query is blank gets shows with new episodes.
 */
class ShowsDiscoverLiveData(
    private val context: Context,
    private val scope: CoroutineScope
) : LiveData<ShowsDiscoverLiveData.Result>() {

    data class Result(
        val searchResults: List<SearchResult>,
        val emptyText: String,
        val isResultsForQuery: Boolean,
        val successful: Boolean
    )

    private var query: String = ""
    private var language: String = context.getString(R.string.show_default_language)
    private var currentJob: Job? = null

    /**
     * Schedules loading, give two letter ISO 639-1 [language] code.
     * Set [forceLoad] to load new set of results even if language has not changed.
     * Returns if it will load.
     */
    fun load(query: String, language: String, forceLoad: Boolean): Boolean {
        return if (forceLoad || this.query != query || this.language != language || currentJob == null) {
            this.query = query
            this.language = language

            currentJob?.cancel()
            currentJob = scope.launch(Dispatchers.IO) {
                fetchDiscoverData(query, language)
            }
            true
        } else {
            false
        }
    }

    private suspend fun fetchDiscoverData(
        query: String,
        language: String
    ) = withContext(Dispatchers.IO) {
        val result = if (query.isBlank()) {
            // No query: load a list of shows with new episodes in the last 7 days.
            getShowsWithNewEpisodes(language)
        } else {
            // Have a query: search using TheTVDB.
            searchShowsOnTvdb(query, language)
        }
        // Note: Do not bother posting results if cancelled.
        if (isActive && result != null) {
            postValue(result)
        }
    }

    private suspend fun getShowsWithNewEpisodes(language: String): Result =
        withContext(Dispatchers.IO) {
            val languageActual = language

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
                    response.body() ?: return@withContext buildResultFailure(R.string.tmdb, false)
                } else {
                    Errors.logAndReport(action, response)
                    return@withContext buildResultFailure(R.string.tmdb, false)
                }
            } catch (e: Exception) {
                Errors.logAndReport(action, e)
                return@withContext buildResultFailure(R.string.tmdb, false)
            }

            val results = resultsPage.results
                ?: return@withContext buildResultFailure(R.string.tmdb, false)

            val searchResults = TmdbTools2().mapTvShowsToSearchResults(
                languageActual,
                results
            )
            SearchTools().markLocalShowsAsAddedAndPreferLocalPoster(context, searchResults)
            return@withContext buildResultSuccess(searchResults, R.string.add_empty, false)
        }

    private fun searchShowsOnTvdb(query: String, language: String): Result? {
        val tvdbTools = SgApp.getServicesComponent(context).tvdbTools()

        try {
            val results = tvdbTools.searchSeries(query, language)
            SearchTools().markLocalShowsAsAddedAndPreferLocalPoster(context, results)
            return buildResultSuccess(results, R.string.no_results, true)
        } catch (e: TvdbException) {
            Timber.e(e, "Searching show failed")
        }

        return buildResultFailure(R.string.tvdb, true)
    }

    private fun buildResultSuccess(
        results: List<SearchResult>?, @StringRes emptyTextResId: Int,
        isResultsForQuery: Boolean
    ): Result {
        return Result(
            results ?: emptyList(), context.getString(emptyTextResId),
            isResultsForQuery,
            true
        )
    }

    private fun buildResultFailure(
        @StringRes serviceResId: Int,
        isResultsForQuery: Boolean
    ): Result {
        // only check for network here to allow hitting the response cache
        val emptyText = if (AndroidUtils.isNetworkConnected(context)) {
            context.getString(R.string.api_error_generic, context.getString(serviceResId))
        } else {
            context.getString(R.string.offline)
        }
        return Result(emptyList(), emptyText, isResultsForQuery, false)
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
