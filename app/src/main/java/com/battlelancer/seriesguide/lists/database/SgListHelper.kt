// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists.database

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import kotlinx.coroutines.flow.Flow
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails.Companion.LIST_ITEMS_WITH_DETAILS
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Qualified
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.tools.ShowStatus

@Dao
interface SgListHelper {

    /**
     * Is null on error or if it does not exist.
     */
    @Query("SELECT * FROM lists WHERE list_id = :id")
    fun getList(id: String): SgList?

    @Query("SELECT * FROM lists ORDER BY ${Lists.SORT_ORDER_THEN_NAME}")
    fun getListsForDisplay(): LiveData<List<SgList>>

    @Query("SELECT * FROM lists ORDER BY ${Lists.SORT_ORDER_THEN_NAME}")
    fun getListsForExport(): List<SgList>

    /**
     * Is 0 on error.
     */
    @Query("SELECT COUNT(_id) FROM lists")
    fun getListsCount(): Int

    @Query("SELECT * FROM listitems WHERE item_ref_id = :tmdbId AND item_type = :type")
    fun getListItemsWithTmdbId(tmdbId: Int, @ListItemTypes type: Int): List<SgListItem>

    @RawQuery(observedEntities = [SgListItem::class, SgShow2::class])
    fun getListItemsWithDetails(query: SupportSQLiteQuery): Flow<List<SgListItemWithDetails>>

    @Query("SELECT * FROM listitems WHERE list_id = :listId")
    fun getListItemsForExport(listId: String): List<SgListItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertListItems(listItems: List<SgListItem>)

    @Query("SELECT * FROM listitems WHERE item_type = ${ListItemTypes.TVDB_SHOW}")
    fun getTvdbShowListItems(): List<SgListItem>

    @Query("DELETE FROM listitems WHERE list_item_id = :listItemId")
    fun deleteListItem(listItemId: String)

    @Transaction
    fun deleteListItems(listItemIds: List<String>) {
        listItemIds.forEach {
            deleteListItem(it)
        }
    }

    /**
     * By default the database inserts a first list when being created, d
     */
    @VisibleForTesting
    @Query("DELETE FROM lists")
    fun deleteAllLists()

}

/**
 * For query see [LIST_ITEMS_WITH_DETAILS].
 */
