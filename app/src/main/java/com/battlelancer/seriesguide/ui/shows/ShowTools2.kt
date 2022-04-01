package com.battlelancer.seriesguide.ui.shows

import android.content.Context
import android.text.format.DateUtils
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.collection.SparseArrayCompat
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.HexagonError
import com.battlelancer.seriesguide.backend.HexagonRetry
import com.battlelancer.seriesguide.backend.HexagonStop
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.enums.NetworkResult
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgSeason2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SgEpisode2Ids
import com.battlelancer.seriesguide.provider.SgEpisode2TmdbIdUpdate
import com.battlelancer.seriesguide.provider.SgEpisode2Update
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgSeason2Numbers
import com.battlelancer.seriesguide.provider.SgSeason2TmdbIdUpdate
import com.battlelancer.seriesguide.provider.SgSeason2Update
import com.battlelancer.seriesguide.provider.SgShow2Update
import com.battlelancer.seriesguide.service.NotificationService
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync
import com.battlelancer.seriesguide.sync.HexagonShowSync
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.sync.TraktEpisodeSync
import com.battlelancer.seriesguide.tmdbapi.TmdbError
import com.battlelancer.seriesguide.tmdbapi.TmdbRetry
import com.battlelancer.seriesguide.tmdbapi.TmdbStop
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.traktapi.TraktError
import com.battlelancer.seriesguide.traktapi.TraktRetry
import com.battlelancer.seriesguide.traktapi.TraktStop
import com.battlelancer.seriesguide.traktapi.TraktTools2
import com.battlelancer.seriesguide.ui.shows.ShowTools.Status
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowService.HEXAGON
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowService.TMDB
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowService.TRAKT
import com.battlelancer.seriesguide.util.NextEpisodeUpdater
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.uwetrottmann.androidutils.AndroidUtils
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShow
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.trakt5.entities.BaseShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.util.TimeZone
import kotlin.collections.set
import com.battlelancer.seriesguide.enums.Result as SgResult

/**
 * Provides some show operations as (async) suspend functions, running within global scope.
 */
class ShowTools2(val showTools: ShowTools, val context: Context) {

    enum class ShowService(@StringRes val nameResId: Int) {
        HEXAGON(R.string.hexagon),
        TMDB(R.string.tmdb),
        TRAKT(R.string.trakt)
    }

    enum class ShowResult {
        SUCCESS,
        IN_DATABASE,
        DOES_NOT_EXIST,
        TMDB_ERROR,
        TRAKT_ERROR,
        HEXAGON_ERROR,
        DATABASE_ERROR
    }

    sealed class UpdateResult {
        object Success : UpdateResult()
        object DoesNotExist : UpdateResult()
        class ApiErrorStop(val service: ShowService) : UpdateResult()
        class ApiErrorRetry(val service: ShowService) : UpdateResult()
        object DatabaseError : UpdateResult()
    }

    /**
     * Gets row ID of a show by TMDB id first, then if given by TVDB id and null TMDB id (show is
     * not migrated, yet). Null if not in database or matched by TVDB id, but has different TMDB id.
     */
    fun getShowId(showTmdbId: Int, showTvdbId: Int?): Long? {
        val helper = SgRoomDatabase.getInstance(context).sgShow2Helper()

        val showIdByTmdbId = helper.getShowIdByTmdbId(showTmdbId)
        if (showIdByTmdbId > 0) return showIdByTmdbId

        if (showTvdbId != null) {
            // Note: TVDB might have a single show that is split into two or more shows on TMDB,
            // so on TMDB the same TVDB is linked for both. To not prevent adding the second one,
            // only return show ID if it was not migrated to avoid adding a duplicate show.
            val showIdByTvdbId = helper.getShowIdByTvdbIdWithNullTmdbId(showTvdbId)
            if (showIdByTvdbId > 0) return showIdByTvdbId
        }

        return null
    }

    data class ShowDetails(
        val show: SgShow2? = null,
        val showUpdate: SgShow2Update? = null,
        val seasons: List<TvSeason>? = null
    )

    sealed class GetShowError(val service: ShowService)

    /**
     * The API request might succeed if tried again after a brief delay
     * (e.g. time outs or other temporary network issues).
     */
    class GetShowRetry(service: ShowService) : GetShowError(service)

