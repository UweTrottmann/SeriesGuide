package com.battlelancer.seriesguide.provider

import android.content.Context
import android.text.format.DateUtils
import androidx.paging.DataSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.TimeTools

@Dao
interface SgEpisode2Helper {

    /**
     * WAIT, just used for compile time validation of [SgEpisode2WithShow.SELECT].
     */
    @Query("SELECT sg_episode._id, episode_tvdb_id, episode_title, episode_number, episode_season_number, episode_firstairedms, episode_watched, episode_collected, series_tvdb_id, series_title, series_network, series_poster_small FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id=sg_show._id")
    fun dummyForValidationOfSgEpisode2WithShow(): SgEpisode2WithShow

    /**
     * See [SgEpisode2WithShow.buildEpisodesWithShowQuery].
     */
    @RawQuery(observedEntities = [SgEpisode2::class, SgShow2::class])
    fun getEpisodesWithShowDataSource(query: SupportSQLiteQuery): DataSource.Factory<Int, SgEpisode2WithShow>

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
    @ColumnInfo(name = SgShow2Columns.TITLE) val seriestitle: String?,
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
