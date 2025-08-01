// SPDX-License-Identifier: Apache-2.0
// Copyright 2021-2025 Uwe Trottmann

package com.battlelancer.seriesguide.shows.database

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.ABSOLUTE_NUMBER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.COLLECTED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.DIRECTORS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.DVDNUMBER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.FIRSTAIREDMS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.GUESTSTARS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.IMAGE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.NUMBER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.ORDER
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.OVERVIEW
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.PLAYS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.RATING_TMDB
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.RATING_TMDB_VOTES
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SEASON
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SELECTION_COLLECTED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SELECTION_HAS_RELEASE_DATE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SELECTION_NO_SPECIALS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SELECTION_ONLY_PREMIERES
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SELECTION_UNWATCHED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SORT_RECENT
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.SORT_UPCOMING
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.TITLE
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.TMDB_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.TVDB_ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.WATCHED
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns.WRITERS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns._ID
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.shows.episodes.EpisodeFlags
import com.battlelancer.seriesguide.shows.episodes.EpisodesSettings
import com.battlelancer.seriesguide.util.TimeTools

@Dao
interface SgEpisode2Helper {

    @Insert
    fun insertEpisode(episode: SgEpisode2): Long

    @Insert
    fun insertEpisodes(episodes: List<SgEpisode2>): LongArray

    @Update(entity = SgEpisode2::class)
    fun updateEpisodes(episodes: List<SgEpisode2Update>): Int

    @Update(entity = SgEpisode2::class)
    fun updateTmdbIds(episodes: List<SgEpisode2TmdbIdUpdate>): Int

    @Query("UPDATE sg_episode SET episode_rating = :rating, episode_rating_votes = :votes WHERE _id = :episodeId")
    fun updateRating(episodeId: Long, rating: Double, votes: Int)

    @Query("UPDATE sg_episode SET episode_rating_user = :userRating WHERE _id = :episodeId")
    fun updateUserRating(episodeId: Long, userRating: Int): Int

    @Query("UPDATE sg_episode SET episode_rating_user = :userRating WHERE episode_tmdb_id = :tmdbId")
    fun updateUserRatingByTmdbId(tmdbId: Int, userRating: Int)

    @Transaction
    fun updateUserRatings(tmdbIdsToRating: Map<Int, Int>) {
        tmdbIdsToRating.forEach {
            updateUserRatingByTmdbId(it.key, it.value)
        }
    }

    @Query("UPDATE sg_episode SET episode_imdbid = :imdbId WHERE _id = :episodeId")
    fun updateImdbId(episodeId: Long, imdbId: String)

    @Query("DELETE FROM sg_episode")
    fun deleteAllEpisodes()

    @Query("DELETE FROM sg_episode WHERE _id = :episodeId")
    fun deleteEpisode(episodeId: Long)

    @Transaction
    fun deleteEpisodes(episodeIds: List<Long>) {
        episodeIds.forEach {
            deleteEpisode(it)
        }
    }

    @Query("DELETE FROM sg_episode WHERE series_id = :showId AND episode_tmdb_id IS NULL")
    fun deleteEpisodesWithoutTmdbId(showId: Long)

    @Query("SELECT _id FROM sg_episode WHERE episode_tmdb_id = :tmdbId")
    fun getEpisodeIdByTmdbId(tmdbId: Int): Long

    @Query("SELECT _id FROM sg_episode WHERE episode_tvdb_id = :tvdbId")
    fun getEpisodeIdByTvdbId(tvdbId: Int): Long

    @Query("SELECT _id FROM sg_episode WHERE series_id = :showId AND episode_season_number = :season AND episode_number = :number")
    fun getEpisodeIdByNumber(showId: Long, season: Int, number: Int): Long

    @Query("SELECT episode_tvdb_id FROM sg_episode WHERE _id = :episodeId")
    fun getEpisodeTvdbId(episodeId: Long): Int

    @Query("SELECT episode_tmdb_id FROM sg_episode WHERE _id = :episodeId")
    fun getEpisodeTmdbId(episodeId: Long): Int

    @Query("SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode WHERE _id = :episodeId")
    fun getEpisodeNumbers(episodeId: Long): SgEpisode2Numbers?

    @Query("SELECT _id, season_id, series_id, episode_tvdb_id, episode_title, episode_number, episode_absolute_number, episode_season_number, episode_dvd_number, episode_firstairedms, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE _id = :episodeId")
    fun getEpisodeInfo(episodeId: Long): SgEpisode2Info?

    @RawQuery
    fun getEpisodeInfo(query: SupportSQLiteQuery): SgEpisode2Info?