    /**
     * The API request is unlikely to succeed if retried, at least right now
     * (e.g. API bugs or changes).
     */
    class GetShowStop(service: ShowService) : GetShowError(service)
    object GetShowDoesNotExist : GetShowError(TMDB)

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
                DisplaySettings.getShowsLanguageFallback(context),
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
            "Returning Series" -> Status.RETURNING
            "Planned" -> Status.PLANNED
            "Pilot" -> Status.PILOT
            "Ended" -> Status.ENDED
            "Canceled" -> Status.CANCELED
            "In Production" -> Status.IN_PRODUCTION
            else -> Status.UNKNOWN
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

    /**
     * Decodes the [ShowTools.Status] and returns the localized text representation.
     * May be `null` if status is unknown.
     */
    fun getStatus(encodedStatus: Int): String? {
        return when (encodedStatus) {
            Status.IN_PRODUCTION -> context.getString(R.string.show_status_in_production)
            Status.PILOT -> context.getString(R.string.show_status_pilot)
            Status.CANCELED -> context.getString(R.string.show_status_canceled)
            Status.PLANNED -> context.getString(R.string.show_isUpcoming)
            Status.RETURNING -> context.getString(R.string.show_isalive)
            Status.ENDED -> context.getString(R.string.show_isnotalive)
            else -> {
                // status unknown, display nothing
                null
            }
        }
    }

    fun addShow(
        showTmdbId: Int,
        desiredLanguage: String?,
        traktCollection: Map<Int, BaseShow>?,
        traktWatched: Map<Int, BaseShow>?,
        hexagonEpisodeSync: HexagonEpisodeSync
    ): ShowResult {
        // Do nothing if TMDB ID already in database.
        if (getShowId(showTmdbId, null) != null) return ShowResult.IN_DATABASE

        val language = desiredLanguage ?: DisplaySettings.LANGUAGE_EN

        val showDetails = getShowDetails(showTmdbId, language)
            .getOrElse { return it.toShowResult() }
        val show = showDetails.show!!

        // Check again if in database using TVDB id, show might not have TMDB id, yet.
        if (getShowId(showTmdbId, show.tvdbId) != null) return ShowResult.IN_DATABASE

        // Restore properties from Hexagon
        val hexagonEnabled = HexagonSettings.isEnabled(context)
        if (hexagonEnabled) {
            val hexagonShow = SgApp.getServicesComponent(context).hexagonTools()
                .getShow(showTmdbId, show.tvdbId)
                .getOrElse { return ShowResult.HEXAGON_ERROR }
            if (hexagonShow != null) {
                if (hexagonShow.isFavorite != null) {
                    show.favorite = hexagonShow.isFavorite
                }
                if (hexagonShow.notify != null) {
                    show.notify = hexagonShow.notify
                }
                if (hexagonShow.isHidden != null) {
                    show.hidden = hexagonShow.isHidden
                }
            }
        }

        // Run within transaction to avoid show ID foreign key constraint failures.
        val database = SgRoomDatabase.getInstance(context)
        var showId = -1L
        val result = database.runInTransaction<ShowResult> {
            // Store show to database to get row ID
            showId = database.sgShow2Helper().insertShow(show)
            if (showId == -1L) return@runInTransaction ShowResult.DATABASE_ERROR

            // Store seasons to database to get row IDs
            val seasons = mapToSgSeason2(showDetails.seasons, showId)
            val seasonIds = database.sgSeason2Helper().insertSeasons(seasons)

            // Download episodes by season and store to database
            val episodeHelper = database.sgEpisode2Helper()
            seasons.forEachIndexed { index, season ->
                val seasonId = seasonIds[index]
                if (seasonId == -1L) return@forEachIndexed

                val episodeDetails = getEpisodesOfSeason(
                    ReleaseInfo(
                        show.releaseTimeZone,
                        show.releaseTimeOrDefault,
                        show.releaseCountry,
                        show.network
                    ),
                    showTmdbId,
                    showId,
                    season.number,
                    seasonId,
                    language,
                    null,
                    null
                ).getOrElse { return@runInTransaction ShowResult.TMDB_ERROR }
                val episodes = episodeDetails.toInsert
                episodeHelper.insertEpisodes(episodes)
            }
            return@runInTransaction ShowResult.SUCCESS
        }
        if (result != ShowResult.SUCCESS) return result

        // restore episode flags...
        if (hexagonEnabled) {
            // ...from Hexagon
            val success = hexagonEpisodeSync.downloadFlags(showId, showTmdbId, show.tvdbId)
            if (!success) {
                // failed to download episode flags
                // flag show as needing an episode merge
                database.sgShow2Helper().setHexagonMergeNotCompleted(showId)
            }

            // Adds the show on Hexagon. Or if it does already exist, clears the isRemoved flag and
            // updates the language, so the show will be auto-added on other connected devices.
            val cloudShow = SgCloudShow()
            cloudShow.tmdbId = showTmdbId
            cloudShow.language = language
            cloudShow.isRemoved = false
            // Prevent losing restored properties from a legacy cloud show by always sending them.
            cloudShow.isFavorite = show.favorite
            cloudShow.isHidden = show.hidden
            cloudShow.notify = show.notify
            uploadShowsToCloud(listOf(cloudShow))
        } else {
            // ...from Trakt
            val traktEpisodeSync = TraktEpisodeSync(context, null)
            if (!traktEpisodeSync.storeEpisodeFlags(
                    traktWatched,
                    showTmdbId,
                    showId,
                    TraktEpisodeSync.Flag.WATCHED
                )) {
                return ShowResult.DATABASE_ERROR
            }
            if (!traktEpisodeSync.storeEpisodeFlags(
                    traktCollection,
                    showTmdbId,
                    showId,
                    TraktEpisodeSync.Flag.COLLECTED
                )) {
                return ShowResult.DATABASE_ERROR
            }
        }

        // Calculate next episode
        NextEpisodeUpdater().updateForShows(context, showId)

        return ShowResult.SUCCESS
    }

