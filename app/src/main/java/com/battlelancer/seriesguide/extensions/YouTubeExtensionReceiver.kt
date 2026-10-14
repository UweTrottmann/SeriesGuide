// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2023 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.extensions

import android.content.Context
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.ExtensionActionProvider
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.util.ServiceUtils

/**
 * Searches YouTube for an episode or movie title. Useful for web shows and trailers!
 *
 * The class name is used as the receiver component name, which is stored as enabled extension.
 * The name passed to the super constructor is used to store subscriptions. Do not change either.
 */
class YouTubeExtensionReceiver : ExtensionActionProvider("YouTubeExtension") {

    override fun onEpisodeRequest(
        context: Context,
        episodeIdentifier: Int,
        episode: Episode
    ): Action? {
        // we need at least a show or an episode title
        if (episode.showTitle.isNullOrEmpty() || episode.title.isNullOrEmpty()) {
            return null
        }
        return buildAction(
            context,
            episodeIdentifier,
            String.format("%s %s", episode.showTitle, episode.title)
        )
    }

    override fun onMovieRequest(
        context: Context,
        movieIdentifier: Int,
        movie: Movie
    ): Action? {
        // we need a title to search for
        val title = movie.title
        if (title.isNullOrEmpty()) {
            return null
        }
        return buildAction(context, movieIdentifier, title)
    }

    private fun buildAction(context: Context, identifier: Int, searchTerm: String): Action {
        return Action.Builder(context.getString(R.string.extension_youtube), identifier)
            .viewIntent(ServiceUtils.buildYouTubeSearchIntent(searchTerm))
            .build()
    }
}