data class SgListItemWithDetails(
    @ColumnInfo(name = ListItems._ID) val id: Long,
    @ColumnInfo(name = ListItems.LIST_ITEM_ID) val listItemId: String,
    @ColumnInfo(name = Lists.LIST_ID) val listId: String,
    @ColumnInfo(name = ListItems.TYPE) val type: Int,
    @ColumnInfo(name = ListItems.ITEM_REF_ID) val itemRefId: String,
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIME) val releaseTime: Int?,
    @ColumnInfo(name = SgShow2Columns.NEXTTEXT) val nextText: String?,
    @ColumnInfo(name = SgShow2Columns.NEXTAIRDATEMS) val nextAirdateMs: Long,
    @ColumnInfo(name = SgShow2Columns.TITLE) val title: String,
    @ColumnInfo(name = SgShow2Columns.TITLE_NOARTICLE) val titleNoArticle: String?,
    @ColumnInfo(name = SgShow2Columns.POSTER_SMALL) val posterSmall: String?,
    @ColumnInfo(name = SgShow2Columns.NETWORK) val network: String?,
    @ColumnInfo(name = SgShow2Columns.STATUS) val status: Int?,
    @ColumnInfo(name = SgShow2Columns.NEXTEPISODE) val nextEpisode: String?,
    @ColumnInfo(name = SgShow2Columns.FAVORITE) var favorite: Boolean,
    @ColumnInfo(name = SgShow2Columns.RELEASE_WEEKDAY) val releaseWeekDay: Int?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIMEZONE) val releaseTimeZone: String?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_COUNTRY) val releaseCountry: String?,
    @ColumnInfo(name = SgShow2Columns.CUSTOM_RELEASE_TIME) var customReleaseTime: Int?,
    @ColumnInfo(name = SgShow2Columns.CUSTOM_RELEASE_DAY_OFFSET) var customReleaseDayOffset: Int?,
    @ColumnInfo(name = SgShow2Columns.CUSTOM_RELEASE_TIME_ZONE) var customReleaseTimeZone: String?,
    @ColumnInfo(name = SgShow2Columns.LASTWATCHED_MS) val lastWatchedMs: Long,
    @ColumnInfo(name = SgShow2Columns.UNWATCHED_COUNT) val unwatchedCount: Int,
) {

    val releaseTimeOrDefault: Int
        get() = releaseTime ?: -1
    val releaseWeekDayOrDefault: Int
        get() = releaseWeekDay ?: -1
    val customReleaseTimeOrDefault: Int
        get() = customReleaseTime ?: SgShow2.CUSTOM_RELEASE_TIME_NOT_SET
    val customReleaseDayOffsetOrDefault: Int
        get() = customReleaseDayOffset ?: SgShow2.CUSTOM_RELEASE_DAY_OFFSET_NOT_SET
    val statusOrUnknown: Int
        get() = status ?: ShowStatus.UNKNOWN

    val nextEpisodeId: Long
        get() = nextEpisode?.toLongOrNull() ?: 0

    companion object {

        private const val ITEMS_COLUMNS: String =
            "${ListItems.LIST_ITEM_ID},${Lists.LIST_ID},${ListItems.TYPE},${ListItems.ITEM_REF_ID}"

        private const val SELECT_LIST_ITEMS_MAP_ROW_ID: String =
            "SELECT ${ListItems._ID} AS item_row_id,$ITEMS_COLUMNS " +
                    "FROM ${Tables.LIST_ITEMS}"

        private const val SELECT_TMDB_SHOWS: String =
            "($SELECT_LIST_ITEMS_MAP_ROW_ID " +
                    "WHERE ${ListItems.TYPE}=${ListItemTypes.TMDB_SHOW}) " +
                    "AS ${Tables.LIST_ITEMS}"

        private const val SELECT_TVDB_SHOWS: String =
            "($SELECT_LIST_ITEMS_MAP_ROW_ID " +
                    "WHERE ${ListItems.TYPE}=${ListItemTypes.TVDB_SHOW}) " +
                    "AS ${Tables.LIST_ITEMS}"

        private const val SELECT_TVDB_SEASONS: String =
            "($SELECT_LIST_ITEMS_MAP_ROW_ID " +
                    "WHERE ${ListItems.TYPE}=${ListItemTypes.SEASON}) " +
                    "AS ${Tables.LIST_ITEMS}"

        private const val SELECT_TVDB_EPISODES: String =
            "($SELECT_LIST_ITEMS_MAP_ROW_ID " +
                    "WHERE ${ListItems.TYPE}=${ListItemTypes.EPISODE}) " +
                    "AS ${Tables.LIST_ITEMS}"

        private const val SELECT_ITEMS_AND_SHOWS_COLUMNS: String =
            ("SELECT item_row_id AS " + ListItems._ID + ","
                    + ITEMS_COLUMNS + ","
                    + Qualified.SG_SHOW_ID + " AS " + SgShow2Columns.REF_SHOW_ID + ","
                    + SgShow2Columns.RELEASE_TIME + ","
                    + SgShow2Columns.NEXTTEXT + ","
                    + SgShow2Columns.NEXTAIRDATEMS + ","
                    + SgShow2Columns.TITLE + ","
                    + SgShow2Columns.TITLE_NOARTICLE + ","
                    + SgShow2Columns.POSTER_SMALL + ","
                    + SgShow2Columns.NETWORK + ","
                    + SgShow2Columns.STATUS + ","
                    + SgShow2Columns.NEXTEPISODE + ","
                    + SgShow2Columns.FAVORITE + ","
                    + SgShow2Columns.RELEASE_WEEKDAY + ","
                    + SgShow2Columns.RELEASE_TIMEZONE + ","
                    + SgShow2Columns.RELEASE_COUNTRY + ","
                    + SgShow2Columns.CUSTOM_RELEASE_TIME + ","
                    + SgShow2Columns.CUSTOM_RELEASE_DAY_OFFSET + ","
                    + SgShow2Columns.CUSTOM_RELEASE_TIME_ZONE + ","
                    + SgShow2Columns.LASTWATCHED_MS + ","
                    + SgShow2Columns.UNWATCHED_COUNT)

        private const val SG_SEASON_JOIN_SG_SHOW: String =
            "${Tables.SG_SEASON} LEFT OUTER JOIN ${Tables.SG_SHOW} " +
                    "ON ${Tables.SG_SEASON}.${SgShow2Columns.REF_SHOW_ID}=${Qualified.SG_SHOW_ID}"

        private const val SG_EPISODE_JOIN_SG_SHOW: String =
            "${Tables.SG_EPISODE} LEFT OUTER JOIN ${Tables.SG_SHOW} " +
                    "ON ${Tables.SG_EPISODE}.${SgShow2Columns.REF_SHOW_ID}=${Qualified.SG_SHOW_ID}"

        /**
         * The columns of the final rows must match [SgListItemWithDetails].
         */
        private const val LIST_ITEMS_WITH_DETAILS: String = "(" +
                // new TMDB shows
                SELECT_ITEMS_AND_SHOWS_COLUMNS + " FROM " +
                "(" +
                SELECT_TMDB_SHOWS +
                " LEFT OUTER JOIN " + Tables.SG_SHOW +
                " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + SgShow2Columns.TMDB_ID +
                ")" +
                // legacy TVDB shows
                " UNION " +
                SELECT_ITEMS_AND_SHOWS_COLUMNS + " FROM " +
                "(" +
                SELECT_TVDB_SHOWS +
                " LEFT OUTER JOIN " + Tables.SG_SHOW +
                " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + SgShow2Columns.TVDB_ID +
                ")" +
                // legacy TVDB seasons
                " UNION " +
                SELECT_ITEMS_AND_SHOWS_COLUMNS + " FROM " +
                "(" +
                SELECT_TVDB_SEASONS +
                " LEFT OUTER JOIN " + "(" + SG_SEASON_JOIN_SG_SHOW + ") AS " + Tables.SG_SEASON +
                " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + SgSeason2Columns.TVDB_ID +
                ")" +
                // legacy TVDB episodes
                " UNION " +
                SELECT_ITEMS_AND_SHOWS_COLUMNS + " FROM " +
                "(" +
                SELECT_TVDB_EPISODES +
                " LEFT OUTER JOIN " + "(" + SG_EPISODE_JOIN_SG_SHOW + ") AS " + Tables.SG_EPISODE +
                " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + SgEpisode2Columns.TVDB_ID +
                ")" +
                //
                ")"

        const val SORT_TYPE: String = ListItems.TYPE + " ASC"

        /**
         * Selects items of this list, but exclude any if show was removed from the database
         * (the join on show data will fail, hence the show id will be 0/null).
         *
         * [orderClause] as built by [com.battlelancer.seriesguide.lists.ListsDistillationSettings].
         */
        fun buildSelect(orderClause: String): String = "SELECT * FROM $LIST_ITEMS_WITH_DETAILS" +
                " WHERE ${Lists.LIST_ID}=? AND ${SgShow2Columns.REF_SHOW_ID}>0" +
                " ORDER BY $orderClause"
    }
}