    fun getEpisodeInfo(
        showId: Long,
        nextEpisodeSelection: String,
        sortClause: String,
        selectionArgs: Array<Any>
    ): SgEpisode2Info? {
        return getEpisodeInfo(
            SimpleSQLiteQuery(
                "SELECT * FROM " + SeriesGuideDatabase.Tables.SG_EPISODE
                        + " WHERE " + SgShow2Columns.REF_SHOW_ID + " = " + showId
                        + " AND " + nextEpisodeSelection
                        + " ORDER BY " + sortClause
                        + " LIMIT 1",
                selectionArgs
            )
        )
    }

    @Query("SELECT * FROM sg_episode WHERE _id = :episodeId")
    fun getEpisode(episodeId: Long): SgEpisode2?

    @Query("SELECT * FROM sg_episode WHERE _id=:id")
    fun getEpisodeLiveData(id: Long): LiveData<SgEpisode2?>

    /**
     * Get the watched or skipped episode with the latest release date, if multiple highest season,
     * if multiple highest number. Or null if none found.
     */
    @Query("SELECT _id, season_id, series_id, episode_tvdb_id, episode_title, episode_number, episode_absolute_number, episode_season_number, episode_dvd_number, episode_firstairedms, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE series_id = :showId AND episode_watched != ${EpisodeFlags.UNWATCHED} ORDER BY episode_firstairedms DESC, episode_season_number DESC, episode_number DESC LIMIT 1")
    fun getNewestWatchedEpisodeOfShow(showId: Long): SgEpisode2Info?

    @Query(
        """SELECT _id FROM sg_episode WHERE season_id = :seasonId 
        ORDER BY episode_number DESC, episode_firstairedms DESC"""
    )
    fun getHighestEpisodeOfSeason(seasonId: Long): Long

    @Query(
        """SELECT _id FROM sg_episode WHERE series_id = :showId 
        AND episode_season_number > 0 AND episode_watched != ${EpisodeFlags.UNWATCHED} 
        AND (episode_season_number < :seasonNumber OR (episode_season_number = :seasonNumber AND episode_number < :episodeNumber))
        ORDER BY episode_season_number DESC, episode_number DESC, episode_firstairedms DESC"""
    )
    fun getPreviousWatchedEpisodeOfShow(showId: Long, seasonNumber: Int, episodeNumber: Int): Long

    /**
     * Also used for compile time validation of [SgEpisode2WithShow.SELECT] (minus the WHERE clause).
     */
    @Query("SELECT sg_episode._id, episode_title, episode_number, episode_season_number, episode_firstairedms, episode_watched, episode_collected, episode_description, series_title, series_network, series_poster_small FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id=sg_show._id WHERE sg_episode._id = :episodeId")
    fun getEpisodeWithShow(episodeId: Long): SgEpisode2WithShow?

    /**
     * See [SgEpisode2WithShow.buildEpisodesWithShowQuery].
     */
    @RawQuery(observedEntities = [SgEpisode2::class, SgShow2::class])
    fun getEpisodesWithShow(query: SupportSQLiteQuery): List<SgEpisode2WithShow>

    /**
     * See [SgEpisode2WithShow.buildEpisodesWithShowQuery].
     */
    @RawQuery(observedEntities = [SgEpisode2::class, SgShow2::class])
    fun getEpisodesWithShowDataSource(query: SupportSQLiteQuery): PagingSource<Int, SgEpisode2WithShow>

    @Query("SELECT _id, episode_tmdb_id, episode_number FROM sg_episode WHERE season_id = :seasonId")
    fun getEpisodeIdsOfSeason(seasonId: Long): List<SgEpisode2Ids>

    /**
     * Also serves as compile time validation of [SgEpisode2Numbers.buildQuery]
     */
    @Query("SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode WHERE season_id = :seasonId ORDER BY episode_season_number ASC, episode_number ASC")
    fun getEpisodeNumbersOfSeason(seasonId: Long): List<SgEpisode2Numbers>

    @RawQuery(observedEntities = [SgEpisode2::class])
    fun getEpisodeNumbersOfSeason(query: SupportSQLiteQuery): List<SgEpisode2Numbers>

    /**
     * Excludes specials.
     */
    @Query("SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode WHERE series_id = :showId AND episode_season_number != 0 ORDER BY episode_season_number ASC, episode_number ASC")
    fun getEpisodeNumbersOfShow(showId: Long): List<SgEpisode2Numbers>

    @Query("SELECT _id, episode_number, episode_season_number, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE series_id = :showId AND episode_tmdb_id > 0 AND (episode_watched != ${EpisodeFlags.UNWATCHED} OR episode_collected = 1)")
    fun getEpisodesForHexagonSync(showId: Long): List<SgEpisode2ForSync>

    @Query("SELECT _id, episode_number, episode_season_number, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE season_id=:seasonId")
    fun getEpisodesForTraktSync(seasonId: Long): List<SgEpisode2ForSync>

