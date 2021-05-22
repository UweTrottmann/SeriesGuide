package com.battlelancer.seriesguide.ui.lists

import android.content.Context
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.model.SgListItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.Errors
import com.google.api.client.http.HttpResponseException
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

                // Note: do not re-insert items with empty list ID (e.g. user modified database);
                // also do not remove and re-insert to Cloud, would fail.
                val oldListId = oldItem.listId
                if (!oldListId.isNullOrEmpty()) {
                    val newListItem = SgListItem(tmdbIdOrZero, ListItemTypes.TMDB_SHOW, oldListId)
                    toInsert.add(newListItem)

                    // Cloud changes
                    toRemoveCloud.add(SgCloudList().apply {
                        listId = oldListId
                        listItems = listOf(SgCloudListItem().apply {
                            listItemId = oldItem.listItemId
                        })
                    })
                    toInsertCloud.add(SgCloudList().apply {
                        listId = oldListId
                        listItems = listOf(SgCloudListItem().apply {
                            listItemId = newListItem.listItemId
                        })
                    })
                }
            }
        }

        Timber.d("Migrating %d list items to TMDB IDs", toRemove.size)

        // For cloud need list ID and item ID
        val requiresCloudUpdate = toRemoveCloud.isNotEmpty() || toInsertCloud.isNotEmpty()
        if (requiresCloudUpdate && HexagonSettings.isEnabled(context)) {
            val listsService = SgApp.getServicesComponent(context).hexagonTools().listsService
            if (listsService == null) {
                Timber.e("Cloud not signed in")
                return
            }
            try {
                if (toRemoveCloud.isNotEmpty()) {
                    listsService.removeItems(SgListList().apply {
                        lists = toRemoveCloud
                    }).execute()
                }
                if (toInsertCloud.isNotEmpty()) {
                    listsService.save(SgListList().apply {
                        lists = toInsertCloud
                    }).execute()
                }
            } catch (e: IOException) {
                Errors.logAndReportHexagon("migrate list items", e)
                if (e is HttpResponseException && e.statusCode == 400) {
                    // Bad Request, do not try again, but require a full lists sync.
                    // Note: had reports on Cloud where save failed due to not yet saved lists.
                    if (!HexagonSettings.setListsNotMerged(context)) {
                        // Failed to save, try again next sync
                        return
                    }
                } else {
                    // Abort and try again next sync
                    return
                }
            }
        }

        // Apply database updates
        if (toRemove.isNotEmpty()) database.sgListHelper().deleteListItems(toRemove)
        if (toInsert.isNotEmpty()) database.sgListHelper().insertListItems(toInsert)
    }

}