    private fun mapToSgSeason2(seasons: List<TvSeason>?, showId: Long): List<SgSeason2> {
        if (seasons.isNullOrEmpty()) return emptyList()
        return seasons.mapNotNull {
            mapToSgSeason2(it, showId)
        }
    }

    private fun mapToSgSeason2(tmdbSeason: TvSeason, showId: Long): SgSeason2? {
        val tmdbId = tmdbSeason.id
        val number = tmdbSeason.season_number
        if (tmdbId == null || number == null) return null
        return SgSeason2(
            showId = showId,
            tmdbId = tmdbId.toString(),
            numberOrNull = number,
            order = number,
            name = tmdbSeason.name
        )
    }

    private fun getEpisodesOfSeason(
        releaseInfo: ReleaseInfo,
        showTmdbId: Int,
        showId: Long,
        seasonNumber: Int,
        seasonId: Long,
        language: String,
        localEpisodesByTmdbId: MutableMap<Int, SgEpisode2Ids>?,
        localEpisodesWithoutTmdbIdByNumber: MutableMap<Int, SgEpisode2Ids>?
    ): Result<EpisodeDetails, TmdbError> {
        val fallbackLanguage: String? = DisplaySettings.getShowsLanguageFallback(context)
            .let { if (it != language) it else null }

        val tmdbEpisodes = TmdbTools2().getSeason(showTmdbId, seasonNumber, language, context)
            .getOrElse { return Err(it) }

        val tmdbEpisodesFallback = if (fallbackLanguage != null
            && tmdbEpisodes.find { it.name.isNullOrEmpty() || it.overview.isNullOrEmpty() } != null) {
            // Also fetch in fallback language if some episodes have no name or overview.
            TmdbTools2().getSeason(showTmdbId, seasonNumber, fallbackLanguage, context)
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

    data class EpisodeDetails(
        val toInsert: List<SgEpisode2>,
        val toUpdate: List<SgEpisode2Update>,
        val toRemove: List<Long>
    )

    data class ReleaseInfo(
        val releaseTimeZone: String?,
        val releaseTimeOrDefault: Int,
        val releaseCountry: String?,
        val network: String?
    )

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
        val showTimeZone = TimeTools.getDateTimeZone(releaseInfo.releaseTimeZone)
        val showReleaseTime = TimeTools.getShowReleaseTime(releaseInfo.releaseTimeOrDefault)
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
                showReleaseTime,
                releaseInfo.releaseCountry,
                releaseInfo.network,
                deviceTimeZone
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
                        writers = TextTools.buildPipeSeparatedString(writers)
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
                        directors = TextTools.buildPipeSeparatedString(directors),
                        guestStars = TextTools.buildPipeSeparatedString(guestStars),
                        writers = TextTools.buildPipeSeparatedString(writers),
                        image = tmdbEpisode.still_path,
                        firstReleasedMs = releaseDateTime
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

    /**
     * Updates a show. Adds new, updates changed and removes orphaned episodes.
     */
    fun updateShow(showId: Long): UpdateResult {
        val helper = SgRoomDatabase.getInstance(context).sgShow2Helper()

        val language = helper.getLanguage(showId).let {
            // handle legacy records
            // default to 'en' for consistent behavior across devices
            // and to encourage users to set language
            if (it.isNullOrEmpty()) DisplaySettings.LANGUAGE_EN else it
        }

        var showTmdbId = helper.getShowTmdbId(showId)
        if (showTmdbId == 0) {
            Timber.d("Try to migrate show %d to TMDB IDs", showId)
            showTmdbId = migrateShowToTmdbIds(showId, language)
                .getOrElse {
                    return when (it) {
                        UpdateResult.DoesNotExist -> {
                            // Can not migrate (yet), try again later.
                            helper.setLastUpdated(showId, System.currentTimeMillis())
                            UpdateResult.Success
                        }
                        else -> it // Failure.
                    }
                }
        }

        val showDetails = getShowDetails(showTmdbId, language, true)
            .getOrElse { return it.toUpdateResult() }
        val show = showDetails.showUpdate!!
        show.id = showId

        // Insert, update and remove seasons.
        val seasons = updateSeasons(showDetails.seasons, showId)
        // Insert, update and remove episodes of inserted or updated seasons.
        val database = SgRoomDatabase.getInstance(context)
        val episodeHelper = database.sgEpisode2Helper()
        seasons.forEach { season ->
            val episodes = episodeHelper.getEpisodeIdsOfSeason(season.id)

            val episodesByTmdbId = mutableMapOf<Int, SgEpisode2Ids>()
            val episodesWithoutTmdbIdByNumber = mutableMapOf<Int, SgEpisode2Ids>()
            episodes.forEach {
                if (it.tmdbId != null) {
                    episodesByTmdbId[it.tmdbId] = it
                } else {
                    episodesWithoutTmdbIdByNumber[it.episodenumber] = it
                }
            }

            val episodeDetails = getEpisodesOfSeason(
                ReleaseInfo(
                    show.releaseTimeZone,
                    show.releaseTime,
                    show.releaseCountry,
                    show.network
                ),
                showTmdbId,
                showId,
                season.number,
                season.id,
                language,
                episodesByTmdbId,
                episodesWithoutTmdbIdByNumber
            ).getOrElse { return it.toUpdateResult() }
            episodeHelper.insertEpisodes(episodeDetails.toInsert)
            episodeHelper.updateEpisodes(episodeDetails.toUpdate)
            episodeHelper.deleteEpisodes(episodeDetails.toRemove)
        }

        // Temporarily disabled to make migration easier for users.
        // - Remakes: newer seasons might be in a separate show.
        // - Anime: all episodes might be combined into single season on TMDb.
        // Remove legacy seasons and episodes that only have a TVDB ID
//        episodeHelper.deleteEpisodesWithoutTmdbId(showId)
//        database.sgSeason2Helper().deleteSeasonsWithoutTmdbId(showId)

        // At last store shows update (sets last updated timestamp).
        val updated = database.sgShow2Helper().updateShow(show)
        return if (updated == 1) {
            UpdateResult.Success
        } else {
            UpdateResult.DatabaseError
        }
    }

    data class SeasonInfo(val id: Long, val number: Int)

    /**
     * Inserts, updates and removes (removal incl. episodes) seasons in the database based on the
     * given seasons.
     * Returns season IDs (and numbers) that were inserted or updated, excluding removed seasons.
     */
    private fun updateSeasons(tmdbSeasons: List<TvSeason>?, showId: Long): List<SeasonInfo> {
        if (tmdbSeasons.isNullOrEmpty()) return emptyList()

        val database = SgRoomDatabase.getInstance(context)
        val helper = database.sgSeason2Helper()
        val seasons = helper.getSeasonNumbersOfShow(showId)

        val seasonsByTmdbId = mutableMapOf<String, SgSeason2Numbers>()
        seasons.forEach {
            if (it.tmdbId != null) seasonsByTmdbId[it.tmdbId] = it
        }

        val toInsert = mutableListOf<SgSeason2>()
        val toUpdate = mutableListOf<SgSeason2Update>()
        val toReturn = mutableListOf<SeasonInfo>()
        tmdbSeasons.forEach { tmdbSeason ->
            val tmdbId = tmdbSeason.id
            val number = tmdbSeason.season_number
            if (tmdbId == null || number == null) return@forEach

            val seasonOrNull = seasonsByTmdbId[tmdbId.toString()]
            if (seasonOrNull == null) {
                // Insert
                mapToSgSeason2(tmdbSeason, showId)?.also {
                    toInsert.add(it)
                }
            } else {
                // Update
                toUpdate.add(
                    SgSeason2Update(
                        id = seasonOrNull.id,
                        number = number,
                        order = number,
                        name = tmdbSeason.name
                    )
                )
                toReturn.add(SeasonInfo(seasonOrNull.id, number))
                // Remove from map so it will not get deleted.
                seasonsByTmdbId.remove(tmdbId.toString())
            }
        }

        if (toInsert.isNotEmpty()) {
            val seasonIds = helper.insertSeasons(toInsert)
            toInsert.forEachIndexed { index, season ->
                val seasonId = seasonIds[index]
                if (seasonId == -1L) return@forEachIndexed
                toReturn.add(SeasonInfo(seasonId, season.number))
            }
        }
        if (toUpdate.isNotEmpty()) helper.updateSeasons(toUpdate)

        // Remove any local season (and its episodes) that is not on TMDB any longer.
        // Note: this rarely happens as seasons can only be removed by mods.
        val toRemove = seasonsByTmdbId.map { it.value.id }
        if (toRemove.isNotEmpty()) {
            database.sgEpisode2Helper().deleteEpisodesOfSeasons(toRemove)
            helper.deleteSeasons(toRemove)
        }

        return toReturn
    }

    /**
     * Finds TMDB ID by TVDB ID, then sets season and episode TMDB IDs by matching on numbers.
     * If Hexagon is enabled and not uploaded via TMDB ID, uploads show info and schedules
     * episode upload.
     */
    private fun migrateShowToTmdbIds(showId: Long, language: String): Result<Int, UpdateResult> {
        val database = SgRoomDatabase.getInstance(context)
        val helper = database.sgShow2Helper()

        val showTvdbId = helper.getShowTvdbId(showId)
        if (showTvdbId == 0) return Err(UpdateResult.DatabaseError)

        // Find TMDB ID
        return TmdbTools2().findShowTmdbId(context, showTvdbId)
            .mapError { it.toUpdateResult() }
            .andThen {
                if (it == null) Err(UpdateResult.DoesNotExist) else Ok(it)
            }.andThen { showTmdbId ->
                val result = migrateSeasonsToTmdbIds(showId, showTmdbId, language)
                if (result != UpdateResult.Success) return@andThen Err(result)

                // If Hexagon does not have this show by TMDB ID,
                // send current show info and schedule re-upload of episodes using TMDB IDs.
                if (HexagonSettings.isEnabled(context)) {
                    val hexagonShow = SgApp.getServicesComponent(context).hexagonTools()
                        .getShow(showTmdbId, null)
                        .getOrElse { return@andThen Err(it.toUpdateResult()) }
                    if (hexagonShow == null) {
                        // Hexagon does not have show via TMDB ID
                        // Upload local show info
                        val show = helper.getForCloudUpdate(showId)
                            ?: return Err(UpdateResult.DatabaseError)
                        val uploadSuccess = uploadShowsToCloud(listOf(SgCloudShow().also {
                            it.tmdbId = showTmdbId
                            it.isFavorite = show.favorite
                            it.notify = show.notify
                            it.isHidden = show.hidden
                            it.language = show.language
                            it.isRemoved = false
                        }))
                        if (!uploadSuccess) return@andThen Err(UpdateResult.ApiErrorStop(HEXAGON))
                        // Schedule episode upload
                        helper.setHexagonMergeNotCompleted(showId)
                    }
                }

                // Set TMDB ID on show last, is used to determine if successfully migrated.
                val updated = helper.updateTmdbId(showId, showTmdbId)
                return@andThen if (updated == 1) Ok(showTmdbId) else Err(UpdateResult.DatabaseError)
            }
    }

    private fun migrateSeasonsToTmdbIds(
        showId: Long,
        showTmdbId: Int,
        language: String
    ): UpdateResult {
        val database = SgRoomDatabase.getInstance(context)
        val seasonNumbers = database.sgSeason2Helper().getSeasonNumbersOfShow(showId)
        val tmdbShow = TmdbTools2().getShowAndExternalIds(showTmdbId, language, context)
            .getOrElse { return it.toUpdateResult() }
            ?: return UpdateResult.DoesNotExist
        val tmdbSeasons = tmdbShow.seasons

        if (tmdbSeasons.isNullOrEmpty()) {
            return if (seasonNumbers.isEmpty()) {
                // No seasons locally or on TMDB, done.
                Timber.d("Migration done early, no seasons")
                UpdateResult.Success
            } else {
                // TMDB has no data, avoid removing and try again later.
                Timber.d("Stopping migration, no seasons on TMDB")
                UpdateResult.DoesNotExist
            }
        }

        // Set TMDB IDs on seasons, match by number.
        val seasonUpdates = mutableListOf<SgSeason2TmdbIdUpdate>()
        seasonNumbers.forEach { localSeason ->
            val seasonTmdbId = tmdbSeasons.find { it.season_number == localSeason.number }?.id
            if (seasonTmdbId == null) {
                Timber.d("Failed to find TMDB ID for season %d", localSeason.number)
                return@forEach
            }

            seasonUpdates.add(SgSeason2TmdbIdUpdate(localSeason.id, seasonTmdbId.toString()))

            val episodeNumbers =
                database.sgEpisode2Helper().getEpisodeNumbersOfSeason(localSeason.id)
            val tmdbEpisodes =
                TmdbTools2().getSeason(showTmdbId, localSeason.number, language, context)
                    .getOrElse { return it.toUpdateResult() }

            if (tmdbEpisodes.isEmpty()) {
                if (episodeNumbers.isEmpty()) {
                    // No episodes, done with this season.
                    Timber.d("Migration done early, season %d has no episodes", localSeason.number)
                    return@forEach
                } else {
                    // TMDB has no data, avoid removing and try again later.
                    Timber.d(
                        "Stopping migration, no episodes for season %d on TMDB",
                        localSeason.number
                    )
                    return UpdateResult.DoesNotExist
                }
            }

            // Set TMDB IDs on episodes, match by number.
            val episodeUpdates = mutableListOf<SgEpisode2TmdbIdUpdate>()
            episodeNumbers.forEach { localEpisode ->
                val episodeTmdbId =
                    tmdbEpisodes.find { it.episode_number == localEpisode.episodenumber }?.id
                // Note: not aborting if an episode is not found,
                // let it get removed by update process.
                if (episodeTmdbId != null) {
                    episodeUpdates.add(
                        SgEpisode2TmdbIdUpdate(localEpisode.id, episodeTmdbId)
                    )
                } else {
                    Timber.d(
                        "Failed to find TMDB ID for episode %d:%d",
                        localSeason.number,
                        localEpisode.episodenumber
                    )
                }
            }
            // Apply episode updates for season.
            Timber.d(
                "Adding TMDB ID to %d of %d episodes of season %d",
                episodeUpdates.size,
                episodeNumbers.size,
                localSeason.number
            )
            database.sgEpisode2Helper().updateTmdbIds(episodeUpdates)
        }

        // Apply season updates.
        Timber.d("Adding TMDB ID to %d of %d seasons", seasonUpdates.size, seasonNumbers.size)
        database.sgSeason2Helper().updateTmdbIds(seasonUpdates)

        return UpdateResult.Success
    }

    /**
     * Posted if a show is about to get removed.
     */
    data class OnRemovingShowEvent(val showId: Long)

    /**
     * Posted if show was just removed (or failure).
     */
    data class OnShowRemovedEvent(
        val showTmdbId: Int,
        /** One of [com.battlelancer.seriesguide.enums.NetworkResult]. */
        val resultCode: Int
    )

    /**
     * Removes a show and its seasons and episodes, including search docs. Sends isRemoved flag to
     * Hexagon so the show will not be auto-added on any device connected to Hexagon.
     *
     * Posts [OnRemovingShowEvent] when starting and [OnShowRemovedEvent] once completed.
     */
    fun removeShow(showId: Long) {
        SgApp.coroutineScope.launch(SgApp.SINGLE) {
            withContext(Dispatchers.Main) {
                EventBus.getDefault().post(OnRemovingShowEvent(showId))
            }

            // Get TMDB id for OnShowRemovedEvent before removing show.
            val showTmdbId = withContext(Dispatchers.IO) {
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            }

            val result = removeShowAsync(showId)

            withContext(Dispatchers.Main) {
                if (result == NetworkResult.OFFLINE) {
                    Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show()
                } else if (result == NetworkResult.ERROR) {
                    Toast.makeText(context, R.string.delete_error, Toast.LENGTH_LONG).show()
                }
                EventBus.getDefault().post(OnShowRemovedEvent(showTmdbId, result))
            }
        }
    }

    /**
     * Returns [com.battlelancer.seriesguide.enums.NetworkResult].
     */
    private suspend fun removeShowAsync(showId: Long): Int {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                // If this is a legacy show (has TVDB ID but no TMDB ID), still allow removal
                // from local database. Do not send to Cloud but pretend no failure.
                val showTvdbId =
                    SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTvdbId(showId)
                return@withContext showTvdbId == 0
            }

            // Sets the isRemoved flag of the given show on Hexagon, so the show will
            // not be auto-added on any device connected to Hexagon.
            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.isRemoved = true

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return SgResult.ERROR

        return withContext(Dispatchers.IO) {
            // Remove database entries in stages, so if an earlier stage fails,
            // user can try again. Also saves memory by using smaller database transactions.
            val database = SgRoomDatabase.getInstance(context)

            var rowsUpdated = database.sgEpisode2Helper().deleteEpisodesOfShow(showId)
            if (rowsUpdated == -1) return@withContext SgResult.ERROR

            rowsUpdated = database.sgSeason2Helper().deleteSeasonsOfShow(showId)
            if (rowsUpdated == -1) return@withContext SgResult.ERROR

            rowsUpdated = database.sgShow2Helper().deleteShow(showId)
            if (rowsUpdated == -1) return@withContext SgResult.ERROR

            SeriesGuideDatabase.rebuildFtsTable(context)
            SgResult.SUCCESS
        }
    }

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeIsFavorite(showId: Long, isFavorite: Boolean) {
        SgApp.coroutineScope.launch {
            storeIsFavoriteAsync(showId, isFavorite)
        }
    }

    private suspend fun storeIsFavoriteAsync(showId: Long, isFavorite: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.isFavorite = isFavorite

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowFavorite(showId, isFavorite)

            // Also notify URI used by lists.
            context.contentResolver
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null)
        }

