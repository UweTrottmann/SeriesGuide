package com.battlelancer.seriesguide.traktapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowResult
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.Ratings
import com.uwetrottmann.trakt5.entities.Show
import com.uwetrottmann.trakt5.enums.Extended
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import retrofit2.Response
import java.net.SocketTimeoutException
import java.util.HashMap

object TraktTools2 {

    data class SearchResult(val result: ShowResult, val show: Show?)

    /**
     * Look up a show by its TMDB ID, may return `null` if not found.
     */
    fun getShowByTmdbId(showTmdbId: Int, context: Context): SearchResult {
        val action = "show trakt lookup"
        try {
            val response = getServicesComponent(context).trakt()
                .search()
                .idLookup(
                    IdType.TMDB,
                    showTmdbId.toString(),
                    Type.SHOW,
                    Extended.FULL,
                    1,
                    1
                ).execute()
            if (response.isSuccessful) {
                return SearchResult(ShowResult.SUCCESS, response.body()?.first()?.show)
            } else {
                Errors.logAndReport(action, response)
            }
        } catch (e: Exception) {
            Errors.logAndReport(action, e)
            if (e is SocketTimeoutException) return SearchResult(ShowResult.TIMEOUT_ERROR, null)
        }
        return SearchResult(ShowResult.TRAKT_ERROR, null)
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

    @JvmStatic
    fun mapByTmdbId(traktShows: List<BaseShow>?): Map<Int, BaseShow> {
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

    fun getEpisodeRatings(
        context: Context,
        showTraktId: String,
        seasonNumber: Int,
        episodeNumber: Int
    ): Pair<Double, Int>? {
        val ratings: Ratings =
            SgTrakt.executeCall(
                getServicesComponent(context).trakt()
                    .episodes().ratings(showTraktId, seasonNumber, episodeNumber),
                "get episode rating"
            ) ?: return null
        val rating = ratings.rating
        val votes = ratings.votes
        return if (rating != null && votes != null) {
            Pair(rating, votes)
        } else {
            null
        }
    }

}