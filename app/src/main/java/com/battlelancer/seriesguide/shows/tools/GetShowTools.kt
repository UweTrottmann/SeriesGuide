package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.provider.SgShow2Update
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.tmdbapi.TmdbError
import com.battlelancer.seriesguide.tmdbapi.TmdbRetry
import com.battlelancer.seriesguide.tmdbapi.TmdbStop
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.traktapi.TraktError
import com.battlelancer.seriesguide.traktapi.TraktRetry
import com.battlelancer.seriesguide.traktapi.TraktStop
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools.ShowService.TMDB
import com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools.ShowService.TRAKT
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowDoesNotExist
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowRetry
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowStop
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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
     * If [updateOnly] returns a show for updating, but without its ID set!
     */
    fun getShowDetails(
        showTmdbId: Int,
        desiredLanguage: String,
        updateOnly: Boolean = false
    ): Result<ShowDetails, GetShowError> {
        var tmdbShow = TmdbTools2().getShowAndExternalIds(showTmdbId, desiredLanguage, context)
            .getOrElse { return Err(it.toGetShowError()) }
            ?: return Err(GetShowDoesNotExist)
        val tmdbSeasons = tmdbShow.seasons

        val noTranslation = tmdbShow.overview.isNullOrEmpty()
        if (noTranslation) {
            tmdbShow = TmdbTools2().getShowAndExternalIds(
                showTmdbId,
                ShowsSettings.getShowsLanguageFallback(context),
                context
            ).getOrElse { return Err(it.toGetShowError()) }
                ?: return Err(GetShowDoesNotExist)
        }

        val traktShow = TraktTools2.getShowByTmdbId(showTmdbId, context)
            .getOrElse {
                // Fail if looking up Trakt details failed to avoid removing them for existing shows.
                return Err(it.toGetShowError())
            }
        if (traktShow == null) {
            Timber.w("getShowDetails: no Trakt show found, using default values.")
        }

        val title = if (tmdbShow.name.isNullOrEmpty()) {
            context.getString(R.string.no_translation_title)
        } else {
            tmdbShow.name
        }

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
        val traktIdOrNull = traktShow?.ids?.trakt
        val titleNoArticle = TextTools.trimLeadingArticle(tmdbShow.name)
        val releaseTime = TimeTools.parseShowReleaseTime(traktShow?.airs?.time)
        val releaseWeekDay = TimeTools.parseShowReleaseWeekDay(traktShow?.airs?.day)
        val releaseCountry = traktShow?.country
        val releaseTimeZone = traktShow?.airs?.timezone
        val firstRelease = TimeTools.parseShowFirstRelease(traktShow?.first_aired)
        val rating = traktShow?.rating?.let { if (it in 0.0..10.0) it else 0.0 } ?: 0.0
        val votes = traktShow?.votes?.let { if (it >= 0) it else 0 } ?: 0
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

        val showDetails = if (updateOnly) {
            // For updating existing show.
            ShowDetails(
                showUpdate = SgShow2Update(
                    tvdbId = tvdbIdOrNull,
                    traktId = traktIdOrNull,
                    title = title,
                    titleNoArticle = titleNoArticle,
                    overview = overview,
                    releaseTime = releaseTime,
                    releaseWeekDay = releaseWeekDay,
                    releaseCountry = releaseCountry,
                    releaseTimeZone = releaseTimeZone,
                    firstRelease = firstRelease,
                    ratingGlobal = rating,
                    ratingVotes = votes,
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
                    traktId = traktIdOrNull,
                    title = title,
                    titleNoArticle = titleNoArticle,
                    overview = overview,
                    releaseTime = releaseTime,
                    releaseWeekDay = releaseWeekDay,
                    releaseCountry = releaseCountry,
                    releaseTimeZone = releaseTimeZone,
                    firstRelease = firstRelease,
                    ratingGlobal = rating,
                    ratingVotes = votes,
                    genres = genres,
                    network = network,
                    imdbId = imdbId,
                    runtime = runtime,
                    status = status,
                    poster = poster,
                    posterSmall = poster,
                    // set desired language, might not be the content language if fallback used above.
                    language = desiredLanguage,
                    lastUpdatedMs = System.currentTimeMillis() // now
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

    private fun TraktError.toGetShowError(): GetShowError {
        return when (this) {
            TraktRetry -> GetShowRetry(TRAKT)
            TraktStop -> GetShowStop(TRAKT)
        }
    }

}