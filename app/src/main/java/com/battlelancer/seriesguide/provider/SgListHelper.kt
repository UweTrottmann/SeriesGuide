package com.battlelancer.seriesguide.provider

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.battlelancer.seriesguide.model.SgList
import com.battlelancer.seriesguide.model.SgListItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists

@Dao
interface SgListHelper {

    @Query("SELECT * FROM lists ORDER BY ${Lists.SORT_ORDER_THEN_NAME}")
    fun getListsForExport(): List<SgList>

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