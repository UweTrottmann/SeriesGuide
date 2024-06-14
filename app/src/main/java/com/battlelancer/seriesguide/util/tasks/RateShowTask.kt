// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.uwetrottmann.trakt5.entities.ShowIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncShow
import com.uwetrottmann.trakt5.enums.Rating

/**
 * See [BaseRateItemTask]
 */
class RateShowTask(
    context: Context,
    rating: Rating?,
    private val showId: Long
) : BaseRateItemTask(context, rating) {

    override val traktAction: String
        get() = "rate show"

    override fun buildTraktSyncItems(): SyncItems? {
        val showTmdbIdOrZero = SgRoomDatabase.getInstance(context).sgShow2Helper()
            .getShowTmdbId(showId)
        if (showTmdbIdOrZero == 0) return null
        return SyncItems()
            .shows(SyncShow().id(ShowIds.tmdb(showTmdbIdOrZero)).rating(rating))
    }

    override fun doDatabaseUpdate(): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).sgShow2Helper()
            .updateUserRating(showId, rating?.value ?: 0)
        return rowsUpdated > 0
    }
}
