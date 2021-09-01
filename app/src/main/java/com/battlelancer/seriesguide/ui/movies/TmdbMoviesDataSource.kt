package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.paging.PageKeyedDataSource
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.search.NetworkState
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.enumerations.ReleaseType
import retrofit2.Call
import java.util.Calendar
import java.util.Date

/**
 * Loads movies from TMDb in chunks.
 *
 * If a query is given, will load search results for that query. Otherwise will load a list of
 * movies based on the given link.
 */
class TmdbMoviesDataSource(
    private val context: Context,
    private val link: MoviesDiscoverLink,
    private val query: String?
) : PageKeyedDataSource<Int, BaseMovie>() {

    data class Page(
        val items: List<BaseMovie>?,
        val totalCount: Int = -1
    )

    val networkState = MutableLiveData<NetworkState>()

    private val tmdb: Tmdb = SgApp.getServicesComponent(context).tmdb()

    private val dateNow: TmdbDate
        get() = TmdbDate(Date())

    private val dateOneMonthAgo: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            return TmdbDate(calendar.time)
        }

    private val dateInOneMonth: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 30)
            return TmdbDate(calendar.time)
        }

    private fun loadPage(page: Int): Page {
        networkState.postValue(NetworkState.LOADING)

        val languageCode = DisplaySettings.getMoviesLanguage(context)
        val regionCode = DisplaySettings.getMoviesRegion(context)

        val action: String
        val call: Call<MovieResultsPage>
        if (query.isNullOrEmpty()) {
            val pair = buildMovieListCall(languageCode, regionCode, page)
            action = pair.first
            call = pair.second
        } else {
            action = "search for movies"
            call = tmdb.searchService()
                .movie(
                    query,
                    page,
                    languageCode,
                    regionCode,
                    false,
                    null,
                    null
                )
        }

        val response = try {
            call.execute()
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            // Not checking for connection until here to allow hitting the response cache.
            return if (AndroidUtils.isNetworkConnected(context)) {
                buildResultGenericFailure()
            } else {
                buildResultOffline()
            }
        } finally {
            networkState.postValue(NetworkState.LOADED)
        }

        // Check for failures or broken body.
        if (!response.isSuccessful) {
            Errors.logAndReport(action, response)
            return buildResultGenericFailure()
        }
        val body = response.body()
        if (body == null) {
            Errors.logAndReport(action, IllegalStateException("body is null"))
            return buildResultGenericFailure()
        }
        val totalResults = body.total_results
        if (totalResults == null) {
            Errors.logAndReport(action, IllegalStateException("total_results is null"))
            return buildResultGenericFailure()
        }

        // Filter null items (a few users affected).
        return Page(body.results?.filterNotNull(), totalResults)
    }

    private fun buildMovieListCall(
        languageCode: String?,
        regionCode: String?,
        page: Int
    ): Pair<String, Call<MovieResultsPage>> {
        val action: String
        val call: Call<MovieResultsPage>
        when (link) {
            MoviesDiscoverLink.POPULAR -> {
                action = "get popular movies"
                call = tmdb.moviesService().popular(page, languageCode, regionCode)
            }
            MoviesDiscoverLink.DIGITAL -> {
                action = "get movie digital releases"
                call = tmdb.discoverMovie()
                    .with_release_type(
                        DiscoverFilter(
                            DiscoverFilter.Separator.AND,
                            ReleaseType.DIGITAL
                        )
                    )
                    .release_date_lte(dateNow)
                    .release_date_gte(dateOneMonthAgo)
                    .language(languageCode)
                    .region(regionCode)
                    .page(page)
                    .build()
            }
            MoviesDiscoverLink.DISC -> {
                action = "get movie disc releases"
                call = tmdb.discoverMovie()
                    .with_release_type(
                        DiscoverFilter(
                            DiscoverFilter.Separator.AND,
                            ReleaseType.PHYSICAL
                        )
                    )
                    .release_date_lte(dateNow)
                    .release_date_gte(dateOneMonthAgo)
                    .language(languageCode)
                    .region(regionCode)
                    .page(page)
                    .build()
            }
            MoviesDiscoverLink.IN_THEATERS -> {
                action = "get now playing movies"
                call = tmdb.discoverMovie()
                    .with_release_type(
                        DiscoverFilter(
                            DiscoverFilter.Separator.OR,
                            ReleaseType.THEATRICAL, ReleaseType.THEATRICAL_LIMITED
                        )
                    )
                    .release_date_lte(dateNow)
                    .release_date_gte(dateOneMonthAgo)
                    .language(languageCode)
                    .region(regionCode)
                    .page(page)
                    .build()
            }
            MoviesDiscoverLink.UPCOMING -> {
                action = "get upcoming movies"
                call = tmdb.discoverMovie()
                    .with_release_type(
                        DiscoverFilter(
                            DiscoverFilter.Separator.OR,
                            ReleaseType.THEATRICAL,
                            ReleaseType.THEATRICAL_LIMITED
                        )
                    )
                    .release_date_gte(dateNow)
                    .release_date_lte(dateInOneMonth)
                    .language(languageCode)
                    .region(regionCode)
                    .page(page)
                    .build()
            }
        }
        return Pair(action, call)
    }

    private fun buildResultGenericFailure(): Page {
        networkState.postValue(
            NetworkState.error(
                context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
            )
        )
        return Page(null)
    }

    private fun buildResultOffline(): Page {
        networkState.postValue(
            NetworkState.error(
                context.getString(R.string.offline)
            )
        )
        return Page(null)
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, BaseMovie>
    ) {
        val page = loadPage(1)
        if (page.items != null) {
            val nextPage = if (page.items.isEmpty()) null else 2
            callback.onResult(page.items, 0, page.totalCount, null, nextPage)
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, BaseMovie>) {
        val pageNumber = params.key
        val page = loadPage(pageNumber)
        if (page.items != null) {
            val previousPage = if (pageNumber > 1) pageNumber - 1 else null
            callback.onResult(page.items, previousPage)
        }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, BaseMovie>) {
        val pageNumber = params.key
        val page = loadPage(pageNumber)
        if (page.items != null) {
            val nextPage = if (page.items.isEmpty()) null else pageNumber + 1
            callback.onResult(page.items, nextPage)
        }
    }
}