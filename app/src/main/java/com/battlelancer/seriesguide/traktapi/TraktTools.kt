// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2014 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.traktapi

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.trakt5.TraktLink
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.BaseEpisode
import com.uwetrottmann.trakt5.entities.BaseSeason
import com.uwetrottmann.trakt5.enums.IdType
import com.uwetrottmann.trakt5.enums.Type
import timber.log.Timber

object TraktTools {

    private val linksBaseUrl = "https://app.trakt.tv"

    fun mapSeasonsByNumber(seasons: List<BaseSeason>): HashMap<Int, BaseSeason> {
        @SuppressLint("UseSparseArrays") val traktSeasonsMap =
            HashMap<Int, BaseSeason>(seasons.size)
        for (season in seasons) {
            val number = season.number
            val episodes = season.episodes
            if (number == null || episodes == null || episodes.isEmpty()) {
                continue // Missing required data, skip
            }
            traktSeasonsMap[number] = season
        }
        return traktSeasonsMap
    }

    fun buildTraktEpisodesMap(episodes: MutableList<BaseEpisode>): HashMap<Int, BaseEpisode> {
        val traktEpisodesMap = HashMap<Int, BaseEpisode>(episodes.size)
        for (episode in episodes) {
            val number = episode.number
                ?: continue // Skip
            traktEpisodesMap[number] = episode
        }
        return traktEpisodesMap
    }

    fun buildShowUrl(slugOrTraktId: String): String =
        "$linksBaseUrl/shows/$slugOrTraktId"

    fun buildEpisodeUrl(showSlugOrTraktId: String, seasonNumber: Int, episodeNumber: Int): String =
        "$linksBaseUrl/shows/$showSlugOrTraktId/seasons/$seasonNumber/episodes/$episodeNumber"

    fun buildMovieUrl(slugOrTraktId: String): String =
        "$linksBaseUrl/movies/$slugOrTraktId"

    /**
     * Converts a rating index from 1 to 10 into the localized string representation. Any other
     * value will return the rate action string.
     */
    fun buildUserRatingString(context: Context, rating: Int?): String {
        val resId = getRatingStringRes(rating)
        return if (resId == 0) {
            context.getString(R.string.action_rate)
        } else {
            context.getString(
                R.string.rating_number_text_format, rating,
                context.getString(resId)
            )
        }
    }

    @StringRes
    private fun getRatingStringRes(rating: Int?): Int {
        return when (rating) {
            1 -> R.string.hate
            2 -> R.string.rating2
            3 -> R.string.rating3
            4 -> R.string.rating4
            5 -> R.string.rating5
            6 -> R.string.rating6
            7 -> R.string.rating7
            8 -> R.string.rating8
            9 -> R.string.rating9
            10 -> R.string.love
            else -> 0
        }
    }

    /**
     * @return `null` if looking up the id failed, -1 if the movie was not found or the movie
     * id if it was found.
     */
    fun lookupMovieTraktId(trakt: TraktV2, movieTmdbId: Int): Int? {
        try {
            val response = trakt
                .search()
                .idLookup(IdType.TMDB, movieTmdbId.toString(), Type.MOVIE, null, 1, 1)
                .execute()
            if (response.isSuccessful) {
                val results = response.body()
                if (results == null || results.size != 1) {
                    Timber.e("Finding movie failed (no results)")
                    return -1
                }

                val traktId = results[0].movie?.ids?.trakt
                if (traktId == null) {
                    Timber.e("Finding movie failed (no Trakt ID)")
                    return null
                }

                return traktId
            } else {
                Errors.logAndReport("movie trakt id lookup", response)
            }
        } catch (e: Exception) {
            Errors.logAndReport("movie trakt id lookup", e)
        }
        return null
    }

}
