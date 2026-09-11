// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import androidx.datastore.core.IOException
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.room.withTransaction
import com.battlelancer.seriesguide.lists.ListsTools
import com.battlelancer.seriesguide.lists.database.SgList
import com.battlelancer.seriesguide.lists.database.SgListHelper
import com.battlelancer.seriesguide.lists.database.SgListItem
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools4
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
        EXISTING_LISTS_UPLOAD
    }

    /**
     * Note: this uses [runBlocking], so if the calling thread is interrupted this will throw
     * [InterruptedException].
     */
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

        if (isInitialSync) {
            // TODO Depending on user preference, delete existing lists or upload them
            val initialSyncPreference: InitialSync = InitialSync.EXISTING_LISTS_DELETE
            when (initialSyncPreference) {
                InitialSync.EXISTING_LISTS_DELETE -> {
                    Timber.d("initial sync: use chose to delete existing lists")
                    runBlocking(Dispatchers.IO) {
                        database.useWriterConnection {
                            it.immediateTransaction {
                                listHelper.deleteAllListsAndItems()
                            }
                        }
                    }
                }

                InitialSync.EXISTING_LISTS_UPLOAD -> {
                    Timber.d("initial sync: use chose to upload existing lists")
                    val success = runBlocking(Dispatchers.Default) {
                        uploadAllLists()
                    }
                    if (!success) {
                        return false
                    }
                }
            }
        }

        // Match local lists with what's on Trakt (not items)
        val traktLists = runBlocking(Dispatchers.Default) {
            when (val response = TraktTools4.getLists(traktSync.users)) {
                is Success -> response.data
                else -> null
            }
        } ?: return false

        try {
            runBlocking(Dispatchers.Default) {
                database.withTransaction {
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

                        val databaseList = databaseLists.find { it.traktId == traktId }
                        val databaseId = if (databaseList == null) {
                            // Add list to database
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
                            // Update list in database
                            databaseList.name = traktName
                            listHelper.insertList(databaseList)
                            databaseList.listId
                        }
                        databaseListIdsToKeep.add(databaseId)
                    }

                    // Delete lists with items not on Trakt
                    val databaseListIdsToDelete = databaseLists
                        .filter { it.listId !in databaseListIdsToKeep }
                        .map { it.listId }
                    databaseListIdsToDelete.forEach { listId ->
                        listHelper.deleteListAndItems(listId)
                    }
                }
            }
        } catch (_: IOException) {
            return false
        }

        // Match local list items with what's on Trakt
        traktLists.forEach {
            val success = runBlocking(Dispatchers.Default) {
                syncListItems(it.ids!!.trakt!!)
            }
            if (!success) {
                return false
            }
        }

        if (isInitialSync) {
            TraktSettings.setInitialSyncListsCompleted(context)
        }
        TraktSettings.storeLastListsUpdatedAt(context, updatedAt)

        return true
    }

    /**
     * Creates a list on Trakt for each local list and adds all items to the list. Sets the Trakt ID
     * if successful.
     */
    private suspend fun uploadAllLists(): Boolean {
        listHelper.getListsForExport().forEach { list: SgList ->
            if (list.traktId != null) {
                // List was already uploaded, skip it
                return@forEach
            }

            val traktList =
                when (val response = TraktTools4.createList(traktSync.users, list.name)) {
                    is Success -> response.data
                    else -> null
                } ?: return false

            val listTraktId = traktList.ids?.trakt
            if (listTraktId == null) {
                Timber.e(
                    "Created Trakt list doesn't have an ID (name=%s)",
                    list.name
                )
                return false
            }

            // Upload list items
            val listItems = listHelper.getListItemsForExport(list.listId)
            val shows = listItems
                .filter { it.type == ListItemTypes.TMDB_SHOW }
                .map { SyncShow().id(ShowIds.tmdb(it.itemRefId.toInt())) }
            val movies = listItems
                .filter { it.type == ListItemTypes.TMDB_MOVIE }
                .map { SyncMovie().id(MovieIds.tmdb(it.itemRefId.toInt())) }
            val syncItems = SyncItems()
                .shows(shows)
                .movies(movies)

            val syncResponse =
                when (val response =
                    TraktTools4.addItemsToList(traktSync.users, listTraktId, syncItems)) {
                    is Success -> response.data
                    else -> null
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
                return false
            }

            // Set Trakt ID to signal this list was uploaded
            listHelper.updateListTraktId(list.listId, listTraktId)
        }

        return true
    }

    /**
     * Note: does not add or remove movies added to or removed from a custom list to or from the
     * database. This is done during a later sync step.
     */
    private suspend fun syncListItems(traktListId: Int): Boolean {
        // Do API call outside database transaction so it can complete quickly
        val traktListItems =
            when (val response = TraktTools4.getListItems(traktSync.users, traktListId)) {
                is Success -> response.data
                else -> null
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
                                "Skipping unsupported list item (list id=%s, id=%s",
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