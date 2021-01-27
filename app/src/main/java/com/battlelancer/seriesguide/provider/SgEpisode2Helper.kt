package com.battlelancer.seriesguide.provider

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.Constants
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags
import com.battlelancer.seriesguide.util.TimeTools

@Dao
interface SgEpisode2Helper {

    @Query("SELECT _id FROM sg_episode WHERE episode_tvdb_id=:tvdbId")
    fun getEpisodeId(tvdbId: Int): Long

    @Query("SELECT _id, season_id, series_id, episode_tvdb_id, episode_title, episode_number, episode_absolute_number, episode_season_number, episode_dvd_number, episode_firstairedms, episode_watched, episode_collected FROM sg_episode WHERE _id = :episodeId")
    fun getEpisodeInfo(episodeId: Long): SgEpisode2Info?

    @Query("SELECT * FROM sg_episode WHERE _id=:id")
    fun getEpisodeLiveData(id: Long): LiveData<SgEpisode2?>

    @Query("SELECT * FROM sg_episode WHERE episode_tvdb_id=:tvdbId")
    fun getEpisodeLiveData(tvdbId: Int): LiveData<SgEpisode2?>

    /**
     * WAIT, just used for compile time validation of [SgEpisode2WithShow.SELECT].
     */
    @Query("SELECT sg_episode._id, episode_tvdb_id, episode_title, episode_number, episode_season_number, episode_firstairedms, episode_watched, episode_collected, series_tvdb_id, series_title, series_network, series_poster_small FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id=sg_show._id")
    fun dummyForValidationOfSgEpisode2WithShow(): SgEpisode2WithShow

    /**
     * See [SgEpisode2WithShow.buildEpisodesWithShowQuery].
     */
    @RawQuery(observedEntities = [SgEpisode2::class, SgShow2::class])
    fun getEpisodesWithShow(query: SupportSQLiteQuery): List<SgEpisode2WithShow>

    /**
     * See [SgEpisode2WithShow.buildEpisodesWithShowQuery].
     */
    @RawQuery(observedEntities = [SgEpisode2::class, SgShow2::class])
    fun getEpisodesWithShowDataSource(query: SupportSQLiteQuery): DataSource.Factory<Int, SgEpisode2WithShow>

    /**
     * WAIT, just for compile time validation of [SgEpisode2Info.SELECT]
     */
    @Query("SELECT _id, season_id, series_id, episode_tvdb_id, episode_title, episode_number, episode_absolute_number, episode_season_number, episode_dvd_number, episode_firstairedms, episode_watched, episode_collected FROM sg_episode WHERE season_id = :seasonId")
    fun dummyToValidateSgEpisode2Info(seasonId: Long): List<SgEpisode2Info>

    @RawQuery(observedEntities = [SgEpisode2::class])
    fun getEpisodeInfoOfSeason(query: SupportSQLiteQuery): List<SgEpisode2Info>

    @RawQuery(observedEntities = [SgEpisode2::class])
    fun getEpisodeInfoOfSeasonLiveData(query: SupportSQLiteQuery): LiveData<List<SgEpisode2Info>>

    /**
     * Returns how many episodes of a show are left to collect. Only considers regular, released
     * episodes (no specials, must have a release date in the past).
     */
    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_collected = 0 AND episode_season_number != 0 AND episode_firstairedms != ${Constants.EPISODE_UNKNOWN_RELEASE} AND episode_firstairedms <= :currentTimeToolsTime")
    fun countNotCollectedEpisodesOfShow(showId: Long, currentTimeToolsTime: Long): Int

