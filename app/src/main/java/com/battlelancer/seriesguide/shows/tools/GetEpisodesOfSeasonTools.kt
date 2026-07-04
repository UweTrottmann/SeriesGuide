// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.database.SgEpisode2Ids
import com.battlelancer.seriesguide.shows.database.SgEpisode2Update
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbError
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrElse
import com.uwetrottmann.tmdb2.entities.TvEpisode
import java.util.TimeZone
import kotlin.collections.forEach

/**
 * Helps get episodes of a season from TMDB.
 */
class GetEpisodesOfSeasonTools(
    private val context: Context
) {

    data class ReleaseInfo(
        val releaseTimeZone: String?,
        val releaseTimeOrDefault: Int,
        val customReleaseTimeZone: String,
        val customReleaseTime: Int,
        val customReleaseDayOffset: Int,
        val releaseCountry: String?,
        val network: String?
    )

    data class EpisodeDetails(
        val toInsert: List<SgEpisode2>,
        val toUpdate: List<SgEpisode2Update>,
        val toRemove: List<Long>
    )

    fun getEpisodesOfSeason(
        releaseInfo: ReleaseInfo,
        showTmdbId: Int,
        showId: Long,
        seasonNumber: Int,
        seasonId: Long,
        language: String,
        localEpisodesByTmdbId: MutableMap<Int, SgEpisode2Ids>?,
        localEpisodesWithoutTmdbIdByNumber: MutableMap<Int, SgEpisode2Ids>?
    ): Result<EpisodeDetails, TmdbError> {
        val fallbackLanguage: String? = ShowsSettings.getShowsLanguageFallback(context)
            .let { if (it != language) it else null }

        val tmdbEpisodes = TmdbTools3.getSeason(showTmdbId, seasonNumber, language, context)
            .getOrElse { return Err(it) }

        val tmdbEpisodesFallback = if (fallbackLanguage != null
            && tmdbEpisodes.find { it.name.isNullOrEmpty() || it.overview.isNullOrEmpty() } != null) {
            // Also fetch in fallback language if some episodes have no name or overview.
            TmdbTools3.getSeason(showTmdbId, seasonNumber, fallbackLanguage, context)
                .getOrElse { return Err(it) }
        } else {
            null
        }

        val episodeDetails = mapToSgEpisode2(
            tmdbEpisodes,
            tmdbEpisodesFallback,
            releaseInfo,
            showId,
            seasonId,
            seasonNumber,
            localEpisodesByTmdbId,
            localEpisodesWithoutTmdbIdByNumber
        )

        return Ok(episodeDetails)
    }

    /**
     * If [localEpisodesByTmdbId] is not null, will add update or delete info.
     * Will choose to update episode if not found in [localEpisodesByTmdbId],
     * but found in [localEpisodesWithoutTmdbIdByNumber].
     */
    private fun mapToSgEpisode2(
        tmdbEpisodes: List<TvEpisode>,
        tmdbEpisodesFallback: List<TvEpisode>?,
        releaseInfo: ReleaseInfo,
        showId: Long,
        seasonId: Long,
        seasonNumber: Int,
        localEpisodesByTmdbId: MutableMap<Int, SgEpisode2Ids>?,
        localEpisodesWithoutTmdbIdByNumber: MutableMap<Int, SgEpisode2Ids>?
    ): EpisodeDetails {
        // Only apply release time auto-corrections if not using a custom time.
        val usingCustomTime = releaseInfo.customReleaseTime != SgShow2.CUSTOM_RELEASE_TIME_NOT_SET
        // Prefer custom time zone and release time.
        val showTimeZone = TimeTools.getDateTimeZone(
            if (usingCustomTime) releaseInfo.customReleaseTimeZone else releaseInfo.releaseTimeZone
        )
        val showReleaseTime = TimeTools.getShowReleaseTime(
            if (usingCustomTime) releaseInfo.customReleaseTime else releaseInfo.releaseTimeOrDefault
        )
        val deviceTimeZone = TimeZone.getDefault().id

        val toInsert = mutableListOf<SgEpisode2>()
        val toUpdate = mutableListOf<SgEpisode2Update>()
        tmdbEpisodes.forEach { tmdbEpisode ->
            val tmdbId = tmdbEpisode.id ?: return@forEach

            // If name or overview are empty use fallback
            val isMissingTitle = tmdbEpisode.name.isNullOrEmpty()
            val isMissingOverview = tmdbEpisode.overview.isNullOrEmpty()
            val fallbackEpisode = if (isMissingTitle || isMissingOverview) {
                tmdbEpisodesFallback?.find { it.id == tmdbId }
            } else {
                null
            }
            val titleOrNull = if (isMissingTitle) fallbackEpisode?.name else tmdbEpisode.name
            // Note: trim as contributors sometimes add pointless new lines.
            val overviewOrNull =
                (if (isMissingOverview) fallbackEpisode?.overview else tmdbEpisode.overview)?.trim()

            // calculate release time
            val releaseDateTime = TimeTools.parseEpisodeReleaseDate(
                showTimeZone,
                tmdbEpisode.air_date,
                releaseInfo.customReleaseDayOffset,
                showReleaseTime,
                releaseInfo.releaseCountry,
                releaseInfo.network,
                deviceTimeZone,
                applyCorrections = !usingCustomTime
            )

            val guestStars = tmdbEpisode.guest_stars?.mapNotNull { it.name } ?: emptyList()
            val directors = tmdbEpisode.crew?.filter { it.job == "Director" }
                ?.mapNotNull { it.name }
                ?: emptyList()
            val writers = tmdbEpisode.crew?.filter { it.job == "Writer" }
                ?.mapNotNull { it.name }
                ?: emptyList()

            // Note: last edited time is not available on TMDB,
            // so it and last updated time are currently not used
            // to only update changed episodes.

            // Update if episode with TMDb ID is in database, or if episode with same number is.
            // Why same number? If legacy episodes get added to TMDb they would not get updated,
            // but instead duplicates would be inserted. So instead add the TMDb ID and update
            // the legacy episode.
            val localEpisodeIdOrNull = localEpisodesByTmdbId?.get(tmdbId)
                ?: tmdbEpisode.episode_number?.let { localEpisodesWithoutTmdbIdByNumber?.get(it) }
            if (localEpisodeIdOrNull == null) {
                // Insert
                toInsert.add(
                    SgEpisode2(
                        showId = showId,
                        seasonId = seasonId,
                        tmdbId = tmdbEpisode.id,
                        title = titleOrNull ?: "",
                        overview = overviewOrNull,
                        number = tmdbEpisode.episode_number ?: 0,
                        order = tmdbEpisode.episode_number ?: 0,
                        season = seasonNumber,
                        image = tmdbEpisode.still_path,
                        firstReleasedMs = releaseDateTime,
                        directors = TextTools.buildPipeSeparatedString(directors),
                        guestStars = TextTools.buildPipeSeparatedString(guestStars),
                        writers = TextTools.buildPipeSeparatedString(writers),
                        ratingTmdb = tmdbEpisode.vote_average,
                        ratingTmdbVotes = tmdbEpisode.vote_count,
                        // Trakt ratings loaded later by TraktRatingsFetcher
                        ratingTrakt = null,
                        ratingTraktVotes = null,
                        // Added by TraktRatingsSync
                        ratingUser = null
                    )
                )
            } else {
                // Update
                // Note: update adds TMDb ID in case episode was matched by number.
                toUpdate.add(
                    SgEpisode2Update(
                        id = localEpisodeIdOrNull.id,
                        tmdbId = tmdbId,
                        title = titleOrNull ?: "",
                        overview = overviewOrNull,
                        number = tmdbEpisode.episode_number ?: 0,
                        order = tmdbEpisode.episode_number ?: 0,
                        season = seasonNumber,
                        directors = TextTools.buildPipeSeparatedString(directors),
                        guestStars = TextTools.buildPipeSeparatedString(guestStars),
                        writers = TextTools.buildPipeSeparatedString(writers),
                        image = tmdbEpisode.still_path,
                        firstReleasedMs = releaseDateTime,
                        ratingTmdb = tmdbEpisode.vote_average,
                        ratingTmdbVotes = tmdbEpisode.vote_count,
                    )
                )
                // Remove from map so episode will not get deleted.
                localEpisodesByTmdbId?.remove(tmdbId)
            }
        }

        // Mark any local episodes that are no longer on TMDB for removal.
        val toRemove = localEpisodesByTmdbId?.map { it.value.id } ?: emptyList()

        return EpisodeDetails(toInsert, toUpdate, toRemove)
    }

}