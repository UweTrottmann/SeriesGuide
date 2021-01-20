package com.battlelancer.seriesguide.provider

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns

@Dao
interface SgShow2Helper {

    @RawQuery(observedEntities = [SgShow2::class])
    fun getShows(query: SupportSQLiteQuery): List<SgShow2ForLists>

    @RawQuery(observedEntities = [SgShow2::class])
    fun getShowsLiveData(query: SupportSQLiteQuery): LiveData<List<SgShow2ForLists>>

}

data class SgShow2ForLists(
    @ColumnInfo(name = SgShow2Columns._ID) val id: Long,
    @ColumnInfo(name = SgShow2Columns.TVDB_ID) val tvdbId: Int?,
    @ColumnInfo(name = SgShow2Columns.TITLE) val title: String,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIME) val releaseTime: Int,
    @ColumnInfo(name = SgShow2Columns.RELEASE_WEEKDAY) val releaseWeekDay: Int,
    @ColumnInfo(name = SgShow2Columns.RELEASE_COUNTRY) val releaseCountry: String?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIMEZONE) val releaseTimeZone: String?,
    @ColumnInfo(name = SgShow2Columns.NETWORK) val network: String?,
    @ColumnInfo(name = SgShow2Columns.STATUS) val status: Int?,
    @ColumnInfo(name = SgShow2Columns.NEXTEPISODE) val nextEpisode: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER) val poster: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val posterSmall: String?,
    @ColumnInfo(name = SgShow2Columns.NEXTAIRDATEMS) val nextAirdateMs: Long,
    @ColumnInfo(name = SgShow2Columns.NEXTTEXT) val nextText: String,
    @ColumnInfo(name = SgShow2Columns.UNWATCHED_COUNT) val unwatchedCount: Int,
    @ColumnInfo(name = SgShow2Columns.FAVORITE) val favorite: Boolean,
    @ColumnInfo(name = SgShow2Columns.HIDDEN) val hidden: Boolean
)
