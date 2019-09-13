package com.battlelancer.seriesguide.ui.search

import android.content.Context
import com.battlelancer.seriesguide.ui.shows.ShowTools

class SearchTools {

    fun markLocalShowsAsAddedAndSetPosterPath(context: Context, results: List<SearchResult>?) {
        val existingPosterPaths = ShowTools.getSmallPostersByTvdbId(context)
        if (existingPosterPaths == null || results == null) {
            return
        }

        for (result in results) {
            result.overview = String.format("(%s) %s", result.language, result.overview)

            if (existingPosterPaths.indexOfKey(result.tvdbid) >= 0) {
                // Is already in local database.
                result.state = SearchResult.STATE_ADDED
                // Use the poster we fetched for it (or null if there is none).
                result.posterPath = existingPosterPaths[result.tvdbid]
            }
        }
    }

}