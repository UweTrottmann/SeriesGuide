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
 * Performs a web search for a given episode or movie title using a search [android.content.Intent].
 *
 * The class name is used as the receiver component name, which is stored as enabled extension.
 * The name passed to the super constructor is used to store subscriptions. Do not change either.
 */
class WebSearchExtensionReceiver : ExtensionActionProvider("WebSearchExtension") {

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
        return Action.Builder(context.getString(R.string.web_search), identifier)
            .viewIntent(ServiceUtils.buildWebSearchIntent(searchTerm))
            .build()
    }
}
