package com.battlelancer.seriesguide.ui.search

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.uwetrottmann.tmdb2.entities.BaseTvShow

object SearchTools {

    /**
     * Maps TMDB TV shows to search results.
     */
    fun mapTvShowsToSearchResults(
        languageCode: String,
        results: List<BaseTvShow>
    ): List<SearchResult> {
        return results.mapNotNull { tvShow ->
            val tmdbId = tvShow.id ?: return@mapNotNull null
            SearchResult().also {
                it.tmdbId = tmdbId
                it.title = tvShow.name
                it.overview = tvShow.overview
                it.language = languageCode
                it.posterPath = tvShow.poster_path
            }
        }
    }

    /**
     * Replaces with local poster (e.g. if the user added the show in a different language to
     * ensure it shows up with the same poster and to avoid fetching another image).
     */
    fun markLocalShowsAsAddedAndPreferLocalPoster(context: Context, results: List<SearchResult>?) {
        if (results == null) {
            return
        }

        val localShowsToPoster = SgApp.getServicesComponent(context).showTools().tmdbIdsToPoster
        for (result in results) {
            if (localShowsToPoster.indexOfKey(result.tmdbId) >= 0) {
                // Is already in local database.
                result.state = SearchResult.STATE_ADDED
                // Use the poster already fetched for it.
                val posterOrNull = localShowsToPoster[result.tmdbId]
                if (posterOrNull != null) {
                    result.posterPath = posterOrNull
                }
            }
        }
    }

}