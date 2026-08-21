// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.sync

import com.battlelancer.seriesguide.traktapi.TraktSettings
import com.battlelancer.seriesguide.util.TimeTools
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

    fun sync(updatedAt: OffsetDateTime?): Boolean {
        if (updatedAt == null) {
            Timber.e("sync: null updatedAt")
            return false
        }

        val isInitialSync = TraktSettings.isInitialSyncLists(context)

        // Do not sync if notes have not changed, or is not initial sync
        val lastUpdatedAt = TraktSettings.getLastListsUpdatedAt(context)
        if (!isInitialSync && !TimeTools.isAfterMillis(updatedAt, lastUpdatedAt)) {
            Timber.d("sync: no changes since %tF %tT", lastUpdatedAt, lastUpdatedAt)
            return true
        }

        TODO()

        return true
    }

}