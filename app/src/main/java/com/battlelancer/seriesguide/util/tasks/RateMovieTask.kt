// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util.tasks

import android.content.ContentValues
import android.content.Context
import com.battlelancer.seriesguide.movies.tools.MovieTools.MovieChangedEvent
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.uwetrottmann.trakt5.entities.MovieIds
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncMovie
import com.uwetrottmann.trakt5.enums.Rating
import org.greenrobot.eventbus.EventBus

/**
 * Stores the rating for the given movie in the database (if it is in the database) and sends it
 * to trakt.
 */
class RateMovieTask(
    context: Context,
    rating: Rating,
    private val movieTmdbId: Int
) : BaseRateItemTask(context, rating) {
    override val traktAction: String
        get() = "rate movie"

    override fun buildTraktSyncItems(): SyncItems {
        return SyncItems()
            .movies(SyncMovie().id(MovieIds.tmdb(movieTmdbId)).rating(rating))
    }

    override fun doDatabaseUpdate(): Boolean {
        val values = ContentValues()
        values.put(SeriesGuideContract.Movies.RATING_USER, rating.value)

        val rowsUpdated = context.contentResolver
            .update(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId), values, null, null)

        return rowsUpdated > 0
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: Int?) {
        super.onPostExecute(result)

        // post event so movie UI reloads (it is not listening to database changes)
        EventBus.getDefault().post(MovieChangedEvent(movieTmdbId))
    }
}
