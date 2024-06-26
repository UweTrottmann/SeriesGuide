// SPDX-License-Identifier: Apache-2.0
// Copyright 2015-2024 Uwe Trottmann

package com.battlelancer.seriesguide.util.tasks

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.uwetrottmann.trakt5.entities.SyncItems
import com.uwetrottmann.trakt5.entities.SyncResponse
import com.uwetrottmann.trakt5.enums.Rating

/**
 * Stores the [rating] in the database and sends it to Trakt.
 *
 * If it is `null`, removes the rating instead.
 */
abstract class BaseRateItemTask(
    context: Context,
    protected val rating: Rating?
) : BaseActionTask(context) {

    override val isSendingToHexagon: Boolean
        get() = false // Hexagon does not support ratings.

    override fun doBackgroundAction(vararg params: Void?): Int? {
        if (isSendingToTrakt) {
            if (!TraktCredentials.get(context).hasCredentials()) {
                return ERROR_TRAKT_AUTH
            }

            val ratedItems = buildTraktSyncItems() ?: return ERROR_DATABASE

            val trakt = SgApp.getServicesComponent(context).trakt()
            val traktSync = trakt.sync()
            val call = if (rating != null) {
                traktSync.addRatings(ratedItems)
            } else {
                traktSync.deleteRatings(ratedItems)
            }
            val result =
                executeTraktCall<SyncResponse>(
                    call,
                    trakt,
                    traktAction,
                    object : ResponseCallback<SyncResponse> {
                        override fun handleSuccessfulResponse(body: SyncResponse): Int {
                            val notFound = body.not_found
                            if (notFound != null) {
                                val movies = notFound.movies
                                val shows = notFound.shows
                                val episodes = notFound.episodes
                                if ((movies != null && movies.size != 0)
                                    || (shows != null && shows.size != 0)
                                    || (episodes != null && episodes.size != 0)) {
                                    // movie, show or episode not found on trakt
                                    return ERROR_TRAKT_API_NOT_FOUND
                                }
                            }
                            return SUCCESS
                        }
                    })
            if (result != SUCCESS) {
                return result
            }
        }

        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE
        }

        return SUCCESS
    }

    override val successTextResId: Int
        get() = R.string.ack_rated

    protected abstract val traktAction: String

    /**
     * May return null on database error.
     */
    protected abstract fun buildTraktSyncItems(): SyncItems?

    protected abstract fun doDatabaseUpdate(): Boolean
}
