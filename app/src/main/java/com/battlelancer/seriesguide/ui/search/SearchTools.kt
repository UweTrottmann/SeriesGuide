package com.battlelancer.seriesguide.ui.search

import android.content.Context
import com.battlelancer.seriesguide.SgApp

class SearchTools {

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
            result.overview = String.format("(%s) %s", result.language, result.overview)

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