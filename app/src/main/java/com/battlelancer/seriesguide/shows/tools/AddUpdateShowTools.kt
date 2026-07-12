// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.shows.tools

import android.content.Context
import androidx.annotation.StringRes
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.backend.HexagonError
import com.battlelancer.seriesguide.backend.HexagonRetry
import com.battlelancer.seriesguide.backend.HexagonStop
import com.battlelancer.seriesguide.backend.HexagonTools
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.modules.ApplicationContext
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2Ids
import com.battlelancer.seriesguide.shows.database.SgEpisode2TmdbIdUpdate
import com.battlelancer.seriesguide.shows.database.SgSeason2
import com.battlelancer.seriesguide.shows.database.SgSeason2Numbers
import com.battlelancer.seriesguide.shows.database.SgSeason2TmdbIdUpdate
import com.battlelancer.seriesguide.shows.database.SgSeason2Update
import com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools.ShowService.HEXAGON
import com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools.ShowService.TMDB
import com.battlelancer.seriesguide.shows.tools.GetEpisodesOfSeasonTools.ReleaseInfo
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowDoesNotExist
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowRetry
import com.battlelancer.seriesguide.shows.tools.GetShowTools.GetShowError.GetShowStop
import com.battlelancer.seriesguide.streaming.SgWatchProviderShowMapping
import com.battlelancer.seriesguide.streaming.StreamingSearch
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync
import com.battlelancer.seriesguide.sync.HexagonShowSync
import com.battlelancer.seriesguide.sync.TraktEpisodeSync
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbError
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbRetry
import com.battlelancer.seriesguide.tmdbapi.TmdbTools3.TmdbStop
import com.battlelancer.seriesguide.util.LanguageTools
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.mapError
import com.uwetrottmann.seriesguide.backend.shows.model.SgCloudShow
import com.uwetrottmann.tmdb2.entities.TvSeason
import com.uwetrottmann.trakt5.entities.BaseShow
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

/**
 * Adds or updates a show and its seasons and episodes.
 */
