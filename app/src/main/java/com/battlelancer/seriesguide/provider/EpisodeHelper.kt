package com.battlelancer.seriesguide.provider

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.model.EpisodeWithShow
import com.battlelancer.seriesguide.model.SgEpisode
import com.battlelancer.seriesguide.model.SgEpisodeForTraktSync
import com.battlelancer.seriesguide.model.SgEpisodeSeasonAndShow
import com.battlelancer.seriesguide.model.SgShow
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes

/**
 * Data Access Object for the episodes table.
 */
@Dao
interface EpisodeHelper {
    /**
     * For testing: Get the first episode from the table.
     */
    @Query("SELECT * FROM episodes LIMIT 1")
    fun getEpisode(): SgEpisode?

    /**
     * For testing: Get single episode.
     */
    @Query("SELECT * FROM episodes WHERE _id=:episodeTvdbId")
    fun getEpisode(episodeTvdbId: Int): SgEpisode?

    /**
     * Gets episodes of season ordered by episode number.
     */
    @Query("SELECT * FROM episodes WHERE season_id=:seasonTvdbId ORDER BY episodenumber ASC")
    fun getSeason(seasonTvdbId: Int): List<SgEpisode>

    @Query("SELECT _id, episodenumber, watched, plays, episode_collected FROM episodes WHERE season_id=:seasonTvdbId")
    fun getSeasonForTraktSync(seasonTvdbId: Int): List<SgEpisodeForTraktSync>

    @Query("SELECT season_id, season, series_id  FROM episodes WHERE _id=:episodeTvdbId")
    fun getEpisodeMinimal(episodeTvdbId: Int): SgEpisodeSeasonAndShow?

    @RawQuery(observedEntities = [SgEpisode::class, SgShow::class])
    fun getEpisodesWithShow(query: SupportSQLiteQuery): DataSource.Factory<Int, EpisodeWithShow>

    @Query("UPDATE episodes SET watched = 0, plays = 0 WHERE _id=:episodeTvdbId")
    fun setNotWatchedAndRemovePlays(episodeTvdbId: Int): Int

    @Query("UPDATE episodes SET watched = 1, plays = plays + 1 WHERE _id=:episodeTvdbId")
    fun setWatchedAndAddPlay(episodeTvdbId: Int): Int

    @Query("UPDATE episodes SET watched = 2 WHERE _id=:episodeTvdbId")
    fun setSkipped(episodeTvdbId: Int): Int

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
        """UPDATE episodes SET watched = 1, plays = plays + 1 WHERE series_id=:showTvdbId
            AND (
            episode_firstairedms < :episodeFirstAired
            OR (episode_firstairedms = :episodeFirstAired AND episodenumber <= :episodeNumber)
            )
            AND ${Episodes.SELECTION_HAS_RELEASE_DATE}
            AND ${Episodes.SELECTION_UNWATCHED_OR_SKIPPED}"""
    )
    fun setWatchedUpToAndAddPlay(showTvdbId: Int, episodeFirstAired: Long, episodeNumber: Int): Int

    /**
     * Sets all watched or skipped as not watched and removes all plays.
     *
     * Note: keep in sync with SeasonWatchedJob.
     */
    @Query(
        """UPDATE episodes SET watched = 0, plays = 0 WHERE season_id=:seasonTvdbId
            AND ${Episodes.SELECTION_WATCHED_OR_SKIPPED}"""
    )
    fun setSeasonNotWatchedAndRemovePlays(seasonTvdbId: Int): Int

    /**
     * Sets not watched or skipped episodes, released until within the hour,
     * as watched and adds play.
     *
     * Does NOT mark watched episodes again to avoid adding a new play (Trakt and local).
     *
     * Note: keep in sync with SeasonWatchedJob.
     */
    @Query(
        """UPDATE episodes SET watched = 1, plays = plays + 1 WHERE season_id=:seasonTvdbId
            AND episode_firstairedms <= :currentTimePlusOneHour
            AND ${Episodes.SELECTION_HAS_RELEASE_DATE}
            AND ${Episodes.SELECTION_UNWATCHED_OR_SKIPPED}"""
    )
    fun setSeasonWatchedAndAddPlay(seasonTvdbId: Int, currentTimePlusOneHour: Long): Int

    /**
     * Sets not watched episodes, released until within the hour, as skipped.
     *
     * Note: keep in sync with SeasonWatchedJob.
     */
    @Query(
        """UPDATE episodes SET watched = 2 WHERE season_id=:seasonTvdbId
            AND episode_firstairedms <= :currentTimePlusOneHour
            AND ${Episodes.SELECTION_HAS_RELEASE_DATE}
            AND ${Episodes.SELECTION_UNWATCHED}"""
    )
    fun setSeasonSkipped(seasonTvdbId: Int, currentTimePlusOneHour: Long): Int

    /**
     * Sets watched or skipped episodes, excluding specials,
     * as not watched and removes all plays.
     *
     * Note: keep in sync with ShowWatchedJob.
     */
    @Query(
        """UPDATE episodes SET watched = 0, plays = 0 WHERE series_id=:showTvdbId
            AND ${Episodes.SELECTION_WATCHED_OR_SKIPPED}
            AND ${Episodes.SELECTION_NO_SPECIALS}"""
    )
    fun setShowNotWatchedAndRemovePlays(showTvdbId: Int): Int

    /**
     * Sets not watched or skipped episodes, released until within the hour, excluding specials,
     * as watched and adds play.
     *
     * Does NOT mark watched episodes again to avoid adding a new play (Trakt and local).
     *
     * Note: keep in sync with ShowWatchedJob.
     */
    @Query(
        """UPDATE episodes SET watched = 1, plays = plays + 1 WHERE series_id=:showTvdbId
            AND episode_firstairedms <= :currentTimePlusOneHour
            AND ${Episodes.SELECTION_HAS_RELEASE_DATE}
            AND ${Episodes.SELECTION_UNWATCHED_OR_SKIPPED}
            AND ${Episodes.SELECTION_NO_SPECIALS}"""
    )
    fun setShowWatchedAndAddPlay(showTvdbId: Int, currentTimePlusOneHour: Long): Int
}