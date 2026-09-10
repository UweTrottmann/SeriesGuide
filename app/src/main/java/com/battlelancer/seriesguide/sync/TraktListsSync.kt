// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse.Success
import com.battlelancer.seriesguide.util.TimeTools
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
    private val traktSync: TraktSync
) {

    private val context = traktSync.context

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

        val listsAtTrakt = runBlocking(Dispatchers.Default) {
            when (val response = TraktTools4.getLists(traktSync.users)) {
                is Success -> response.data
                else -> null
            }
        } ?: return false

        listsAtTrakt.forEach { list ->
            val listId = list.ids?.trakt
            if (listId == null) {
                // Something must be wrong with the API, stop
                Timber.e("sync: list id is null")
                return false
            }

            // Add lists at Trakt, but not in database
            // On initial sync, upload lists and assign Trakt ID
            // Delete lists not at Trakt (for safety, only if they have a Trakt list ID)

            val listItemsAtTrakt = runBlocking(Dispatchers.Default) {
                when (val response = TraktTools4.getListItems(traktSync.users, listId)) {
                    is Success -> response.data
                    else -> null
                }
            } ?: return false
        }

        if (isInitialSync) {
            TraktSettings.setInitialSyncListsCompleted(context)
        }
        TraktSettings.storeLastListsUpdatedAt(context, updatedAt)

        return true
    }

}