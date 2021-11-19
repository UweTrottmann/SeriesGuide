package com.battlelancer.seriesguide.ui.movies

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.tmdb2.Tmdb
import com.uwetrottmann.tmdb2.entities.BaseMovie
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.enumerations.ReleaseType
import retrofit2.Call
import java.io.IOException
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
    private val tmdb: Tmdb,
    private val link: MoviesDiscoverLink,
    private val query: String,
    private val languageCode: String,
    private val regionCode: String
) : PagingSource<Int, BaseMovie>() {

    data class Page(
        val items: List<BaseMovie>?,
        val totalCount: Int = -1
    )

    private val dateNow: TmdbDate
        get() = TmdbDate(Date())

    private val dateOneMonthAgo: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -30)
            return TmdbDate(calendar.time)
        }

    private val dateTomorrow: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            return TmdbDate(calendar.time)
        }

    private val dateInOneMonth: TmdbDate
        get() {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, 30)
            return TmdbDate(calendar.time)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BaseMovie> {
        val pageNumber = params.key ?: 1

        val action: String
        val call: Call<MovieResultsPage>
        if (query.isEmpty()) {
            val pair = buildMovieListCall(languageCode, regionCode, pageNumber)
            action = pair.first
            call = pair.second
        } else {
            action = "search for movies"
            call = tmdb.searchService()
                .movie(
                    query,
                    pageNumber,
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
        val movies = body.results?.filterNotNull()

        return if (movies == null || movies.isEmpty()) {
            LoadResult.Page(
                data = emptyList(),
                prevKey = null, // Only paging forward.
                nextKey = null
            )
        } else {
            LoadResult.Page(
                data = movies,
                prevKey = null, // Only paging forward.
                nextKey = pageNumber + 1
            )
        }
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
                    .release_date_gte(dateTomorrow)
                    .release_date_lte(dateInOneMonth)
                    .language(languageCode)
                    .region(regionCode)
                    .page(page)
                    .build()
            }
        }
        return Pair(action, call)
    }

    private fun buildResultGenericFailure(): LoadResult.Error<Int, BaseMovie> {
        val message =
            context.getString(R.string.api_error_generic, context.getString(R.string.tmdb))
        return LoadResult.Error(IOException(message))
    }

    private fun buildResultOffline(): LoadResult.Error<Int, BaseMovie> {
        val message = context.getString(R.string.offline)
        return LoadResult.Error(IOException(message))
    }

    override fun getRefreshKey(state: PagingState<Int, BaseMovie>): Int? {
        // Try to find the page key of the closest page to anchorPosition, from
        // either the prevKey or the nextKey, but you need to handle nullability
        // here:
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey null -> anchorPage is the initial page, so
        //    just return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

}