        // display info toast
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, context.getString(
                    if (isFavorite)
                        R.string.favorited
                    else
                        R.string.unfavorited
                ), Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Saves new hidden flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeIsHidden(showId: Long, isHidden: Boolean) {
        SgApp.coroutineScope.launch {
            storeIsHiddenAsync(showId, isHidden)
        }
    }

    private suspend fun storeIsHiddenAsync(showId: Long, isHidden: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.isHidden = isHidden

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowHidden(showId, isHidden)
        }

        // display info toast
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context, context.getString(
                    if (isHidden)
                        R.string.hidden
                    else
                        R.string.unhidden
                ), Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Saves new notify flag to the local database and, if signed in, up into the cloud as well.
     */
    fun storeNotify(showId: Long, notify: Boolean) {
        SgApp.coroutineScope.launch {
            storeNotifyAsync(showId, notify)
        }
    }

    private suspend fun storeNotifyAsync(showId: Long, notify: Boolean) {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }

            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.notify = notify

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return

        // Save to local database.
        withContext(Dispatchers.IO) {
            SgRoomDatabase.getInstance(context).sgShow2Helper().setShowNotify(showId, notify)
        }

        // new notify setting may determine eligibility for notifications
        withContext(Dispatchers.Default) {
            NotificationService.trigger(context)
        }
    }

    /**
     * Removes hidden flag from all hidden shows in the local database and, if signed in, sends to
     * the cloud as well.
     */
    fun storeAllHiddenVisible() {
        SgApp.coroutineScope.launch {
            // Send to cloud.
            val isCloudFailed = withContext(Dispatchers.Default) {
                if (!HexagonSettings.isEnabled(context)) {
                    return@withContext false
                }
                if (isNotConnected(context)) {
                    return@withContext true
                }

                val hiddenShowTmdbIds = withContext(Dispatchers.IO) {
                    SgRoomDatabase.getInstance(context).sgShow2Helper().getHiddenShowsTmdbIds()
                }

                val shows = hiddenShowTmdbIds.map { tmdbId ->
                    val show = SgCloudShow()
                    show.tmdbId = tmdbId
                    show.isHidden = false
                    show
                }

                val success = withContext(Dispatchers.IO) {
                    uploadShowsToCloud(shows)
                }
                return@withContext !success
            }
            // Do not save to local database if sending to cloud has failed.
            if (isCloudFailed) return@launch

            // Save to local database.
            withContext(Dispatchers.IO) {
                SgRoomDatabase.getInstance(context).sgShow2Helper().makeHiddenVisible()
            }
        }
    }

    fun storeLanguage(showId: Long, languageCode: String) = SgApp.coroutineScope.launch {
        // Send to cloud.
        val isCloudFailed = withContext(Dispatchers.Default) {
            if (!HexagonSettings.isEnabled(context)) {
                return@withContext false
            }
            if (isNotConnected(context)) {
                return@withContext true
            }
            val showTmdbId =
                SgRoomDatabase.getInstance(context).sgShow2Helper().getShowTmdbId(showId)
            if (showTmdbId == 0) {
                return@withContext true
            }

            val show = SgCloudShow()
            show.tmdbId = showTmdbId
            show.language = languageCode

            val success = uploadShowToCloud(show)
            return@withContext !success
        }
        // Do not save to local database if sending to cloud has failed.
        if (isCloudFailed) return@launch

        // Save to local database and schedule sync.
        withContext(Dispatchers.IO) {
            // change language
            val database = SgRoomDatabase.getInstance(context)
            database.sgShow2Helper().updateLanguage(showId, languageCode)
            // reset episode last update time so all get updated
            database.sgEpisode2Helper().resetLastUpdatedForShow(showId)
            // trigger update
            SgSyncAdapter.requestSyncSingleImmediate(context, false, showId)
        }

        withContext(Dispatchers.Main) {
            // show immediate feedback, also if offline and sync won't go through
            if (AndroidUtils.isNetworkConnected(context)) {
                // notify about upcoming sync
                Toast.makeText(context, R.string.update_scheduled, Toast.LENGTH_SHORT).show()
            } else {
                // offline
                Toast.makeText(context, R.string.update_no_connection, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun isNotConnected(context: Context): Boolean {
        val isConnected = AndroidUtils.isNetworkConnected(context)
        // display offline toast
        if (!isConnected) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show()
            }
        }
        return !isConnected
    }

    private suspend fun uploadShowToCloud(show: SgCloudShow): Boolean {
        return withContext(Dispatchers.IO) {
            uploadShowsToCloud(listOf(show))
        }
    }

    private fun uploadShowsToCloud(shows: List<SgCloudShow>): Boolean {
        return HexagonShowSync(
            context,
            SgApp.getServicesComponent(context).hexagonTools()
        ).upload(shows)
    }

    fun getTmdbIdsToPoster(context: Context): SparseArrayCompat<String> {
        val shows = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowsMinimal()
        val map = SparseArrayCompat<String>()
        shows.forEach {
            if (it.tmdbId != null && it.tmdbId != 0) {
                map.put(it.tmdbId, it.posterSmall)
            }
        }
        return map
    }

    fun getTmdbIdsToShowIds(context: Context): Map<Int, Long> {
        val showIds = SgRoomDatabase.getInstance(context).sgShow2Helper().getShowIds()
        val map = mutableMapOf<Int, Long>()
        showIds.forEach {
            if (it.tmdbId != null) map[it.tmdbId] = it.id
        }
        return map
    }

    /**
     * Returns true if the given show has not been updated in the last 12 hours.
     */
    fun shouldUpdateShow(showId: Long): Boolean {
        val lastUpdatedMs = SgRoomDatabase.getInstance(context).sgShow2Helper()
            .getLastUpdated(showId) ?: return false
        return System.currentTimeMillis() - lastUpdatedMs > DateUtils.HOUR_IN_MILLIS * 12
    }

    private fun GetShowError.toShowResult(): ShowResult {
        return when (this) {
            GetShowDoesNotExist -> ShowResult.DOES_NOT_EXIST
            else -> when (this.service) {
                HEXAGON -> throw IllegalStateException("getShowDetails does not use HEXAGON")
                TMDB -> ShowResult.TMDB_ERROR
                TRAKT -> ShowResult.TRAKT_ERROR
            }
        }
    }

    private fun GetShowError.toUpdateResult(): UpdateResult {
        return when (this) {
            GetShowDoesNotExist -> UpdateResult.DoesNotExist
            is GetShowRetry -> UpdateResult.ApiErrorRetry(this.service)
            is GetShowStop -> UpdateResult.ApiErrorStop(this.service)
        }
    }

    private fun HexagonError.toUpdateResult(): UpdateResult {
        return when (this) {
            HexagonRetry -> UpdateResult.ApiErrorRetry(HEXAGON)
            HexagonStop -> UpdateResult.ApiErrorStop(HEXAGON)
        }
    }

    private fun TmdbError.toUpdateResult(): UpdateResult {
        return when (this) {
            TmdbRetry -> UpdateResult.ApiErrorRetry(TMDB)
            TmdbStop -> UpdateResult.ApiErrorStop(TMDB)
        }
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