class AddUpdateShowTools(
    private val context: Context,
    private val getShowTools: GetShowTools,
    private val hexagonShowSync: Lazy<HexagonShowSync>,
    private val hexagonTools: Lazy<HexagonTools>,
    private val showTools: Lazy<ShowTools2>,
    private val getEpisodesOfSeasonTools: GetEpisodesOfSeasonTools
) {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        getShowTools: GetShowTools,
        hexagonShowSync: Lazy<HexagonShowSync>,
        hexagonTools: Lazy<HexagonTools>,
        showTools: Lazy<ShowTools2>
    ) : this(
        context,
        getShowTools,
        hexagonShowSync,
        hexagonTools,
        showTools,
        GetEpisodesOfSeasonTools(context)
    )

    enum class ShowResult {
        SUCCESS,
        IN_DATABASE,
        DOES_NOT_EXIST,
        TMDB_ERROR,
        HEXAGON_ERROR,
        DATABASE_ERROR
    }

    private class AddShowDatabaseException(val result: ShowResult) : Exception(result.name)

    /**
     * Add a show to the database.
     *
     * If Hexagon is enabled, gets show and episode state from Hexagon.
     *
     * If Hexagon is not enabled and [traktCollection] and [traktWatched] are given, uses
     * [TraktEpisodeSync.storeEpisodeFlags].
     *
     * Note: this calls [updateWatchProviderMappings] which may throw [InterruptedException].
     */
    @Throws(InterruptedException::class)
    fun addShow(
        showTmdbId: Int,
        languageCode: String,
        traktCollection: Map<Int, BaseShow>?,
        traktWatched: Map<Int, BaseShow>?,
        hexagonEpisodeSync: HexagonEpisodeSync
    ): ShowResult {
        // Do nothing if TMDB ID already in database.
        if (showTools.get().getShowId(showTmdbId, null) != null) {
            return ShowResult.IN_DATABASE
        }

        val showDetails = getShowTools.getShowDetails(showTmdbId, languageCode)
            .getOrElse { return it.toShowResult() }
        val show = showDetails.show!!

        // Check again if in database using TVDB id, show might not have TMDB id, yet.
        if (showTools.get().getShowId(showTmdbId, show.tvdbId) != null) {
            return ShowResult.IN_DATABASE
        }

        // Restore properties from Hexagon
        val hexagonEnabled = HexagonSettings.isEnabled(context)
        if (hexagonEnabled) {
            val hexagonShow = hexagonTools.get()
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
                if (hexagonShow.customReleaseTime != null) {
                    show.customReleaseTime = hexagonShow.customReleaseTime
                }
                if (hexagonShow.customReleaseDayOffset != null) {
                    show.customReleaseDayOffset = hexagonShow.customReleaseDayOffset
                }
                if (hexagonShow.customReleaseTimeZone != null) {
                    show.customReleaseTimeZone = hexagonShow.customReleaseTimeZone
                }
                if (hexagonShow.note != null) {
                    show.userNote = hexagonShow.note
                }
            }
        }

        // Run within transaction to avoid show ID foreign key constraint failures and to be able to
        // roll back all changes on failure (to avoid a partially inserted show, such as if adding a
        // season fails).
        val database = SgRoomDatabase.getInstance(context)
        val showId: Long
        try {
            showId = database.runInTransaction<Long> {
                // Store show to database to get row ID
                val showId = database.sgShow2Helper().insertShow(show)
                if (showId == -1L) {
                    throw AddShowDatabaseException(ShowResult.DATABASE_ERROR)
                }

                // Store seasons to database to get row IDs
                val seasons = mapToSgSeason2(showDetails.seasons, showId)
                val seasonIds = database.sgSeason2Helper().insertSeasons(seasons)

                // Download episodes by season and store to database
                val episodeHelper = database.sgEpisode2Helper()
                seasons.forEachIndexed { index, season ->
                    val seasonId = seasonIds[index]
                    if (seasonId == -1L) return@forEachIndexed

                    val episodeDetails = getEpisodesOfSeasonTools.getEpisodesOfSeason(
                        ReleaseInfo(
                            show.releaseTimeZone,
                            show.releaseTimeOrDefault,
                            show.customReleaseTimeZoneOrDefault,
                            show.customReleaseTimeOrDefault,
                            show.customReleaseDayOffsetOrDefault,
                            show.releaseCountry,
                            show.network
                        ),
                        showTmdbId,
                        showId,
                        season.number,
                        seasonId,
                        languageCode,
                        null,
                        null
                    ).getOrElse {
                        // Roll back all changes, user should try again
                        throw AddShowDatabaseException(ShowResult.TMDB_ERROR)
                    }
                    val episodes = episodeDetails.toInsert
                    episodeHelper.insertEpisodes(episodes)
                }

                return@runInTransaction showId
            }
        } catch (e: AddShowDatabaseException) {
            return e.result
        }

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
            cloudShow.language = languageCode
            cloudShow.isRemoved = false
            // Prevent losing restored properties from a legacy Cloud show (see
            // hexagonTools.get().getShow used above) by always sending them.
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

        updateWatchProviderMappings(showId, showTmdbId)

        // Calculate next episode
        NextEpisodeUpdater(context).updateForShows(showId)

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

    /**
     * Updates a show. Adds new, updates changed and removes orphaned episodes.
     *
     * Note: this calls [updateWatchProviderMappings] which may throw [InterruptedException].
     */
    @Throws(InterruptedException::class)
    fun updateShow(showId: Long): UpdateResult {
        val helper = SgRoomDatabase.getInstance(context).sgShow2Helper()
        val show = helper.getShow(showId)
            ?: return UpdateResult.DatabaseError

        val language = show.language.let {
            // Handle legacy records.
            // default to 'en' for consistent behavior across devices
            // and to encourage users to set language
            if (it.isNullOrEmpty()) {
                LanguageTools.LANGUAGE_EN
            } else {
                // Map legacy language codes.
                LanguageTools.mapLegacyShowCode(it)
            }
        }

        var showTmdbId = show.tmdbId ?: 0
        if (showTmdbId == 0) {
            Timber.d("Try to migrate show %d to TMDB IDs", showId)
            showTmdbId = migrateShowToTmdbIds(showId, show.tvdbId ?: 0, language)
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

        val showDetails = getShowTools.getShowDetails(showTmdbId, language, show)
            .getOrElse { return it.toUpdateResult() }
        val updatedShow = showDetails.showUpdate!!
        updatedShow.id = showId

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

            val episodeDetails = getEpisodesOfSeasonTools.getEpisodesOfSeason(
                ReleaseInfo(
                    updatedShow.releaseTimeZone,
                    updatedShow.releaseTime,
                    show.customReleaseTimeZoneOrDefault,
                    show.customReleaseTimeOrDefault,
                    show.customReleaseDayOffsetOrDefault,
                    updatedShow.releaseCountry,
                    updatedShow.network
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

        updateWatchProviderMappings(showId, showTmdbId)

        // At last store shows update (sets last updated timestamp).
        val updated = database.sgShow2Helper().updateShow(updatedShow)
        return if (updated == 1) {
            UpdateResult.Success
        } else {
            UpdateResult.DatabaseError
        }
    }

    /**
     * Download and store watch provider mappings if a streaming search region is configured.
     *
     * Note: this uses [runBlocking], so if the calling thread is interrupted this will throw
     * [InterruptedException].
     */
    @Throws(InterruptedException::class)
    private fun updateWatchProviderMappings(showId: Long, showTmdbId: Int) {
        val region = StreamingSearch.getCurrentRegionOrNull(context) ?: return
        runBlocking {
            val providers = TmdbTools2().getWatchProvidersForShow(showTmdbId, region, context)
            if (providers != null) {
                // Just take all possible options
                (providers.flatrate + providers.free + providers.ads + providers.buy)
                    .mapNotNull { it.provider_id }
                    .distinct()
                    .map { SgWatchProviderShowMapping(it, showId) }
                    .also {
                        val providerHelper =
                            SgRoomDatabase.getInstance(context).sgWatchProviderHelper()
                        providerHelper.deleteShowMappings(showId)
                        // If providers are added that don't exist in the providers table,
                        // not an issue as they just won't be displayed (join will fail).
                        if (it.isNotEmpty()) providerHelper.addShowMappings(it)
                    }
            }
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
    private fun migrateShowToTmdbIds(
        showId: Long,
        showTvdbId: Int,
        language: String
    ): Result<Int, UpdateResult> {
        val database = SgRoomDatabase.getInstance(context)
        val helper = database.sgShow2Helper()

        if (showTvdbId == 0) return Err(UpdateResult.DatabaseError)

        // Find TMDB ID
        return TmdbTools3.findShowTmdbId(context, showTvdbId)
            .mapError { it.toUpdateResult() }
            .andThen {
                if (it == null) Err(UpdateResult.DoesNotExist) else Ok(it)
            }.andThen { showTmdbId ->
                val result = migrateSeasonsToTmdbIds(showId, showTmdbId, language)
                if (result != UpdateResult.Success) return@andThen Err(result)

                // If Hexagon does not have this show by TMDB ID,
                // send current show info and schedule re-upload of episodes using TMDB IDs.
                if (HexagonSettings.isEnabled(context)) {
                    val hexagonShow = hexagonTools.get()
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
                            it.customReleaseTime = show.customReleaseTime
                            it.customReleaseDayOffset = show.customReleaseDayOffset
                            it.customReleaseTimeZone = show.customReleaseTimeZone
                            it.note = show.userNote
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
        val tmdbShow = TmdbTools3.getShowAndExternalIds(showTmdbId, language, context)
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
                TmdbTools3.getSeason(showTmdbId, localSeason.number, language, context)
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
        return hexagonShowSync.get().upload(shows)
    }

    private fun GetShowError.toShowResult(): ShowResult {
        return when (this) {
            GetShowDoesNotExist -> ShowResult.DOES_NOT_EXIST
            else -> when (this.service) {
                HEXAGON -> throw IllegalStateException("getShowDetails does not use HEXAGON")
                TMDB -> ShowResult.TMDB_ERROR
            }
        }
    }

    enum class ShowService(@StringRes val nameResId: Int) {
        HEXAGON(R.string.hexagon),
        TMDB(R.string.tmdb)
    }

    sealed class UpdateResult {
        object Success : UpdateResult()
        object DoesNotExist : UpdateResult()
        class ApiErrorStop(val service: ShowService) : UpdateResult()
        class ApiErrorRetry(val service: ShowService) : UpdateResult()
        object DatabaseError : UpdateResult()
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