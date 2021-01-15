package com.battlelancer.seriesguide.provider

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

@Dao
interface SgEpisode2Helper {

    /**
     * WAIT, just used for compile time validation of [SgEpisode2WithShow.SELECT].
     */
    @Query("SELECT sg_episode._id, episode_tvdb_id, episode_title, episode_number, episode_season_number, episode_firstairedms, episode_watched, episode_collected, series_tvdb_id, series_title, series_network, series_poster_small FROM sg_episode LEFT OUTER JOIN sg_show ON sg_episode.series_id=sg_show._id")
    fun dummyForValidationOfSgEpisode2WithShow(): SgEpisode2WithShow

    @RawQuery(observedEntities = [SgEpisode2::class, SgShow2::class])
    fun getEpisodesWithShow(query: SupportSQLiteQuery): DataSource.Factory<Int, SgEpisode2WithShow>

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
    }
}
