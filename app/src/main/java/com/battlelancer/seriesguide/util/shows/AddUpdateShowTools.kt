package com.battlelancer.seriesguide.util.shows

import android.content.Context
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.HexagonError
import com.battlelancer.seriesguide.backend.HexagonRetry
import com.battlelancer.seriesguide.backend.HexagonStop
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgSeason2
import com.battlelancer.seriesguide.provider.SgEpisode2Ids
import com.battlelancer.seriesguide.provider.SgEpisode2TmdbIdUpdate
import com.battlelancer.seriesguide.provider.SgEpisode2Update
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgSeason2Numbers
import com.battlelancer.seriesguide.provider.SgSeason2TmdbIdUpdate
import com.battlelancer.seriesguide.provider.SgSeason2Update
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync
import com.battlelancer.seriesguide.sync.TraktEpisodeSync
import com.battlelancer.seriesguide.tmdbapi.TmdbError
import com.battlelancer.seriesguide.tmdbapi.TmdbRetry
import com.battlelancer.seriesguide.tmdbapi.TmdbStop
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.NextEpisodeUpdater
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.shows.AddUpdateShowTools.ShowService.HEXAGON
import com.battlelancer.seriesguide.util.shows.AddUpdateShowTools.ShowService.TMDB
import com.battlelancer.seriesguide.util.shows.AddUpdateShowTools.ShowService.TRAKT
import com.battlelancer.seriesguide.util.shows.GetShowTools.GetShowError
import com.battlelancer.seriesguide.util.shows.GetShowTools.GetShowError.GetShowDoesNotExist
import com.battlelancer.seriesguide.util.shows.GetShowTools.GetShowError.GetShowRetry
import com.battlelancer.seriesguide.util.shows.GetShowTools.GetShowError.GetShowStop
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShow
import com.uwetrottmann.tmdb2.entities.TvEpisode
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.trakt5.entities.BaseShow
import timber.log.Timber
import java.util.TimeZone

/**
 * Adds or updates a show and its seasons and episodes.
 */
class AddUpdateShowTools(val context: Context) {

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

    fun addShow(
        showTmdbId: Int,
        desiredLanguage: String?,
        traktCollection: Map<Int, BaseShow>?,
        traktWatched: Map<Int, BaseShow>?,
        hexagonEpisodeSync: HexagonEpisodeSync
    ): ShowResult {
        // Do nothing if TMDB ID already in database.
        val showTools = SgApp.getServicesComponent(context).showTools()
        if (showTools.getShowId(showTmdbId, null) != null) return ShowResult.IN_DATABASE

        val language = desiredLanguage ?: DisplaySettings.LANGUAGE_EN

        val showDetails = GetShowTools(context).getShowDetails(showTmdbId, language)
            .getOrElse { return it.toShowResult() }
        val show = showDetails.show!!

        // Check again if in database using TVDB id, show might not have TMDB id, yet.
        if (showTools.getShowId(showTmdbId, show.tvdbId) != null) return ShowResult.IN_DATABASE

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

        val showDetails = GetShowTools(context).getShowDetails(showTmdbId, language, true)
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

    private fun uploadShowsToCloud(shows: List<SgCloudShow>): Boolean {
        return SgApp.getServicesComponent(context).hexagonShowSync().upload(shows)
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

}