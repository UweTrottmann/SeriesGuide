// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2021 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.lists.database

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.lists.database.SgListItemWithDetails.Companion.LIST_ITEMS_WITH_DETAILS
import com.battlelancer.seriesguide.movies.database.SgMovie
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.MoviesColumns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgEpisode2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgSeason2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Qualified
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.shows.tools.ShowStatus
import kotlinx.coroutines.flow.Flow

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

    @RawQuery(observedEntities = [SgListItem::class, SgShow2::class, SgMovie::class])
    fun getListItemsWithDetails(query: SupportSQLiteQuery): Flow<List<SgListItemWithDetails>>

    @Query("SELECT * FROM listitems WHERE list_id = :listId")
    fun getListItemsForExport(listId: String): List<SgListItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertList(list: SgList)

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

    @Query("DELETE FROM lists")
    fun deleteAllLists()

    @Query("DELETE FROM listitems")
    fun deleteAllListItems()
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
    /**
     * Only if [type] is [ListItemTypes.TMDB_MOVIE].
     */
    @ColumnInfo(name = MoviesColumns.TMDB_ID) val movieTmdbId: Int?,
    /**
     * Only if [type] is **not** a [ListItemTypes.TMDB_MOVIE].
     */
    @ColumnInfo(name = SgShow2Columns.REF_SHOW_ID) val showId: Long?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIME) val releaseTime: Int?,
    @ColumnInfo(name = RUNNING_TIME_MINUTES) val runningTimeMinutes: Int?,
    @ColumnInfo(name = SgShow2Columns.NEXTTEXT) val nextText: String?,
    /**
     * Get via [releasedMsOrDefault].
     */
    @ColumnInfo(name = RELEASED_MS) val releasedMs: Long?,
    @ColumnInfo(name = TITLE) val title: String,
    @ColumnInfo(name = TITLE_NO_ARTICLE) val titleNoArticle: String?,
    /**
     * [MoviesColumns.POSTER] or [SgShow2Columns.POSTER_SMALL].
     */
    @ColumnInfo(name = POSTER) val poster: String?,
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

    /**
     * If [releasedMs] is not null returns it, otherwise [Long.MAX_VALUE].
     * This works for both shows and movies as that's the default for both
     * ([SgMovie.RELEASED_MS_UNKNOWN] and
     * [com.battlelancer.seriesguide.shows.tools.NextEpisodeUpdater.UNKNOWN_NEXT_RELEASE_DATE]), see
     * [MoviesColumns.RELEASED_UTC_MS] and [SgShow2Columns.NEXTAIRDATEMS].
     */
    val releasedMsOrDefault: Long
        get() = releasedMs ?: Long.MAX_VALUE
    val runningTimeMinutesOrZero: Int
        get() = runningTimeMinutes ?: 0
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

        private const val TITLE = "list_item_title"
        private const val TITLE_NO_ARTICLE = "list_item_title_no_article"
        private const val POSTER = "list_item_poster"
        private const val RELEASED_MS = "list_item_released_ms"
        private const val RUNNING_TIME_MINUTES = "list_item_running_time_minutes"
        private const val ITEM_ROW_ID = "item_row_id"

        private const val ITEMS_COLUMNS: String =
            "${ListItems.LIST_ITEM_ID},${Lists.LIST_ID},${ListItems.TYPE},${ListItems.ITEM_REF_ID}"

        private const val SELECT_LIST_ITEMS_MAP_ROW_ID: String =
            "SELECT ${ListItems._ID} AS $ITEM_ROW_ID,$ITEMS_COLUMNS " +
                    "FROM ${Tables.LIST_ITEMS}"

        private const val SELECT_TMDB_MOVIES: String =
            "($SELECT_LIST_ITEMS_MAP_ROW_ID " +
                    "WHERE ${ListItems.TYPE}=${ListItemTypes.TMDB_MOVIE}) " +
                    "AS ${Tables.LIST_ITEMS}"

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

        /**
         * Show columns are mapped to `NULL`.
         */
        private const val SELECT_ITEMS_AND_MOVIES_COLUMNS: String =
            "SELECT $ITEM_ROW_ID AS " + ListItems._ID + "," +
                    ITEMS_COLUMNS + "," +
                    MoviesColumns.TMDB_ID + "," +
                    "NULL AS ${SgShow2Columns.REF_SHOW_ID}," +
                    "NULL AS ${SgShow2Columns.RELEASE_TIME}," +
                    "NULL AS ${SgShow2Columns.NEXTTEXT}," +
                    "${MoviesColumns.RELEASED_UTC_MS} AS $RELEASED_MS," +
                    "${MoviesColumns.RUNTIME_MIN} AS $RUNNING_TIME_MINUTES," +
                    "${MoviesColumns.TITLE} AS $TITLE," +
                    "${MoviesColumns.TITLE_NOARTICLE} AS $TITLE_NO_ARTICLE," +
                    "${MoviesColumns.POSTER} AS $POSTER," +
                    "NULL AS ${SgShow2Columns.NETWORK}," +
                    "NULL AS ${SgShow2Columns.STATUS}," +
                    "NULL AS ${SgShow2Columns.NEXTEPISODE}," +
                    "NULL AS ${SgShow2Columns.FAVORITE}," +
                    "NULL AS ${SgShow2Columns.RELEASE_WEEKDAY}," +
                    "NULL AS ${SgShow2Columns.RELEASE_TIMEZONE}," +
                    "NULL AS ${SgShow2Columns.RELEASE_COUNTRY}," +
                    "NULL AS ${SgShow2Columns.CUSTOM_RELEASE_TIME}," +
                    "NULL AS ${SgShow2Columns.CUSTOM_RELEASE_DAY_OFFSET}," +
                    "NULL AS ${SgShow2Columns.CUSTOM_RELEASE_TIME_ZONE}," +
                    "NULL AS ${SgShow2Columns.LASTWATCHED_MS}," +
                    "NULL AS ${SgShow2Columns.UNWATCHED_COUNT}"

        /**
         * Movie columns are mapped to `NULL`.
         */
        private const val SELECT_ITEMS_AND_SHOWS_COLUMNS: String =
            "SELECT $ITEM_ROW_ID AS " + ListItems._ID + "," +
                    ITEMS_COLUMNS + "," +
                    "NULL AS ${MoviesColumns.TMDB_ID}," +
                    "${Qualified.SG_SHOW_ID} AS ${SgShow2Columns.REF_SHOW_ID}," +
                    SgShow2Columns.RELEASE_TIME + "," +
                    SgShow2Columns.NEXTTEXT + "," +
                    "${SgShow2Columns.NEXTAIRDATEMS} AS $RELEASED_MS," +
                    "${SgShow2Columns.RUNTIME} AS $RUNNING_TIME_MINUTES," +
                    "${SgShow2Columns.TITLE} AS $TITLE," +
                    "${SgShow2Columns.TITLE_NOARTICLE} AS $TITLE_NO_ARTICLE," +
                    "${SgShow2Columns.POSTER_SMALL} AS $POSTER," +
                    SgShow2Columns.NETWORK + "," +
                    SgShow2Columns.STATUS + "," +
                    SgShow2Columns.NEXTEPISODE + "," +
                    SgShow2Columns.FAVORITE + "," +
                    SgShow2Columns.RELEASE_WEEKDAY + "," +
                    SgShow2Columns.RELEASE_TIMEZONE + "," +
                    SgShow2Columns.RELEASE_COUNTRY + "," +
                    SgShow2Columns.CUSTOM_RELEASE_TIME + "," +
                    SgShow2Columns.CUSTOM_RELEASE_DAY_OFFSET + "," +
                    SgShow2Columns.CUSTOM_RELEASE_TIME_ZONE + "," +
                    SgShow2Columns.LASTWATCHED_MS + "," +
                    SgShow2Columns.UNWATCHED_COUNT

        private const val SG_SEASON_JOIN_SG_SHOW: String =
            "${Tables.SG_SEASON} LEFT OUTER JOIN ${Tables.SG_SHOW} " +
                    "ON ${Tables.SG_SEASON}.${SgShow2Columns.REF_SHOW_ID}=${Qualified.SG_SHOW_ID}"

        private const val SG_EPISODE_JOIN_SG_SHOW: String =
            "${Tables.SG_EPISODE} LEFT OUTER JOIN ${Tables.SG_SHOW} " +
                    "ON ${Tables.SG_EPISODE}.${SgShow2Columns.REF_SHOW_ID}=${Qualified.SG_SHOW_ID}"

        /**
         * The columns of the final rows must match [SgListItemWithDetails].
         *
         * Using left outer join so joined show or movie data will be null/0 if there is no match.
         * Can then use this to not display list items for shows or movies that aren't added to the
         * database.
         */
        private const val LIST_ITEMS_WITH_DETAILS: String = "(" +
                // TMDB movies
                SELECT_ITEMS_AND_MOVIES_COLUMNS + " FROM " +
                "(" +
                SELECT_TMDB_MOVIES +
                " LEFT OUTER JOIN " + Tables.MOVIES +
                " ON " + Qualified.LIST_ITEMS_REF_ID + "=" + MoviesColumns.TMDB_ID +
                ")" +
                " UNION " +
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

        // Android provides the UNICODE collator,
        // use it to correctly order characters with, for example, accents.
        // https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase
        const val SORT_TITLE: String = "$TITLE COLLATE UNICODE ASC"
        const val SORT_TITLE_NO_ARTICLE: String = "$TITLE_NO_ARTICLE COLLATE UNICODE ASC"

        /**
         * By oldest movie or next episode release date, then continued first (for no next episode).
         */
        const val SORT_OLDEST_RELEASE_DATE =
            "$RELEASED_MS ASC,${SgShow2Columns.STATUS} DESC"

        /**
         * By latest movie or next episode release date, then continued first (for no next episode).
         */
        const val SORT_LATEST_RELEASE_DATE =
            "$RELEASED_MS DESC,${SgShow2Columns.STATUS} DESC"

        /**
         * By latest watched first.
         */
        const val SORT_LAST_WATCHED = "${SgShow2Columns.LASTWATCHED_MS} DESC"

        /**
         * By least episodes remaining to watch, then continued first (for no remaining episode).
         */
        const val SORT_REMAINING_EPISODES =
            "${SgShow2Columns.UNWATCHED_COUNT} ASC,${SgShow2Columns.STATUS} DESC"

        const val SORT_TYPE: String = ListItems.TYPE + " ASC"

        /**
         * Selects items of this list, but excludes any where the associated movie or show is not in
         * the database (the join on movie or show data will fail and the movie TMDB ID or show ID
         * will be 0/null).
         *
         * [orderClause] as built by [com.battlelancer.seriesguide.lists.ListsDistillationSettings].
         */
        fun buildSelect(orderClause: String): String = "SELECT * FROM $LIST_ITEMS_WITH_DETAILS" +
                " WHERE ${Lists.LIST_ID}=? AND (${MoviesColumns.TMDB_ID}>0 OR ${SgShow2Columns.REF_SHOW_ID}>0)" +
                " ORDER BY $orderClause"
    }
}
