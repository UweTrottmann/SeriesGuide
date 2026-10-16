// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse.IsAccountLimitExceeded
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktErrorResponse.IsAccountLocked
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse.Success
import com.uwetrottmann.trakt5.entities.ListEntry
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.entities.TraktList

/**
 * Trakt API calls used by [TraktListsSync]. Allows tests to replace them.
 */
interface TraktListsApi {

    /** Returns all lists of the user, or null on error. */
    suspend fun getLists(): List<TraktList>?

    /** Returns show and movie entries of the list, or null on error. */
    suspend fun getListItems(traktListId: Long): List<ListEntry>?

    /** Creates a list and returns its Trakt ID, or null on error. */
    suspend fun createList(name: String): Long?

    /** Returns the response, or null on error. */
    suspend fun addItemsToList(traktListId: Long, items: SyncItems): SyncResponse?

}

class TraktSyncListsApi(private val traktSync: TraktSync) : TraktListsApi {

    override suspend fun getLists(): List<TraktList>? {
        return when (val response = traktSync.traktTools.getLists(traktSync.users)) {
            is Success -> response.data
            is IsAccountLocked,
            is IsAccountLimitExceeded,
            is TraktErrorResponse.IsNotVip,
            is TraktErrorResponse.IsUnauthorized,
            is TraktErrorResponse.Other -> null
        }
    }

    override suspend fun getListItems(traktListId: Long): List<ListEntry>? {
        return when (
            val response = traktSync.traktTools.getListItems(traktSync.users, traktListId)
        ) {
            is Success -> response.data
            is IsAccountLocked,
            is IsAccountLimitExceeded,
            is TraktErrorResponse.IsNotVip,
            is TraktErrorResponse.IsUnauthorized,
            is TraktErrorResponse.Other -> null
        }
    }

    override suspend fun createList(name: String): Long? {
        return traktSync.awaitAndHandleUploadError {
            traktSync.traktTools.createList(traktSync.users, name)
        }
    }

    override suspend fun addItemsToList(traktListId: Long, items: SyncItems): SyncResponse? {
        return traktSync.awaitAndHandleUploadError {
            traktSync.traktTools.addItemsToList(traktSync.users, traktListId, items)
        }
    }

}
