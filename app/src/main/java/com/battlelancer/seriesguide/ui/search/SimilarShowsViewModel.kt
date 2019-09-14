package com.battlelancer.seriesguide.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Loads similar shows from TMDB and maps the results to standard search results with TheTVDB id.
 */
class SimilarShowsViewModel(
    application: Application,
    showTvdbId: Int
) : AndroidViewModel(application) {

    val errorLiveData = MutableLiveData<String?>()
    val resultsLiveData = MutableLiveData<List<SearchResult>>()

    init {
        loadSimilarShows(showTvdbId)
    }

    fun loadSimilarShows(showTvdbId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val tmdbTools = TmdbTools2()

            // Resolve TMDB id.
            val showTmdbId = withContext(Dispatchers.IO) {
                tmdbTools.findShowTmdbId(context, showTvdbId)
            }
            if (showTmdbId == null || showTmdbId <= 0) {
                reportError()
                return@launch
            }

            // Get similar shows.
            val languageCode = tmdbTools.getSafeLanguageCode(
                DisplaySettings.getSearchLanguage(getApplication()),
                context.getString(R.string.language_code_any)
            )
            val page = try {
                val response = SgApp.getServicesComponent(getApplication()).tmdb()
                    .tvService()
                    .similar(showTmdbId, null, languageCode)
                    .execute()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Errors.logAndReport("get similar shows", response)
                    return@launch
                }
            } catch (e: Exception) {
                Errors.logAndReport("get similar shows", e)
                return@launch
            }

            val results = if (page?.results == null) {
                reportError()
                return@launch
            } else {
                page.results
            }

            // Map to search results by resolving TheTVDB id.
            val searchResults =
                tmdbTools.mapTvShowsToSearchResults(context, languageCode, results)
            // Mark local shows and use existing TheTVDB poster path.
            SearchTools().markLocalShowsAsAddedAndSetPosterPath(context, searchResults)

            clearError()
            resultsLiveData.postValue(searchResults)
        }
    }

    private fun reportError() {
        val context = getApplication<Application>()
        val message = if (AndroidUtils.isNetworkConnected(context)) {
            context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
        } else {
            context.getString(R.string.offline)
        }
        errorLiveData.postValue(message)
    }

    private fun clearError() {
        errorLiveData.postValue(null)
    }

    fun setStateForTvdbId(showTvdbId: Int, newState: Int) {
        val results = resultsLiveData.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Make a copy (otherwise will modify the item instances used by the adapter).
            val modifiedResults = results.map {
                it.copy()
            }
            // Set new state on affected shows.
            for (element in modifiedResults) {
                if (element.tvdbid == showTvdbId) {
                    element.state = newState
                }
            }
            // Set as new value.
            resultsLiveData.postValue(modifiedResults)
        }
    }

    fun setAllPendingNotAdded() {
        val results = resultsLiveData.value ?: return
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
            resultsLiveData.postValue(modifiedResults)
        }
    }

}