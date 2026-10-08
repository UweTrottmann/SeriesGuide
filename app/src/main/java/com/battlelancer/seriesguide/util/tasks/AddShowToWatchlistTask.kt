// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2016 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.uwetrottmann.trakt5.TraktV2
import com.uwetrottmann.trakt5.entities.SyncResponse

class AddShowToWatchlistTask(
    context: Context,
    showTmdbId: Int
) : BaseShowActionTask(context, showTmdbId) {

    override suspend fun sendToTrakt(trakt: TraktV2): TraktNonNullResponse<SyncResponse> {
        return TraktTools4.addShowToWatchlist(trakt, showTmdbId)
    }

    override val successTextResId: Int
        get() = R.string.watchlist_added
}
