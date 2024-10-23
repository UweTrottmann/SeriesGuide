// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.search.discover

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.util.ImageTools
import com.uwetrottmann.tmdb2.entities.BaseTvShow
import com.uwetrottmann.trakt5.entities.BaseShow

/**
 * See [mapToSearchResults].
 */
abstract class SearchResultMapper<SHOW>(
    private val context: Context
) {

    /**
     * Maps to a list of [SearchResult] with [mapToSearchResult].
     *
     * For added shows, changes [SearchResult.state] to [SearchResult.STATE_ADDED] and if available
     * uses the poster path of that show (which might differ if the added show is set to a different
     * language).
     *
     * Builds the final poster URL with [buildPosterUrl].
     */
    fun mapToSearchResults(shows: List<SHOW>): List<SearchResult> {
        val localShowsToPoster =
            SgApp.getServicesComponent(context).showTools().getTmdbIdsToPoster()
        return shows.mapNotNull { show ->
            val searchResult = mapToSearchResult(show)
                ?: return@mapNotNull null

            if (localShowsToPoster.indexOfKey(searchResult.tmdbId) >= 0) {
                // Is already in local database.
                searchResult.state = SearchResult.STATE_ADDED
                // Use the poster already fetched for it.
                val posterPathOrNull = localShowsToPoster[searchResult.tmdbId]
                if (posterPathOrNull != null) {
                    searchResult.posterUrl = posterPathOrNull
                }
            }

            // It may take some time to build the image cache URL, so do this here instead of when
            // binding to the view.
            searchResult.posterUrl = buildPosterUrl(searchResult)

            searchResult
        }
    }

    abstract fun mapToSearchResult(show: SHOW): SearchResult?

    abstract fun buildPosterUrl(searchResult: SearchResult): String?

}

class TmdbSearchResultMapper(
    private val context: Context,
    private val languageCode: String
) : SearchResultMapper<BaseTvShow>(context) {

    override fun mapToSearchResult(show: BaseTvShow): SearchResult? {
        val tmdbId = show.id ?: return null
        val name = show.name ?: return null
        return SearchResult(
            tmdbId = tmdbId,
            title = name,
            overview = show.overview ?: "",
            languageCode = languageCode,
            posterUrl = show.poster_path // temporarily store path
        )
    }

    override fun buildPosterUrl(searchResult: SearchResult): String? {
        return ImageTools.tmdbOrTvdbPosterUrl(searchResult.posterUrl, context)
    }

}

/**
 * Maps Trakt shows to a list of [SearchResult].
 */
class TraktSearchResultMapper(
    private val context: Context,
    private val languageCode: String
) : SearchResultMapper<BaseShow>(context) {

    override fun mapToSearchResult(show: BaseShow): SearchResult? {
        val traktShow = show.show
        val tmdbId = traktShow?.ids?.tmdb ?: return null // has no TMDB id
        val title = traktShow.title ?: return null
        return SearchResult(
            tmdbId = tmdbId,
            title = title,
            // Trakt might not return an overview, so use the year if available
            overview = if (!traktShow.overview.isNullOrEmpty()) {
                traktShow.overview
            } else if (traktShow.year != null) {
                traktShow.year!!.toString()
            } else {
                ""
            },
            languageCode = languageCode,
            posterUrl = null // Trakt does not supply poster URLs
        )
    }

    /**
     * Uses [ImageTools.posterUrlOrResolve] to build the final poster URL or a special resolve URL.
     *
     * Trakt does not return posters, so sets a special URL that makes the image loader resolve
     * them. But for added shows uses their TMDB poster path to build a regular URL.
     */
    override fun buildPosterUrl(searchResult: SearchResult): String? {
        return ImageTools.posterUrlOrResolve(
            searchResult.posterUrl,
            searchResult.tmdbId,
            searchResult.languageCode,
            context
        )
    }

}