    @Query("SELECT _id, episode_number, episode_season_number, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE season_id=:seasonId AND episode_watched = 1 ORDER BY episode_number ASC")
    fun getWatchedEpisodesForTraktSync(seasonId: Long): List<SgEpisode2ForSync>

    @Query("SELECT _id, episode_number, episode_season_number, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE season_id=:seasonId AND episode_collected = 1 ORDER BY episode_number ASC")
    fun getCollectedEpisodesForTraktSync(seasonId: Long): List<SgEpisode2ForSync>

    /**
     * Gets episodes of season ordered by episode number.
     */
    @Query("SELECT * FROM sg_episode WHERE season_id = :seasonId ORDER BY episode_number ASC")
    fun getEpisodesForExport(seasonId: Long): List<SgEpisode2>

    /**
     * WAIT, just for compile time validation of [SgEpisode2Info.buildQuery]
     */
    @Query("SELECT _id, season_id, series_id, episode_tvdb_id, episode_title, episode_number, episode_absolute_number, episode_season_number, episode_dvd_number, episode_firstairedms, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE season_id = :seasonId")
    fun dummyToValidateSgEpisode2Info(seasonId: Long): List<SgEpisode2Info>

    @RawQuery(observedEntities = [SgEpisode2::class])
    fun getEpisodeInfoOfSeasonLiveData(query: SupportSQLiteQuery): LiveData<List<SgEpisode2Info>>

    /**
     * Note: make sure to limit the result set to avoid memory issues, otherwise this may return
     * thousands of rows depending on how many shows are added.
     *
     * If no limit may cause "RuntimeException: Exception while computing database live data."
     * caused by "IllegalStateException: Couldn't read row 2645, col 0 from CursorWindow.
     * Make sure the Cursor is initialized correctly before accessing data from it."
     */
    @RawQuery(observedEntities = [SgEpisode2::class, SgShow2::class])
    fun getEpisodeSearchResults(query: SupportSQLiteQuery): LiveData<List<SgEpisode2SearchResult>>

    @Query("SELECT COUNT(_id) FROM sg_episode")
    fun countEpisodes(): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE episode_season_number != 0")
    fun countEpisodesWithoutSpecials(): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE episode_watched == ${EpisodeFlags.WATCHED}")
    fun countWatchedEpisodes(): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE episode_watched == ${EpisodeFlags.WATCHED} AND episode_season_number != 0")
    fun countWatchedEpisodesWithoutSpecials(): Int

