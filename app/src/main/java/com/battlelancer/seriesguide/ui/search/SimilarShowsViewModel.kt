package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

/**
 * Loads similar shows from TMDB and maps the results to standard search results with TheTVDB id.
 */
class SimilarShowsViewModel(
    application: Application,
    showTmdbId: Int
) : AndroidViewModel(application) {

    val resultLiveData = MutableLiveData<Result>()

    init {
        loadSimilarShows(showTmdbId)
    }

    fun loadSimilarShows(showTmdbId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()

            // Get similar shows.
            val languageCode = DisplaySettings.getShowsSearchLanguage(getApplication())
            val page = try {
                val response = SgApp.getServicesComponent(getApplication()).tmdb()
                    .tvService()
                    .similar(showTmdbId, null, languageCode)
                    .awaitResponse()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Errors.logAndReport("get similar shows", response)
                    postFailedResult()
                    return@launch
                }
            } catch (e: Exception) {
                Errors.logAndReport("get similar shows", e)
                postFailedResult()
                return@launch
            }

            val results = if (page?.results == null) {
                postFailedResult()
                return@launch
            } else {
                page.results
            }

            val searchResults = SearchTools.mapTvShowsToSearchResults(languageCode, results)
            // Mark local shows and use existing posters.
            SearchTools.markLocalShowsAsAddedAndPreferLocalPoster(context, searchResults)

            postSuccessfulResult(searchResults)
        }
    }

    private fun postFailedResult() {
        val context = getApplication<Application>()
        val message = if (AndroidUtils.isNetworkConnected(context)) {
            context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
        } else {
            context.getString(R.string.offline)
        }
        resultLiveData.postValue(Result(message, null))
    }

    private fun postSuccessfulResult(results: List<SearchResult>) {
        resultLiveData.postValue(
            Result(
                getApplication<Application>().getString(R.string.empty_no_results),
                results
            )
        )
    }

    fun setStateForTmdbId(showTmdbId: Int, newState: Int) {
        val results = resultLiveData.value?.results ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Make a copy (otherwise will modify the item instances used by the adapter).
            val modifiedResults = results.map {
                it.copy()
            }
            // Set new state on affected shows.
            for (element in modifiedResults) {
                if (element.tmdbId == showTmdbId) {
                    element.state = newState
                }
            }
            // Set as new value.
            postSuccessfulResult(modifiedResults)
        }
    }

    fun setAllPendingNotAdded() {
        val results = resultLiveData.value?.results ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Make a copy (otherwise will modify the item instances used by the adapter).
            val modifiedResults = results.map {
                it.copy()
            }
            // Set new state on affected shows.
            for (element in modifiedResults) {
                if (element.state == SearchResult.STATE_ADDING) {
                    element.state = SearchResult.STATE_ADD
                }
            }
            // Set as new value.
            postSuccessfulResult(modifiedResults)
        }
    }

    data class Result(
        val emptyMessage: String,
        val results: List<SearchResult>?
    )

}