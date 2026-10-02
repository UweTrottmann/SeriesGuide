// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2015 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.traktapi.TraktTools4
import com.battlelancer.seriesguide.traktapi.TraktTools4.TraktNonNullResponse
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating
import com.uwetrottmann.trakt5.services.Sync

/**
 * See [BaseRateItemTask]
 */
class RateShowTask(
    context: Context,
    rating: Rating?,
    private val showId: Long
) : BaseRateItemTask(context, rating) {

    override suspend fun sendToTrakt(traktSync: Sync): TraktNonNullResponse<SyncResponse>? {
        val showTmdbIdOrZero = SgRoomDatabase.getInstance(context).sgShow2Helper()
            .getShowTmdbId(showId)
        if (showTmdbIdOrZero == 0) return null
        return TraktTools4.rateShow(traktSync, showTmdbIdOrZero, rating)
    }

    override fun doDatabaseUpdate(): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgShow2Helper()
            .updateUserRating(showId, rating?.value ?: 0)
        return rowsUpdated > 0
    }
}
