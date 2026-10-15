// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import androidx.room.withTransaction
import com.battlelancer.seriesguide.lists.ListsTools
import com.battlelancer.seriesguide.lists.database.SgList
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.lists.database.SgListItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse.IsAccountLimitExceeded
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse.IsAccountLocked
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse.Success
import com.battlelancer.seriesguide.util.TimeTools
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.entities.SyncShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.threeten.bp.OffsetDateTime
import timber.log.Timber
import java.io.IOException

/**
 * Syncs custom lists with Trakt.
 *
 * This doesn't sync a user's "watchlist": shows on it are displayed by
 * [com.battlelancer.seriesguide.shows.search.discover.TraktAddFragment], movies on it are synced by
 * [TraktMovieSync].
 */
class TraktListsSync(
    private val traktSync: TraktSync,
    private val database: SgRoomDatabase = SgRoomDatabase.getInstance(traktSync.context),
    private val listHelper: SgListHelper = database.sgListHelper()
) {

    private val context = traktSync.context

    enum class InitialSync {
        EXISTING_LISTS_DELETE,
        EXISTING_LISTS_MERGE,
        NO_SYNC
    }

    fun sync(updatedAt: OffsetDateTime?): Boolean {
        if (updatedAt == null) {
            Timber.e("sync: null updatedAt")
            return false
        }

        val isInitialSync = TraktSettings.isInitialSyncLists(context)

        // Don't sync if there are no changes, unless this is the initial sync
        val lastUpdatedAt = TraktSettings.getLastListsUpdatedAt(context)
        if (!isInitialSync && !TimeTools.isAfterMillis(updatedAt, lastUpdatedAt)) {
            Timber.d("sync: no changes since %tF %tT", lastUpdatedAt, lastUpdatedAt)
            return true
        }

        // TODO Depending on user preference, delete existing lists or don't sync at all
        val initialSyncPreference: InitialSync = InitialSync.EXISTING_LISTS_DELETE
        if (isInitialSync) {
            if (initialSyncPreference == InitialSync.NO_SYNC) {
                Timber.d("initial sync: user chose not to sync lists")
                // Leave in initial sync not completed state if the user later decides to
                // turn on list sync.
                return true
            }
        }

        // Match local lists with what's on Trakt (not items)
        val traktLists = runBlocking(Dispatchers.Default) {
            when (val response = traktSync.traktTools.getLists(traktSync.users)) {
                is Success -> response.data
                is IsAccountLocked,
                is IsAccountLimitExceeded,
                is TraktErrorResponse.IsNotVip,
                is TraktErrorResponse.IsUnauthorized,
                is TraktErrorResponse.Other -> null
            }
        } ?: return false

        try {
            runBlocking(Dispatchers.Default) {
                database.withTransaction {
                    if (isInitialSync) {
                        if (initialSyncPreference == InitialSync.EXISTING_LISTS_DELETE) {
                            Timber.d("initial sync: user chose to delete existing lists")
                            listHelper.deleteAllListsAndItems()
                        } else if (initialSyncPreference == InitialSync.EXISTING_LISTS_MERGE) {
                            Timber.d("initial sync: user chose to merge with existing lists")
                        }
                    }

                    val databaseLists = listHelper.getListsForExport()

                    val databaseListIdsToKeep = mutableSetOf<String>()

                    traktLists.forEach { list ->
                        val traktId = list.ids?.trakt
                        val traktName = list.name
                        if (traktId == null || traktName.isNullOrBlank()) {
                            // Something must be wrong with the API, stop
                            Timber.e(
                                "Trakt list is missing required values (id=%s, name=%s)",
                                traktId,
                                traktName
                            )
                            throw IOException()
                        }

                        // FIXME Handle multiple Trakt lists with the same name
                        val databaseList = databaseLists.find {
                            // For an initial sync, also try to find a list with the same name.
                            it.traktId == traktId || (isInitialSync && it.name == traktName)
                        }
                        val databaseId = if (databaseList == null) {
                            // Add new list to database
                            // FIXME Handle collision with existing list
                            val generatedId = ListsTools.generateListId(traktName)
                            if (generatedId == null) {
                                Timber.e(
                                    "Generated list ID is null for list (id=%s, name=%s)",
                                    traktId,
                                    traktName
                                )
                                throw IOException()
                            }
                            listHelper.insertList(
                                SgList(
                                    listId = generatedId,
                                    name = traktName,
                                    traktId = traktId
                                )
                            )
                            generatedId
                        } else {
                            // Update existing list in database (the list ID can remain the same,
                            // even if the name changes, compare to RenameListTask).
                            databaseList.name = traktName
                            if (isInitialSync) databaseList.traktId = traktId
                            listHelper.updateList(databaseList)
                            databaseList.listId
                        }
                        databaseListIdsToKeep.add(databaseId)
                    }

                    if (!isInitialSync) {
                        // Delete lists not on Trakt
                        val databaseListIdsToDelete = databaseLists
                            .filter { it.listId !in databaseListIdsToKeep }
                            .map { it.listId }
                        databaseListIdsToDelete.forEach { listId ->
                            listHelper.deleteListAndItems(listId)
                        }
                    }
                }
            }
        } catch (_: IOException) {
            return false
        }

        // On initial sync, upload all lists to
        // - upload lists not on Trakt,
        // - upload items of existing lists not on Trakt.
        if (isInitialSync) {
            val success = runBlocking(Dispatchers.Default) {
                uploadAllLists()
            }
            if (!success) {
                return false // Try again next sync
            }
        }

        // Match local list items with what's on Trakt
        // TODO Re-fetch lists on initial sync or add those created by upload so not found items are
        //  removed immediately)
        traktLists.forEach {
            val success = runBlocking(Dispatchers.Default) {
                syncListItems(it.ids!!.trakt!!)
            }
            if (!success) {
                return false // Try again next sync
            }
        }

        if (isInitialSync) {
            TraktSettings.setInitialSyncListsCompleted(context)
        }
        TraktSettings.storeLastListsUpdatedAt(context, updatedAt)

        return true
    }

    /**
     * If the local list doesn't have a Trakt ID, creates a list on Trakt. Uploads all local items
     * to the list (Trakt doesn't overwrite existing items).
     *
     * Does not fail if items aren't found on Trakt, so they will be removed by the sync step.
     */
    private suspend fun uploadAllLists(): Boolean {
        listHelper.getListsForExport().forEach { list: SgList ->
            val listTraktIdOrNull = list.traktId
            val listTraktId: Long = if (listTraktIdOrNull == null) {
                // Create list on Trakt if needed
                val listTraktId = traktSync.awaitAndHandleUploadError {
                    traktSync.traktTools.createList(traktSync.users, list.name)
                }
                if (listTraktId == null) {
                    Timber.e(
                        "Failed to create Trakt list (name=%s)",
                        list.name
                    )
                    return false
                }
                listHelper.updateListTraktId(list.listId, listTraktId)
                listTraktId
            } else {
                // FIXME Handle lists that were deleted at Trakt (uploading items will fail)
                listTraktIdOrNull
            }

            // Upload list items
            val listItems = listHelper.getListItemsForExport(list.listId)
            val shows = listItems
                .filter { it.type == ListItemTypes.TMDB_SHOW }
                .map { SyncShow().id(ShowIds.tmdb(it.itemRefId.toInt())) }
            val movies = listItems
                .filter { it.type == ListItemTypes.TMDB_MOVIE }
                .map { SyncMovie().id(MovieIds.tmdb(it.itemRefId.toInt())) }
            if (shows.isEmpty() && movies.isEmpty()) {
                return@forEach // List doesn't have items supported by Trakt, continue
            }
            val syncItems = SyncItems()
                .shows(shows)
                .movies(movies)

            val syncResponse = traktSync.awaitAndHandleUploadError {
                traktSync.traktTools.addItemsToList(traktSync.users, listTraktId, syncItems)
            } ?: return false

            val notFoundMovies = syncResponse.not_found?.movies
            val notFoundShows = syncResponse.not_found?.shows
            if (!notFoundMovies.isNullOrEmpty() || !notFoundShows.isNullOrEmpty()) {
                notFoundMovies?.forEach {
                    Timber.e(
                        "Failed to add movie to list, not found at Trakt (list name: %s, movie TMDB ID: %s)",
                        list.name,
                        it.ids?.tmdb
                    )
                }
                notFoundShows?.forEach {
                    Timber.e(
                        "Failed to add show to list, not found at Trakt (list name: %s, show TMDB ID: %s)",
                        list.name,
                        it.ids?.tmdb
                    )
                }
                // Log, but still continue: the assumption is that the user understands that when
                // syncing with Trakt lists can only contain items in the Trakt database and other
                // items will be removed.
            }
        }

        return true
    }

    /**
     * Note: does not add or remove movies added to or removed from a custom list to or from the
     * database. This is done during a later sync step.
     */
    private suspend fun syncListItems(traktListId: Long): Boolean {
        // Do API call outside database transaction so it can complete quickly
        val traktListItems =
            when (val response = traktSync.traktTools.getListItems(traktSync.users, traktListId)) {
                is Success -> response.data
                is IsAccountLocked,
                is IsAccountLimitExceeded,
                is TraktErrorResponse.IsNotVip,
                is TraktErrorResponse.IsUnauthorized,
                is TraktErrorResponse.Other -> null
            } ?: return false

        try {
            database.withTransaction {
                // Check if the user didn't delete the list in the meantime
                val databaseListId = listHelper.getListIdForTraktId(traktListId)
                if (databaseListId == null) {
                    Timber.w("List with Trakt ID %s was deleted", traktListId)
                    throw IOException()
                }

                // Match database items with what is on Trakt
                data class ItemKey(val type: Int, val refId: String)

                val databaseListItems = listHelper.getListItemsForExport(databaseListId)
                val databaseItemsByKey = databaseListItems.associateBy {
                    ItemKey(it.type, it.itemRefId)
                }

                val traktItemKeys = mutableSetOf<ItemKey>()
                val itemsToInsert = mutableListOf<SgListItem>()

                traktListItems.forEach { traktListItem ->
                    // Map a Trakt list entry to an item type + TMDB ref id.
                    val showTmdbId = traktListItem.show?.ids?.tmdb
                    val movieTmdbId = traktListItem.movie?.ids?.tmdb
                    val (itemType, tmdbId) = when {
                        showTmdbId != null ->
                            ListItemTypes.TMDB_SHOW to showTmdbId

                        movieTmdbId != null ->
                            ListItemTypes.TMDB_MOVIE to movieTmdbId

                        else -> {
                            // Unsupported item type (e.g. episode, season, person).
                            // Handle just in case, API call above should already only
                            // return shows and movies.
                            Timber.w(
                                "Skipping unsupported list item (list id=%s, id=%s)",
                                traktListId, traktListItem.id
                            )
                            return@forEach
                        }
                    }

                    val key = ItemKey(itemType, tmdbId.toString())
                    traktItemKeys.add(key)

                    if (!databaseItemsByKey.containsKey(key)) {
                        itemsToInsert.add(
                            SgListItem(
                                itemRefId = tmdbId,
                                type = itemType,
                                listId = databaseListId
                            )
                        )
                    }
                }

                val itemIdsToDelete = databaseItemsByKey
                    .filterKeys { it !in traktItemKeys }
                    .values
                    .map { it.listItemId }

                if (itemsToInsert.isNotEmpty()) {
                    listHelper.insertListItems(itemsToInsert)
                }
                if (itemIdsToDelete.isNotEmpty()) {
                    listHelper.deleteListItems(itemIdsToDelete)
                }
            }
        } catch (_: IOException) {
            return false
        }
        return true
    }

}