    /**
     * Count episodes of a show excluding specials, but including those without a release date.
     */
    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_season_number != 0")
    suspend fun countEpisodesOfShow(showId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_watched = ${EpisodeFlags.WATCHED}")
    fun countWatchedEpisodesOfShow(showId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_watched = ${EpisodeFlags.WATCHED} AND episode_season_number != 0")
    fun countWatchedEpisodesOfShowWithoutSpecials(showId: Long): Int

    /**
     * Returns if at least one episode of the show is collected. Excludes specials.
     */
    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_collected = 1 AND episode_season_number != 0 LIMIT 1")
    suspend fun hasCollectedEpisodes(showId: Long): Boolean

    /**
     * Returns if at least one episode of a show is left to collect. Excludes specials.
     * Should match with what [updateCollectedOfShowExcludeSpecials] changes.
     */
    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_collected = 0 AND episode_season_number != 0 LIMIT 1")
    suspend fun hasEpisodesToCollect(showId: Long): Boolean

    /**
     * Returns how many episodes of a show are left to watch (only aired and not watched, exclusive
     * episodes with no air date and without specials).
     */
    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_season_number != 0 AND episode_firstairedms != ${SgEpisode2.EPISODE_UNKNOWN_RELEASE} AND episode_firstairedms <= :currentTimeToolsTime")
    fun countNotWatchedEpisodesOfShow(showId: Long, currentTimeToolsTime: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId")
    suspend fun countEpisodesOfSeason(seasonId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_firstairedms != ${SgEpisode2.EPISODE_UNKNOWN_RELEASE} AND episode_firstairedms <= :currentTimeToolsTime")
    suspend fun countNotWatchedReleasedEpisodesOfSeason(
        seasonId: Long,
        currentTimeToolsTime: Long
    ): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_firstairedms > :currentTimeToolsTime")
    suspend fun countNotWatchedToBeReleasedEpisodesOfSeason(
        seasonId: Long,
        currentTimeToolsTime: Long
    ): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_firstairedms = ${SgEpisode2.EPISODE_UNKNOWN_RELEASE}")
    suspend fun countNotWatchedNoReleaseEpisodesOfSeason(seasonId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.SKIPPED}")
    suspend fun countSkippedEpisodesOfSeason(seasonId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_collected = 0")
    suspend fun countNotCollectedEpisodesOfSeason(seasonId: Long): Int

    @Query("UPDATE sg_episode SET episode_watched = 0, episode_plays = 0 WHERE _id = :episodeId")
    fun setNotWatchedAndRemovePlays(episodeId: Long): Int

    @Query("UPDATE sg_episode SET episode_watched = 1, episode_plays = episode_plays + 1 WHERE _id = :episodeId")
    fun setWatchedAndAddPlay(episodeId: Long): Int

    @Query("UPDATE sg_episode SET episode_watched = 2 WHERE _id = :episodeId")
    fun setSkipped(episodeId: Long): Int

    /**
     * Sets not watched or skipped episodes, that have been released,
     * as watched and adds play if these conditions are met:
     *
     * Must
     * - be released before given episode release time,
     * - OR at the same time, but with same (itself) or lower (others released at same time) number.
     *
     * Note: keep in sync with EpisodeWatchedUpToJob.
     */
    @Query(
        """UPDATE sg_episode SET episode_watched = 1, episode_plays = episode_plays + 1 WHERE series_id = :showId
            AND (
            episode_firstairedms < :episodeFirstAired
            OR (episode_firstairedms = :episodeFirstAired AND episode_number <= :episodeNumber)
            )
            AND episode_firstairedms != -1
            AND episode_watched != ${EpisodeFlags.WATCHED}"""
    )
    fun setWatchedUpToAndAddPlay(showId: Long, episodeFirstAired: Long, episodeNumber: Int): Int

    /**
     * See [setWatchedUpToAndAddPlay] for which episodes are returned.
     */
    @Query(
        """SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode WHERE series_id = :showId 
            AND (
            episode_firstairedms < :episodeFirstAired
            OR (episode_firstairedms = :episodeFirstAired AND episode_number <= :episodeNumber)
            )
            AND episode_firstairedms != -1
            AND episode_watched != ${EpisodeFlags.WATCHED}
            ORDER BY episode_season_number ASC, episode_number ASC"""
    )
    fun getEpisodeNumbersForWatchedUpTo(
        showId: Long,
        episodeFirstAired: Long,
        episodeNumber: Int
    ): List<SgEpisode2Numbers>

    /**
     * Note: keep in sync with [setSeasonNotWatchedAndRemovePlays].
     */
    @Query(
        """SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode 
        WHERE season_id = :seasonId AND episode_watched != ${EpisodeFlags.UNWATCHED}
        ORDER BY episode_season_number ASC, episode_number ASC"""
    )
    fun getWatchedOrSkippedEpisodeNumbersOfSeason(seasonId: Long): List<SgEpisode2Numbers>

    /**
     * Sets all watched or skipped as not watched and removes all plays.
     *
     * Note: keep in sync with [getWatchedOrSkippedEpisodeNumbersOfSeason].
     */
    @Query(
        """UPDATE sg_episode SET episode_watched = 0, episode_plays = 0
        WHERE season_id = :seasonId AND episode_watched != ${EpisodeFlags.UNWATCHED}"""
    )
    fun setSeasonNotWatchedAndRemovePlays(seasonId: Long): Int

    @Query(
        """UPDATE sg_episode SET episode_watched = 0, episode_plays = 0
        WHERE season_id = :seasonId AND episode_watched == ${EpisodeFlags.WATCHED}"""
    )
    fun setSeasonNotWatchedExcludeSkipped(seasonId: Long): Int

    @Transaction
    fun setSeasonsNotWatchedExcludeSkipped(seasonIds: List<Long>) {
        for (seasonId in seasonIds) {
            setSeasonNotWatchedExcludeSkipped(seasonId)
        }
    }

    @Query(
        """UPDATE sg_episode SET episode_watched = 0, episode_plays = 0
        WHERE series_id = :showId AND episode_watched == ${EpisodeFlags.WATCHED}"""
    )
    fun setShowNotWatchedExcludeSkipped(showId: Long): Int

    @Transaction
    fun setShowsNotWatchedExcludeSkipped(showIds: List<Long>) {
        for (seasonId in showIds) {
            setShowNotWatchedExcludeSkipped(seasonId)
        }
    }

    /**
     * Does NOT include watched episodes to avoid Trakt adding a new play.
     *
     * Note: keep in sync with [setSeasonSkipped] and [setSeasonWatchedAndAddPlay].
     */
    @Query(
        """SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode 
        WHERE season_id = :seasonId AND episode_watched != ${EpisodeFlags.WATCHED}
        ORDER BY episode_season_number ASC, episode_number ASC"""
    )
    fun getNotWatchedOrSkippedEpisodeNumbersOfSeason(seasonId: Long): List<SgEpisode2Numbers>

    @Query("UPDATE sg_episode SET episode_watched = 1, episode_plays = 1 WHERE season_id = :seasonId")
    fun setSeasonWatched(seasonId: Long): Int

    /**
     * Sets all not watched or skipped episodes of a season as watched and adds a play.
     *
     * Does NOT mark watched episodes again to avoid adding a new play (Trakt and local).
     *
     * Note: keep in sync with [getNotWatchedOrSkippedEpisodeNumbersOfSeason].
     */
    @Query(
        """UPDATE sg_episode SET episode_watched = 1, episode_plays = episode_plays + 1 
            WHERE season_id = :seasonId AND episode_watched != ${EpisodeFlags.WATCHED}"""
    )
    fun setSeasonWatchedAndAddPlay(seasonId: Long): Int

    /**
     * Sets all not watched episodes of a season as skipped.
     *
     * Note: keep in sync with [getNotWatchedOrSkippedEpisodeNumbersOfSeason].
     */
    @Query(
        """UPDATE sg_episode SET episode_watched = 2 
            WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.UNWATCHED}"""
    )
    fun setSeasonSkipped(seasonId: Long): Int

    /**
     * Note: keep in sync with [setShowNotWatchedAndRemovePlays].
     */
    @Query(
        """SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode 
        WHERE series_id = :showId AND episode_watched != ${EpisodeFlags.UNWATCHED}
        AND episode_season_number != 0
        ORDER BY episode_season_number ASC, episode_number ASC"""
    )
    fun getWatchedOrSkippedEpisodeNumbersOfShow(showId: Long): List<SgEpisode2Numbers>

    /**
     * Sets watched or skipped episodes, excluding specials, as not watched and removes all plays.
     *
     * Note: keep in sync with [getWatchedOrSkippedEpisodeNumbersOfShow].
     */
    @Query(
        """UPDATE sg_episode SET episode_watched = 0, episode_plays = 0
            WHERE series_id = :showId AND episode_watched != ${EpisodeFlags.UNWATCHED}
            AND episode_season_number != 0"""
    )
    fun setShowNotWatchedAndRemovePlays(showId: Long): Int

    /**
     * Note: keep in sync with [setShowWatchedAndAddPlay].
     */
    @Query(
        """SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode 
        WHERE series_id = :showId AND episode_watched != ${EpisodeFlags.WATCHED}
        AND episode_firstairedms <= :currentTimePlusOneHour AND episode_firstairedms != -1
        AND episode_season_number != 0
        ORDER BY episode_season_number ASC, episode_number ASC"""
    )
    fun getNotWatchedOrSkippedEpisodeNumbersOfShow(
        showId: Long,
        currentTimePlusOneHour: Long
    ): List<SgEpisode2Numbers>

    /**
     * Sets not watched or skipped episodes, released until within the hour, excluding specials,
     * as watched and adds play.
     *
     * Does NOT mark watched episodes again to avoid adding a new play (Trakt and local).
     *
     * Note: keep in sync with [getNotWatchedOrSkippedEpisodeNumbersOfShow].
     */
    @Query(
        """UPDATE sg_episode SET episode_watched = 1, episode_plays = episode_plays + 1
            WHERE series_id = :showId AND episode_watched != ${EpisodeFlags.WATCHED}
            AND episode_firstairedms <= :currentTimePlusOneHour AND episode_firstairedms != -1
            AND episode_season_number != 0"""
    )
    fun setShowWatchedAndAddPlay(showId: Long, currentTimePlusOneHour: Long): Int

    @Update(entity = SgEpisode2::class)
    fun updateEpisodesWatched(episodes: List<SgEpisode2WatchedUpdate>): Int

    @Update(entity = SgEpisode2::class)
    fun updateEpisodesCollected(episodes: List<SgEpisode2CollectedUpdate>): Int

    @Query("UPDATE sg_episode SET episode_collected = :isCollected WHERE _id = :episodeId")
    fun updateCollected(episodeId: Long, isCollected: Boolean): Int

    @Query("UPDATE sg_episode SET episode_collected = :isCollected WHERE season_id = :seasonId")
    fun updateCollectedOfSeason(seasonId: Long, isCollected: Boolean): Int

    @Transaction
    fun updateCollectedOfSeasons(seasonIds: List<Long>, isCollected: Boolean) {
        for (seasonId in seasonIds) {
            updateCollectedOfSeason(seasonId, isCollected)
        }
    }

    @Query("UPDATE sg_episode SET episode_collected = :isCollected WHERE series_id = :showId")
    fun updateCollectedOfShow(showId: Long, isCollected: Boolean): Int

    @Transaction
    fun updateCollectedOfShows(showIds: List<Long>, isCollected: Boolean) {
        for (seasonId in showIds) {
            updateCollectedOfShow(seasonId, isCollected)
        }
    }

    @Query("UPDATE sg_episode SET episode_collected = :isCollected WHERE series_id = :showId AND episode_season_number != 0")
    fun updateCollectedOfShowExcludeSpecials(showId: Long, isCollected: Boolean): Int

    @Query("UPDATE sg_episode SET episode_watched = :watched, episode_plays = :plays WHERE series_id = :showId AND episode_season_number = :seasonNumber AND episode_number = :episodeNumber")
    fun updateWatchedByNumber(
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
        watched: Int,
        plays: Int
    )

    @Query("UPDATE sg_episode SET episode_collected = :isCollected WHERE series_id = :showId AND episode_season_number = :seasonNumber AND episode_number = :episodeNumber")
    fun updateCollectedByNumber(
        showId: Long,
        seasonNumber: Int,
        episodeNumber: Int,
        isCollected: Boolean
    )

    @Transaction
    fun updateWatchedAndCollectedByNumber(
        watched: List<SgEpisode2WatchedUpdateByNumber>,
        collected: List<SgEpisode2CollectedUpdateByNumber>
    ) {
        for (episode in watched) {
            updateWatchedByNumber(
                episode.showId,
                episode.seasonNumber,
                episode.episodeNumber,
                episode.watched,
                episode.plays
            )
        }
        for (episode in collected) {
            updateCollectedByNumber(
                episode.showId,
                episode.seasonNumber,
                episode.episodeNumber,
                episode.collected
            )
        }
    }

    /**
     * Note: currently last updated value is unused, all episodes are always updated.
     * See [com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools].
     */
    @Query("UPDATE sg_episode SET episode_lastupdate = 0")
    fun resetLastUpdatedForAll()

    /**
     * Note: currently last updated value is unused, all episodes are always updated.
     * See [com.battlelancer.seriesguide.shows.tools.AddUpdateShowTools].
     */
    @Query("UPDATE sg_episode SET episode_lastupdate = 0 WHERE series_id = :showId")
    fun resetLastUpdatedForShow(showId: Long)

    @Query("DELETE FROM sg_episode WHERE season_id = :seasonId")
    fun deleteEpisodesOfSeason(seasonId: Long): Int

    @Transaction
    fun deleteEpisodesOfSeasons(seasonIds: List<Long>) {
        seasonIds.forEach {
            deleteEpisodesOfSeason(it)
        }
    }

    @Query("DELETE FROM sg_episode WHERE series_id = :showId")
    suspend fun deleteEpisodesOfShow(showId: Long): Int

    /**
     * Returns at most 500 episodes matching the [searchTermOrNull].
     *
     * If [showTitleOrNull] is given, filters the results by show title.
     */
    fun searchForEpisodes(
        context: Context,
        searchTermOrNull: String?,
        showTitleOrNull: String?
    ): LiveData<List<SgEpisode2SearchResult>> {
        val query = StringBuilder(QUERY_SEARCH_EPISODES)
        if (showTitleOrNull != null) {
            query.append(" WHERE (").append(SgShow2Columns.TITLE).append(" = ?)")
        }
        query.append(" ORDER BY ${SgShow2Columns.SORT_TITLE},$SEASON ASC,$NUMBER ASC")
        // Limit result set to avoid memory issues.
        query.append(" LIMIT 500")

        var searchTerm = if (searchTermOrNull.isNullOrEmpty()) "" else searchTermOrNull
        // ensure to strip double quotation marks (would break the MATCH query)
        searchTerm = searchTerm.replace("\"", "")
        // search for anything starting with the given search term
        searchTerm = "\"$searchTerm*\""

        val selectionArgs: Array<Any> = if (showTitleOrNull != null) {
            arrayOf(searchTerm, showTitleOrNull)
        } else {
            arrayOf(searchTerm)
        }

        return getEpisodeSearchResults(SimpleSQLiteQuery(query.toString(), selectionArgs))
    }

    companion object {
        private const val DOCID = "DOCID"

        private const val EPISODE_COLUMNS: String =
            "$_ID,$TITLE,$NUMBER,$SEASON,$WATCHED"

        private const val SELECT_SHOWS: String =
            "SELECT ${SgShow2Columns._ID} as sid,${SgShow2Columns.TITLE},${SgShow2Columns.POSTER_SMALL}" +
                    " FROM ${Tables.SG_SHOW}"

        private const val SELECT_MATCH: String =
            "SELECT $DOCID,snippet(${Tables.EPISODES_SEARCH},'<b>','</b>','...') AS $OVERVIEW" +
                    " FROM ${Tables.EPISODES_SEARCH} WHERE ${Tables.EPISODES_SEARCH} MATCH ?"

        private const val SELECT_EPISODES: String =
            "SELECT $EPISODE_COLUMNS,${SgShow2Columns.REF_SHOW_ID} FROM ${Tables.SG_EPISODE}"

        private const val JOIN_MATCHES_EPISODES: String =
            "SELECT $EPISODE_COLUMNS,$OVERVIEW,${SgShow2Columns.REF_SHOW_ID}" +
                    " FROM ($SELECT_MATCH) JOIN ($SELECT_EPISODES) ON $DOCID=$_ID"

        const val QUERY_SEARCH_EPISODES: String =
            "SELECT $EPISODE_COLUMNS,$OVERVIEW,${SgShow2Columns.TITLE},${SgShow2Columns.POSTER_SMALL}" +
                    " FROM (($SELECT_SHOWS) JOIN ($JOIN_MATCHES_EPISODES) ON sid=${SgShow2Columns.REF_SHOW_ID})"
    }
}

data class SgEpisode2WithShow(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = TITLE) val episodetitle: String?,
    @ColumnInfo(name = NUMBER) val episodenumber: Int,
    @ColumnInfo(name = SEASON) val season: Int,
    @ColumnInfo(name = FIRSTAIREDMS) val episode_firstairedms: Long,
    @ColumnInfo(name = WATCHED) val watched: Int,
    @ColumnInfo(name = COLLECTED) val episode_collected: Boolean,
    @ColumnInfo(name = OVERVIEW) val overview: String?,

    @ColumnInfo(name = SgShow2Columns.TITLE) val seriestitle: String,
    @ColumnInfo(name = SgShow2Columns.NETWORK) val network: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val series_poster_small: String?
) {
    companion object {
        // WAIT, make sure to update the above dummy query so there is compile time validation!
        const val SELECT =
            "SELECT sg_episode._id, episode_title, episode_number, episode_season_number, episode_firstairedms, episode_watched, episode_collected, episode_description, series_title, series_network, series_poster_small FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id=sg_show._id"

        private const val CALENDAR_DAY_LIMIT_MS = 31 * DateUtils.DAY_IN_MILLIS

        /**
         * For use with [SgEpisode2Helper.getEpisodesWithShowDataSource].
         */
        fun buildEpisodesWithShowQuery(
            context: Context,
            isUpcomingElseRecent: Boolean,
            isInfiniteCalendar: Boolean,
            isOnlyFavorites: Boolean,
            isOnlyUnwatched: Boolean,
            isOnlyCollected: Boolean,
            isOnlyPremieres: Boolean
        ): String {
            // go an hour back in time, so episodes move to recent one hour late
            val recentThreshold = TimeTools.getCurrentTime(context) - DateUtils.HOUR_IN_MILLIS

            val query: StringBuilder
            val sortOrder: String
            if (isUpcomingElseRecent) {
                // UPCOMING
                val timeThreshold = if (isInfiniteCalendar) {
                    // Include all future episodes.
                    Long.MAX_VALUE
                } else {
                    // Only episodes from the next few days.
                    System.currentTimeMillis() + CALENDAR_DAY_LIMIT_MS
                }
                query = StringBuilder(
                    "$FIRSTAIREDMS>=$recentThreshold " +
                            "AND $FIRSTAIREDMS<$timeThreshold " +
                            "AND ${SgShow2Columns.SELECTION_NO_HIDDEN}"
                )
                sortOrder = SORT_UPCOMING
            } else {
                // RECENT
                val timeThreshold = if (isInfiniteCalendar) {
                    // Include all past episodes.
                    Long.MIN_VALUE
                } else {
                    // Only episodes from the last few days.
                    System.currentTimeMillis() - CALENDAR_DAY_LIMIT_MS
                }
                query =
                    StringBuilder(
                        "$SELECTION_HAS_RELEASE_DATE " +
                                "AND $FIRSTAIREDMS<$recentThreshold " +
                                "AND $FIRSTAIREDMS>$timeThreshold " +
                                "AND ${SgShow2Columns.SELECTION_NO_HIDDEN}"
                    )
                sortOrder = SORT_RECENT
            }

            // append only favorites selection if necessary
            if (isOnlyFavorites) {
                query.append(" AND ").append(SgShow2Columns.SELECTION_FAVORITES)
            }

            // append no specials selection if necessary
            if (DisplaySettings.isHidingSpecials(context)) {
                query.append(" AND ").append(SELECTION_NO_SPECIALS)
            }

            // append unwatched selection if necessary
            if (isOnlyUnwatched) {
                query.append(" AND ").append(SELECTION_UNWATCHED)
            }

            // only show collected episodes
            if (isOnlyCollected) {
                query.append(" AND ").append(SELECTION_COLLECTED)
            }

            // Only premieres (first episodes).
            if (isOnlyPremieres) {
                query.append(" AND ").append(SELECTION_ONLY_PREMIERES)
            }

            return "$SELECT WHERE $query ORDER BY $sortOrder "
        }
    }
}

data class SgEpisode2SearchResult(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = TITLE) val episodetitle: String?,
    @ColumnInfo(name = NUMBER) val episodenumber: Int,
    @ColumnInfo(name = SEASON) val season: Int,
    @ColumnInfo(name = WATCHED) val watched: Int,
    @ColumnInfo(name = OVERVIEW) val overview: String?,

    @ColumnInfo(name = SgShow2Columns.TITLE) val seriestitle: String,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val series_poster_small: String?
)

data class SgEpisode2Info(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = SgSeason2Columns.REF_SEASON_ID) val seasonId: Long,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = TVDB_ID) val episodeTvdbId: Int,
    @ColumnInfo(name = TITLE) val title: String,
    @ColumnInfo(name = NUMBER) val episodenumber: Int,
    @ColumnInfo(name = ABSOLUTE_NUMBER) val absoluteNumber: Int,
    @ColumnInfo(name = SEASON) val season: Int,
    @ColumnInfo(name = DVDNUMBER) val dvdNumber: Double,
    @ColumnInfo(name = WATCHED) val watched: Int,
    @ColumnInfo(name = PLAYS) val plays: Int,
    @ColumnInfo(name = COLLECTED) val collected: Boolean = false,
    @ColumnInfo(name = FIRSTAIREDMS) val firstReleasedMs: Long
) {
    companion object {

        /**
         * Compile time validated using copy at [SgEpisode2Helper.dummyToValidateSgEpisode2Info].
         */
        fun buildQuery(seasonId: Long, order: EpisodesSettings.EpisodeSorting): SimpleSQLiteQuery {
            val orderClause = order.query()
            return SimpleSQLiteQuery(
                "SELECT _id, season_id, series_id, episode_tvdb_id, episode_title, episode_number, episode_absolute_number, episode_season_number, episode_dvd_number, episode_firstairedms, episode_watched, episode_plays, episode_collected FROM sg_episode WHERE season_id = $seasonId ORDER BY $orderClause"
            )
        }
    }
}

data class SgEpisode2Ids(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = NUMBER) val episodenumber: Int
)