    /**
     * Returns how many episodes of a show are left to watch (only aired and not watched, exclusive
     * episodes with no air date and without specials).
     */
    @Query("SELECT COUNT(_id) FROM sg_episode WHERE series_id = :showId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_season_number != 0 AND episode_firstairedms != ${Constants.EPISODE_UNKNOWN_RELEASE} AND episode_firstairedms <= :currentTimeToolsTime")
    fun countNotWatchedEpisodesOfShow(showId: Long, currentTimeToolsTime: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId")
    fun countEpisodesOfSeason(seasonId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_firstairedms != ${Constants.EPISODE_UNKNOWN_RELEASE} AND episode_firstairedms <= :currentTimeToolsTime")
    fun countNotWatchedReleasedEpisodesOfSeason(seasonId: Long, currentTimeToolsTime: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_firstairedms > :currentTimeToolsTime")
    fun countNotWatchedToBeReleasedEpisodesOfSeason(seasonId: Long, currentTimeToolsTime: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.UNWATCHED} AND episode_firstairedms = ${Constants.EPISODE_UNKNOWN_RELEASE}")
    fun countNotWatchedNoReleaseEpisodesOfSeason(seasonId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_watched = ${EpisodeFlags.SKIPPED}")
    fun countSkippedEpisodesOfSeason(seasonId: Long): Int

    @Query("SELECT COUNT(_id) FROM sg_episode WHERE season_id = :seasonId AND episode_collected = 0")
    fun countNotCollectedEpisodesOfSeason(seasonId: Long): Int

}

data class SgEpisode2WithShow(
    @ColumnInfo(name = SgEpisode2Columns._ID) val id: Long,
    @ColumnInfo(name = SgEpisode2Columns.TVDB_ID) val episodeTvdbId: Int,
    @ColumnInfo(name = SgEpisode2Columns.TITLE) val episodetitle: String?,
    @ColumnInfo(name = SgEpisode2Columns.NUMBER) val episodenumber: Int,
    @ColumnInfo(name = SgEpisode2Columns.SEASON) val season: Int,
    @ColumnInfo(name = SgEpisode2Columns.FIRSTAIREDMS) val episode_firstairedms: Long,
    @ColumnInfo(name = SgEpisode2Columns.WATCHED) val watched: Int,
    @ColumnInfo(name = SgEpisode2Columns.COLLECTED) val episode_collected: Boolean,

    @ColumnInfo(name = SgShow2Columns.TVDB_ID) val showTvdbId: Int,
    @ColumnInfo(name = SgShow2Columns.TITLE) val seriestitle: String,
    @ColumnInfo(name = SgShow2Columns.NETWORK) val network: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val series_poster_small: String?
) {
    companion object {
        // WAIT, make sure to update the above dummy query so there is compile time validation!
        const val SELECT =
            "SELECT sg_episode._id, episode_tvdb_id, episode_title, episode_number, episode_season_number, episode_firstairedms, episode_watched, episode_collected, series_tvdb_id, series_title, series_network, series_poster_small FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id=sg_show._id"

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
                query = StringBuilder("${SgEpisode2Columns.FIRSTAIREDMS}>=$recentThreshold " +
                        "AND ${SgEpisode2Columns.FIRSTAIREDMS}<$timeThreshold " +
                        "AND ${SgShow2Columns.SELECTION_NO_HIDDEN}")
                sortOrder = SgEpisode2Columns.SORT_UPCOMING
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
                    StringBuilder("${SgEpisode2Columns.SELECTION_HAS_RELEASE_DATE} " +
                            "AND ${SgEpisode2Columns.FIRSTAIREDMS}<$recentThreshold " +
                            "AND ${SgEpisode2Columns.FIRSTAIREDMS}>$timeThreshold " +
                            "AND ${SgShow2Columns.SELECTION_NO_HIDDEN}")
                sortOrder = SgEpisode2Columns.SORT_RECENT
            }

            // append only favorites selection if necessary
            if (isOnlyFavorites) {
                query.append(" AND ").append(SgShow2Columns.SELECTION_FAVORITES)
            }

            // append no specials selection if necessary
            if (DisplaySettings.isHidingSpecials(context)) {
                query.append(" AND ").append(SgEpisode2Columns.SELECTION_NO_SPECIALS)
            }

            // append unwatched selection if necessary
            if (isOnlyUnwatched) {
                query.append(" AND ").append(SgEpisode2Columns.SELECTION_UNWATCHED)
            }

            // only show collected episodes
            if (isOnlyCollected) {
                query.append(" AND ").append(SgEpisode2Columns.SELECTION_COLLECTED)
            }

            // Only premieres (first episodes).
            if (isOnlyPremieres) {
                query.append(" AND ").append(SgEpisode2Columns.SELECTION_ONLY_PREMIERES)
            }

            return "$SELECT WHERE $query ORDER BY $sortOrder "
        }
    }
}

data class SgEpisode2Info (
    @ColumnInfo(name = SgEpisode2Columns._ID) val id: Long,
    @ColumnInfo(name = SgSeason2Columns.REF_SEASON_ID) val seasonId: Long,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = SgEpisode2Columns.TVDB_ID) val episodeTvdbId: Int,
    @ColumnInfo(name = SgEpisode2Columns.TITLE) val title: String,
    @ColumnInfo(name = SgEpisode2Columns.NUMBER) val episodenumber: Int,
    @ColumnInfo(name = SgEpisode2Columns.ABSOLUTE_NUMBER) val absoluteNumber: Int,
    @ColumnInfo(name = SgEpisode2Columns.SEASON) val season: Int,
    @ColumnInfo(name = SgEpisode2Columns.DVDNUMBER) val dvdNumber: Double,
    @ColumnInfo(name = SgEpisode2Columns.WATCHED) val watched: Int,
    @ColumnInfo(name = SgEpisode2Columns.COLLECTED) val collected: Boolean = false,
    @ColumnInfo(name = SgEpisode2Columns.FIRSTAIREDMS) val firstReleasedMs: Long
) {
    companion object {

        /**
         * Compile time validated using copy at [SgEpisode2Helper.dummyToValidateSgEpisode2Info].
         */
        fun buildQuery(seasonId: Long, order: Constants.EpisodeSorting): SimpleSQLiteQuery {
            val orderClause = order.query()
            return SimpleSQLiteQuery(
                "SELECT _id, season_id, series_id, episode_tvdb_id, episode_title, episode_number, episode_absolute_number, episode_season_number, episode_dvd_number, episode_firstairedms, episode_watched, episode_collected FROM sg_episode WHERE season_id = $seasonId ORDER BY $orderClause"
            )
        }
    }
}
