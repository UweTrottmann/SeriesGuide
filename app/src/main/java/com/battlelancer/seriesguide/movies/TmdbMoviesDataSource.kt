// SPDX-License-Identifier: Apache-2.0
// Copyright 2020-2024 Uwe Trottmann

package com.battlelancer.seriesguide.movies

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
 * Loads movies from TMDb in pages.
 *
 * If a [query] is given, returns search results.
 * If the query is empty and a [link] is given returns results for that link.
 * If no link is given, returns an empty list.
 */
class TmdbMoviesDataSource(
    private val context: Context,
    private val tmdb: Tmdb,
    private val link: MoviesDiscoverLink?,
    private val query: String,
    private val languageCode: String,
    private val regionCode: String,
    private val releaseYear: Int?,
    private val originalLanguageCode: String?,
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
        if (link != null && query.isEmpty()) {
            val pair = buildMovieListCall(link, pageNumber)
            action = pair.first
            call = pair.second
        } else if (query.isNotEmpty()) {
            action = "search for movies"
            // Use year instead of primary_release_year as movies may get released years apart
            // in different regions.
            call = tmdb.searchService()
                .movie(
                    query,
                    pageNumber,
                    languageCode,
                    regionCode,
                    false,
                    releaseYear,
                    null
                )
        } else {
            // Only searching, but no query, yet
            return buildResultEmpty()
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

        return if (movies.isNullOrEmpty()) {
            buildResultEmpty()
        } else {
            LoadResult.Page(
                data = movies,
                prevKey = null, // Only paging forward.
                nextKey = pageNumber + 1
            )
        }
    }

    private fun buildMovieListCall(
        link: MoviesDiscoverLink,
        page: Int
    ): Pair<String, Call<MovieResultsPage>> {
        val builder = tmdb.discoverMovie()
            .language(languageCode)
            .region(regionCode)
            .page(page)
        if (supportsYearFilter(link) && releaseYear != null) {
            // Use year instead of primary_release_year as movies may get released years apart
            // in different regions.
            builder.year(releaseYear)
        }
        if (originalLanguageCode != null) {
            builder.with_original_language(originalLanguageCode)
        }
        // Only filter by watch provider if release type DIGITAL included.
        if (supportsWatchProviderFilter(link)) {
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

    private fun buildResultEmpty() = LoadResult.Page<Int, BaseMovie>(
        data = emptyList(),
        prevKey = null,
        nextKey = null
    )

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
        // Always start loading at the previous page.
        // As refresh is currently only triggered by swipe-to-refresh will always be the first
        // page (prevKey == null).
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey
        }
    }

    companion object {
        fun supportsWatchProviderFilter(link: MoviesDiscoverLink?): Boolean =
            link == MoviesDiscoverLink.POPULAR || link == MoviesDiscoverLink.DIGITAL

        fun supportsYearFilter(link: MoviesDiscoverLink?): Boolean =
            link == MoviesDiscoverLink.POPULAR
    }

}