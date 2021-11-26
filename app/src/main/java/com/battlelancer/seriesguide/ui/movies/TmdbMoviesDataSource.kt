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
import com.uwetrottmann.tmdb2.entities.DiscoverFilter.Separator.AND
import com.uwetrottmann.tmdb2.entities.DiscoverFilter.Separator.OR
import com.uwetrottmann.tmdb2.entities.MovieResultsPage
import com.uwetrottmann.tmdb2.entities.TmdbDate
import com.uwetrottmann.tmdb2.enumerations.ReleaseType
import com.uwetrottmann.tmdb2.enumerations.SortBy
import retrofit2.Call
import retrofit2.awaitResponse
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
    private val regionCode: String,
    private val watchProviderIds: List<Int>?,
    private val watchRegion: String?
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
            call.awaitResponse()
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
        val builder = tmdb.discoverMovie()
            .language(languageCode)
            .region(regionCode)
            .page(page)
        // Only filter by watch provider if release type DIGITAL included.
        if (isLinkFilterable(link)) {
            if (!watchProviderIds.isNullOrEmpty() && watchRegion != null) {
                builder
                    .with_watch_providers(DiscoverFilter(OR, *watchProviderIds.toTypedArray()))
                    .watch_region(watchRegion)
            }
        }
        val action: String
        when (link) {
            MoviesDiscoverLink.POPULAR -> {
                action = "get popular movies"
                builder.sort_by(SortBy.POPULARITY_DESC)
            }
            MoviesDiscoverLink.DIGITAL -> {
                action = "get movie digital releases"
                builder
                    .with_release_type(DiscoverFilter(AND, ReleaseType.DIGITAL))
                    .release_date_lte(dateNow)
                    .release_date_gte(dateOneMonthAgo)
            }
            MoviesDiscoverLink.DISC -> {
                action = "get movie disc releases"
                builder
                    .with_release_type(DiscoverFilter(AND, ReleaseType.PHYSICAL))
                    .release_date_lte(dateNow)
                    .release_date_gte(dateOneMonthAgo)
            }
            MoviesDiscoverLink.IN_THEATERS -> {
                action = "get now playing movies"
                builder
                    .with_release_type(
                        DiscoverFilter(OR, ReleaseType.THEATRICAL, ReleaseType.THEATRICAL_LIMITED)
                    )
                    .release_date_lte(dateNow)
                    .release_date_gte(dateOneMonthAgo)
            }
            MoviesDiscoverLink.UPCOMING -> {
                action = "get upcoming movies"
                builder
                    .with_release_type(
                        DiscoverFilter(OR, ReleaseType.THEATRICAL, ReleaseType.THEATRICAL_LIMITED)
                    )
                    .release_date_gte(dateTomorrow)
                    .release_date_lte(dateInOneMonth)
            }
        }
        return Pair(action, builder.build())
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

    companion object {
        fun isLinkFilterable(link: MoviesDiscoverLink): Boolean =
            link == MoviesDiscoverLink.POPULAR || link == MoviesDiscoverLink.DIGITAL
    }

}