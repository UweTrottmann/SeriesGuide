package com.battlelancer.seriesguide.traktapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type

object TraktTools2 {

    data class SearchResult(val failed: Boolean, val show: Show?)

    /**
     * Look up a show by its TMDB ID, may return `null` if not found.
     * The boolean will be false if the network request failed.
     */
    fun getShowByTmdbId(showTmdbId: Int, context: Context): SearchResult {
        val searchResults = SgTrakt.executeCall(
            getServicesComponent(context).trakt()
                .search()
                .idLookup(
                    IdType.TMDB,
                    showTmdbId.toString(),
                    Type.SHOW,
                    Extended.FULL,
                    1,
                    1
                ),
            "show trakt lookup"
        )
        return SearchResult(searchResults == null, searchResults?.first()?.show)
    }

}