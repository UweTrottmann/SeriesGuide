// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.entities.BaseShow
import com.uwetrottmann.trakt5.entities.Ratings
import retrofit2.Response

/**
 * New code should use the [TraktTools4] design patterns.
 */
object TraktTools2 {

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
        val traktSync = SgApp.getServicesComponent(context).traktSync()!!
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
                SgApp.getServicesComponent(context).trakt()
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
