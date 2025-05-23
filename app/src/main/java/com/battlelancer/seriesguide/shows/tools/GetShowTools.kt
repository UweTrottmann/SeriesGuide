// SPDX-License-Identifier: Apache-2.0
// Copyright 2022-2024 Uwe Trottmann

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.dataliberation.ImportTools.toRating
import com.battlelancer.seriesguide.dataliberation.ImportTools.toVotes
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.database.SgShow2Update
import com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools.ShowService.TMDB
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowDoesNotExist
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowRetry
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowStop
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbError
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbRetry
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbStop
import com.battlelancer.seriesguide.traktapi.TraktTools3
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.uwetrottmann.tmdb2.entities.TvSeason
import timber.log.Timber
import javax.inject.Inject

/**
 * Downloads details of a show.
 */
class GetShowTools @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * If [existingShow] is passed, returns a show for updating, but without its ID set!
     */
    fun getShowDetails(
        showTmdbId: Int,
        desiredLanguage: String,
        existingShow: SgShow2? = null
    ): Result<ShowDetails, GetShowError> {
        var tmdbShow = TmdbTools3.getShowAndExternalIds(showTmdbId, desiredLanguage, context)
            .getOrElse { return Err(it.toGetShowError()) }
            ?: return Err(GetShowDoesNotExist)
        val tmdbSeasons = tmdbShow.seasons

        val noTranslation = tmdbShow.overview.isNullOrEmpty()
        if (noTranslation) {
            tmdbShow = TmdbTools3.getShowAndExternalIds(
                showTmdbId,
                ShowsSettings.getShowsLanguageFallback(context),
                context
            ).getOrElse { return Err(it.toGetShowError()) }
                ?: return Err(GetShowDoesNotExist)
        }

        val traktDetails = TraktTools3.getShowByTmdbId(showTmdbId, context)
            .andThen { traktShow ->
                if (traktShow == null) {
                    Timber.w("getShowDetails: no Trakt show found, using default values.")
                }
                Ok(TraktDetails(
                    traktIdOrNull = traktShow?.ids?.trakt,
                    releaseTime = TimeTools.parseShowReleaseTime(traktShow?.airs?.time),
                    releaseWeekDay = TimeTools.parseShowReleaseWeekDay(traktShow?.airs?.day),
                    releaseCountry = traktShow?.country,
                    releaseTimeZone = traktShow?.airs?.timezone,
                    firstRelease = TimeTools.parseShowFirstRelease(traktShow?.first_aired),
                    rating = traktShow?.rating?.let { if (it in 0.0..10.0) it else 0.0 } ?: 0.0,
                    votes = traktShow?.votes?.let { if (it >= 0) it else 0 } ?: 0
                ))
            }.getOrElse {
                if (existingShow == null) {
                    // Use default values instead of failing.
                    // On the next update Trakt might return a response and all
                    // episode release times are recalculated.
                    TraktDetails(
                        traktIdOrNull = null,
                        releaseTime = TimeTools.parseShowReleaseTime(null),
                        releaseWeekDay = TimeTools.parseShowReleaseWeekDay(null),
                        releaseCountry = null,
                        releaseTimeZone = null,
                        firstRelease = TimeTools.parseShowFirstRelease(null),
                        rating = null,
                        votes = null
                    )
                } else {
                    // Use previously loaded details instead of failing.
                    TraktDetails(
                        traktIdOrNull = existingShow.traktId,
                        releaseTime = existingShow.releaseTimeOrDefault,
                        releaseWeekDay = existingShow.releaseWeekDayOrDefault,
                        releaseCountry = existingShow.releaseCountry,
                        releaseTimeZone = existingShow.releaseTimeZone,
                        firstRelease = existingShow.firstReleaseOrDefault,
                        rating = existingShow.ratingTrakt,
                        votes = existingShow.ratingTraktVotes
                    )
                }
            }

        // The title returned from TMDB is never empty, even when fetching for a language that does
        // not have a translation.
        val title = tmdbShow.name ?: context.getString(R.string.unknown)

        val overview = if (noTranslation || tmdbShow.overview.isNullOrEmpty()) {
            // add note about non-translated or non-existing overview
            var overview = TextTools.textNoTranslation(context, desiredLanguage)
            // if there is one, append non-translated overview
            if (!tmdbShow.overview.isNullOrEmpty()) {
                overview += "\n\n${tmdbShow.overview}"
            }
            overview
        } else {
            tmdbShow.overview
        }

        val tvdbIdOrNull = tmdbShow.external_ids?.tvdb_id
        val titleNoArticle = TextTools.trimLeadingArticle(tmdbShow.name)
        val genres =
            TextTools.buildPipeSeparatedString(tmdbShow.genres?.map { genre -> genre.name })
        val network = tmdbShow.networks?.firstOrNull()?.name ?: ""
        val imdbId = tmdbShow.external_ids?.imdb_id ?: ""
        val runtime = tmdbShow.episode_run_time?.firstOrNull() ?: 45 // estimate 45 minutes if none.
        val status = when (tmdbShow.status) {
            "Returning Series" -> ShowStatus.RETURNING
            "Planned" -> ShowStatus.PLANNED
            "Pilot" -> ShowStatus.PILOT
            "Ended" -> ShowStatus.ENDED
            "Canceled" -> ShowStatus.CANCELED
            "In Production" -> ShowStatus.IN_PRODUCTION
            else -> ShowStatus.UNKNOWN
        }
        val poster = tmdbShow.poster_path ?: ""
        val ratingTmdb = tmdbShow.vote_average.toRating()
        val ratingTmdbVotes = tmdbShow.vote_count.toVotes()

        val showDetails = if (existingShow != null) {
            // For updating existing show.
            ShowDetails(
                showUpdate = SgShow2Update(
                    tvdbId = tvdbIdOrNull,
                    traktId = traktDetails.traktIdOrNull,
                    title = title,
                    titleNoArticle = titleNoArticle,
                    overview = overview,
                    releaseTime = traktDetails.releaseTime,
                    releaseWeekDay = traktDetails.releaseWeekDay,
                    releaseCountry = traktDetails.releaseCountry,
                    releaseTimeZone = traktDetails.releaseTimeZone,
                    firstRelease = traktDetails.firstRelease,
                    ratingTmdb = ratingTmdb,
                    ratingTmdbVotes = ratingTmdbVotes,
                    ratingTrakt = traktDetails.rating,
                    ratingTraktVotes = traktDetails.votes,
                    genres = genres,
                    network = network,
                    imdbId = imdbId,
                    runtime = runtime,
                    status = status,
                    poster = poster,
                    posterSmall = poster,
                    lastUpdatedMs = System.currentTimeMillis() // now
                ),
                seasons = tmdbSeasons
            )
        } else {
            // For inserting new show.
            ShowDetails(
                show = SgShow2(
                    tmdbId = tmdbShow.id,
                    tvdbId = tvdbIdOrNull,
                    traktId = traktDetails.traktIdOrNull,
                    title = title,
                    titleNoArticle = titleNoArticle,
                    overview = overview,
                    releaseTime = traktDetails.releaseTime,
                    releaseWeekDay = traktDetails.releaseWeekDay,
                    releaseCountry = traktDetails.releaseCountry,
                    releaseTimeZone = traktDetails.releaseTimeZone,
                    customReleaseTime = SgShow2.CUSTOM_RELEASE_TIME_NOT_SET,
                    customReleaseDayOffset = SgShow2.CUSTOM_RELEASE_DAY_OFFSET_NOT_SET,
                    customReleaseTimeZone = SgShow2.CUSTOM_RELEASE_TIME_ZONE_NOT_SET,
                    firstRelease = traktDetails.firstRelease,
                    ratingTmdb = ratingTmdb,
                    ratingTmdbVotes = ratingTmdbVotes,
                    ratingTrakt = traktDetails.rating,
                    ratingTraktVotes = traktDetails.votes,
                    ratingUser = null, // If at all, added by TraktRatingsSync
                    genres = genres,
                    network = network,
                    imdbId = imdbId,
                    runtime = runtime,
                    status = status,
                    poster = poster,
                    posterSmall = poster,
                    // set desired language, might not be the content language if fallback used above.
                    language = desiredLanguage,
                    lastUpdatedMs = System.currentTimeMillis(), // now
                    userNote = null, // If at all, added later via Hexagon or Trakt (VIP only)
                    userNoteTraktId = null // If at all, added later via Trakt
                ),
                seasons = tmdbSeasons
            )
        }
        return Ok(showDetails)
    }

    data class ShowDetails(
        val show: SgShow2? = null,
        val showUpdate: SgShow2Update? = null,
        val seasons: List<TvSeason>? = null
    )

    data class TraktDetails(
        val traktIdOrNull: Int?,
        val releaseTime: Int,
        val releaseWeekDay: Int,
        val releaseCountry: String?,
        val releaseTimeZone: String?,
        val firstRelease: String,
        val rating: Double?,
        val votes: Int?
    )

    sealed class GetShowError(val service: AddUpdateShowTools.ShowService) {
        /**
         * The API request might succeed if tried again after a brief delay
         * (e.g. time outs or other temporary network issues).
         */
        class GetShowRetry(service: AddUpdateShowTools.ShowService) : GetShowError(service)

        /**
         * The API request is unlikely to succeed if retried, at least right now
         * (e.g. API bugs or changes).
         */
        class GetShowStop(service: AddUpdateShowTools.ShowService) : GetShowError(service)
        object GetShowDoesNotExist : GetShowError(TMDB)
    }

    private fun TmdbError.toGetShowError(): GetShowError {
        return when (this) {
            TmdbRetry -> GetShowRetry(TMDB)
            TmdbStop -> GetShowStop(TMDB)
        }
    }

}