data class SgEpisode2Numbers(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = TMDB_ID) val tmdbId: Int?,
    @ColumnInfo(name = SgSeason2Columns.REF_SEASON_ID) val seasonId: Long,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = NUMBER) val episodenumber: Int,
    @ColumnInfo(name = SEASON) val season: Int,
    @ColumnInfo(name = PLAYS) val plays: Int
) {
    companion object {

        /**
         * Compile time validated using copy at [SgEpisode2Helper.getEpisodeNumbersOfSeason].
         */
        fun buildQuery(seasonId: Long, order: EpisodesSettings.EpisodeSorting): SimpleSQLiteQuery {
            val orderClause = order.query()
            return SimpleSQLiteQuery(
                "SELECT _id, episode_tmdb_id, season_id, series_id, episode_number, episode_season_number, episode_plays FROM sg_episode WHERE season_id = $seasonId ORDER BY $orderClause"
            )
        }
    }
}

data class SgEpisode2ForSync(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = NUMBER) val number: Int,
    @ColumnInfo(name = SEASON) val season: Int,
    @ColumnInfo(name = WATCHED) val watched: Int,
    @ColumnInfo(name = PLAYS) val plays: Int,
    @ColumnInfo(name = COLLECTED) val collected: Boolean
)

/** See [SgEpisode2]. */
data class SgEpisode2Update(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = TMDB_ID) val tmdbId: Int,
    @ColumnInfo(name = TITLE) val title: String?,
    @ColumnInfo(name = OVERVIEW) val overview: String?,
    @ColumnInfo(name = NUMBER) val number: Int,
    @ColumnInfo(name = SEASON) val season: Int,
    @ColumnInfo(name = ORDER) val order: Int,
    @ColumnInfo(name = DIRECTORS) val directors: String?,
    @ColumnInfo(name = GUESTSTARS) val guestStars: String?,
    @ColumnInfo(name = WRITERS) val writers: String?,
    @ColumnInfo(name = IMAGE) val image: String?,
    @ColumnInfo(name = FIRSTAIREDMS) val firstReleasedMs: Long,
    @ColumnInfo(name = RATING_TMDB) val ratingTmdb: Double?,
    @ColumnInfo(name = RATING_TMDB_VOTES) val ratingTmdbVotes: Int?,
)

data class SgEpisode2WatchedUpdate(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = WATCHED) val watched: Int,
    @ColumnInfo(name = PLAYS) val plays: Int,
)

data class SgEpisode2CollectedUpdate(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = COLLECTED) val collected: Boolean
)

data class SgEpisode2TmdbIdUpdate(
    @ColumnInfo(name = _ID) val id: Long,
    @ColumnInfo(name = TMDB_ID) val tmdbId: Int
)

data class SgEpisode2WatchedUpdateByNumber(
    val showId: Long,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val watched: Int,
    val plays: Int
)

data class SgEpisode2CollectedUpdateByNumber(
    val showId: Long,
    val episodeNumber: Int,
    val seasonNumber: Int,
    val collected: Boolean
)
