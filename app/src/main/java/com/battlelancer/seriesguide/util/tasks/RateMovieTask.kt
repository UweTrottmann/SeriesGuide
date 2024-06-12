// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.movies.tools.MovieTools
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.enums.Rating
import org.greenrobot.eventbus.EventBus

/**
 * See [BaseRateItemTask]
 */
class RateMovieTask(
    context: Context,
    rating: Rating?,
    private val movieTmdbId: Int
) : BaseRateItemTask(context, rating) {
    override val traktAction: String
        get() = "rate movie"

    override fun buildTraktSyncItems(): SyncItems {
        return SyncItems()
            .movies(SyncMovie().id(MovieIds.tmdb(movieTmdbId)).rating(rating))
    }

    override fun doDatabaseUpdate(): Boolean {
        val rowsUpdated = SgRoomDatabase.getInstance(context).movieHelper()
            .updateUserRating(movieTmdbId, rating?.value ?: 0)
        return rowsUpdated > 0
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: Int?) {
        @Suppress("DEPRECATION")
        super.onPostExecute(result)

        // post event so movie UI reloads (it is not listening to database changes)
        EventBus.getDefault().post(MovieTools.MovieChangedEvent(movieTmdbId))
    }

}
