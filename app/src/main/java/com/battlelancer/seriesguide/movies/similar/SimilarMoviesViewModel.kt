package com.battlelancer.seriesguide.movies.similar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.entities.BaseMovie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.awaitResponse

/**
 * Loads similar movies from TMDB.
 */
class SimilarMoviesViewModel(
    application: Application,
    movieTmdbId: Int
) : AndroidViewModel(application) {

    val resultLiveData = MutableLiveData<Result>()

    init {
        loadSimilarMovies(movieTmdbId)
    }

    fun loadSimilarMovies(movieTmdbId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val languageCode = MoviesSettings.getMoviesLanguage(getApplication())
            val page = try {
                val response = SgApp.getServicesComponent(getApplication()).tmdb()
                    .moviesService()
                    .similar(movieTmdbId, null, languageCode)
                    .awaitResponse()
                if (response.isSuccessful) {
                    response.body()
                } else {
                    Errors.logAndReport("get similar movies", response)
                    postFailedResult()
                    return@launch
                }
            } catch (e: Exception) {
                Errors.logAndReport("get similar movies", e)
                postFailedResult()
                return@launch
            }

            val results = if (page?.results == null) {
                postFailedResult()
                return@launch
            } else {
                page.results
            }

            postSuccessfulResult(results)
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

    private fun postSuccessfulResult(results: List<BaseMovie>) {
        resultLiveData.postValue(
            Result(
                getApplication<Application>().getString(R.string.empty_no_results),
                results
            )
        )
    }

    data class Result(
        val emptyMessage: String,
        val results: List<BaseMovie>?
    )

    companion object {
        val KEY_TMDB_ID_MOVIE = object : CreationExtras.Key<Int> {}

        val Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                val movieTmdbId = this[KEY_TMDB_ID_MOVIE]!!
                SimilarMoviesViewModel(application, movieTmdbId)
            }
        }
    }

}
