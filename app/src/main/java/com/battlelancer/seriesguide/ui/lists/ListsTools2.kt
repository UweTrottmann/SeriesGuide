package com.battlelancer.seriesguide.ui.lists

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.model.SgListItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Errors
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList
import timber.log.Timber
import java.io.IOException
import com.uwetrottmann.seriesguide.backend.lists.model.SgList as SgCloudList
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem as SgCloudListItem

object ListsTools2 {

    @JvmStatic
    fun migrateTvdbShowListItemsToTmdbIds(context: Context) {
        val database = SgRoomDatabase.getInstance(context)

        val tvdbShowListItems = database.sgListHelper().getTvdbShowListItems()
        if (tvdbShowListItems.isEmpty()) return

        // try to find TMDB ID for each show
        val toRemove = mutableListOf<String>()
        val toInsert = mutableListOf<SgListItem>()
        val toRemoveCloud = mutableListOf<SgCloudList>()
        val toInsertCloud = mutableListOf<SgCloudList>()
        tvdbShowListItems.forEach { oldItem ->
            val tvdbId = oldItem.itemRefId.toInt()
            val tmdbIdOrZero = database.sgShow2Helper().getShowTmdbIdByTvdbId(tvdbId)
            if (tmdbIdOrZero != 0) {
                // Database changes
                toRemove.add(oldItem.listItemId)
                val newListItem = SgListItem(tmdbIdOrZero, ListItemTypes.TMDB_SHOW, oldItem.listId)
                toInsert.add(newListItem)

                // Cloud changes
                toRemoveCloud.add(SgCloudList().apply {
                    listId = oldItem.listId
                    listItems = listOf(SgCloudListItem().apply {
                        listItemId = oldItem.listItemId
                    })
                })
                toInsertCloud.add(SgCloudList().apply {
                    listId = oldItem.listId
                    listItems = listOf(SgCloudListItem().apply {
                        listItemId = newListItem.listItemId
                    })
                })
            }
        }

        // Only need to check one, all sized equally
        if (toRemove.isEmpty()) return

        Timber.d("Migrating %d list items to TMDB IDs", toRemove.size)

        // For cloud need list ID and item ID
        if (HexagonSettings.isEnabled(context)) {
            val listsService = SgApp.getServicesComponent(context).hexagonTools().listsService
            if (listsService == null) {
                Timber.e("Cloud not signed in")
                return
            }
            try {
                listsService.removeItems(SgListList().apply {
                    lists = toRemoveCloud
                }).execute()
                listsService.save(SgListList().apply {
                    lists = toInsertCloud
                }).execute()
            } catch (e: IOException) {
                Errors.logAndReportHexagon("migrate list items", e)
                return
            }
        }

        // Apply database updates
        database.sgListHelper().deleteListItems(toRemove)
        database.sgListHelper().insertListItems(toInsert)
    }

}