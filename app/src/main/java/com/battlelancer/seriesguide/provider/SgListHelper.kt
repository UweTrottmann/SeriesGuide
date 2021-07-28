package com.battlelancer.seriesguide.provider

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.battlelancer.seriesguide.model.SgList
import com.battlelancer.seriesguide.model.SgListItem
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.ui.shows.ShowTools

@Dao
interface SgListHelper {

    @Query("SELECT * FROM lists ORDER BY ${Lists.SORT_ORDER_THEN_NAME}")
    fun getListsForDisplay(): LiveData<List<SgList>>

    @Query("SELECT * FROM lists ORDER BY ${Lists.SORT_ORDER_THEN_NAME}")
    fun getListsForExport(): List<SgList>

    @RawQuery(observedEntities = [SgListItem::class, SgShow2::class])
    fun getListItemsWithDetails(query: SupportSQLiteQuery): LiveData<List<SgListItemWithDetails>>

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

}

// Compare with SeriesGuideDatabase.Tables.LIST_ITEMS_WITH_DETAILS
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
    @ColumnInfo(name = SgShow2Columns.FAVORITE) var favorite: Boolean,
    @ColumnInfo(name = SgShow2Columns.RELEASE_WEEKDAY) val releaseWeekDay: Int?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_TIMEZONE) val releaseTimeZone: String?,
    @ColumnInfo(name = SgShow2Columns.RELEASE_COUNTRY) val releaseCountry: String?,
    @ColumnInfo(name = SgShow2Columns.LASTWATCHED_MS) val lastWatchedMs: Long,
    @ColumnInfo(name = SgShow2Columns.UNWATCHED_COUNT) val unwatchedCount: Int,
    ) {

    val releaseTimeOrDefault: Int
        get() = releaseTime ?: -1
    val releaseWeekDayOrDefault: Int
        get() = releaseWeekDay ?: -1
    val statusOrUnknown: Int
        get() = status ?: ShowTools.Status.UNKNOWN

    companion object {
        const val SELECT = "SELECT ${ListItems._ID}," +
                "${ListItems.LIST_ITEM_ID}," +
                "${ListItems.ITEM_REF_ID}," +
                "${ListItems.TYPE}," +
                "${SgShow2Columns.REF_SHOW_ID}," +
                "${SgShow2Columns.TITLE}," +
                "${SgShow2Columns.POSTER_SMALL}," +
                "${SgShow2Columns.NETWORK}," +
                "${SgShow2Columns.RELEASE_TIME}," +
                "${SgShow2Columns.RELEASE_WEEKDAY}," +
                "${SgShow2Columns.RELEASE_TIMEZONE}," +
                "${SgShow2Columns.RELEASE_COUNTRY}," +
                "${SgShow2Columns.STATUS}," +
                "${SgShow2Columns.NEXTTEXT}," +
                "${SgShow2Columns.NEXTAIRDATEMS}," +
                "${SgShow2Columns.FAVORITE}," +
                SgShow2Columns.UNWATCHED_COUNT
    }
}
