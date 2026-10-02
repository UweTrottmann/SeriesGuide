// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.services.Sync

class RemoveShowFromWatchlistTask(
    context: Context,
    showTmdbId: Int
) : BaseShowActionTask(context, showTmdbId) {

    override suspend fun sendToTrakt(traktSync: Sync): TraktNonNullResponse<SyncResponse> {
        return TraktTools4.removeShowFromWatchlist(traktSync, showTmdbId)
    }

    override val successTextResId: Int
        get() = R.string.watchlist_removed
}
