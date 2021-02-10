package com.battlelancer.seriesguide.traktapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import retrofit2.Response
import java.util.HashMap

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

    enum class ServiceResult {
        SUCCESS,
        AUTH_ERROR,
        API_ERROR
    }

    @JvmStatic
    fun getCollectedOrWatchedShows(
        isCollectionNotWatched: Boolean,
        context: Context
    ): Pair<Map<Int, BaseShow>?, ServiceResult> {
        val traktSync = getServicesComponent(context).traktSync()
        val action = if (isCollectionNotWatched) "get collection" else "get watched"
        try {
            val response: Response<List<BaseShow>> = if (isCollectionNotWatched) {
                traktSync.collectionShows(null).execute()
            } else {
                traktSync.watchedShows(null).execute()
            }
            if (response.isSuccessful) {
                return Pair(mapByTmdbId(response.body()), ServiceResult.SUCCESS)
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return Pair(null, ServiceResult.AUTH_ERROR)
                }
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
        }
        return Pair(null, ServiceResult.API_ERROR)
    }

    private fun mapByTmdbId(traktShows: List<BaseShow>?): Map<Int, BaseShow> {
        if (traktShows == null) return emptyMap()

        val traktShowsMap = HashMap<Int, BaseShow>(traktShows.size)
        for (traktShow in traktShows) {
            val tmdbId = traktShow.show?.ids?.tmdb
            if (tmdbId == null || traktShow.seasons.isNullOrEmpty()) {
                continue  // trakt show misses required data, skip.
            }
            traktShowsMap[tmdbId] = traktShow
        }
        return traktShowsMap
    }

}