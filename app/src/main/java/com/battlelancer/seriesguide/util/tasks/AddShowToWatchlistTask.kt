// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.R
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.services.Sync
import retrofit2.Call

class AddShowToWatchlistTask(
    context: Context,
    showTmdbId: Int
) : BaseShowActionTask(context, showTmdbId) {

    override val traktAction: String
        get() = "add show to watchlist"

    override fun buildTraktCall(traktSync: Sync, items: SyncItems): Call<SyncResponse> {
        return traktSync.addItemsToWatchlist(items)
    }

    override val successTextResId: Int
        get() = R.string.watchlist_added
}
