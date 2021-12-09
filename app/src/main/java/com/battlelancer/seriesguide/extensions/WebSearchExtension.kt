package com.battlelancer.seriesguide.extensions

import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.util.ServiceUtils

/**
 * Performs a web search for a given episode or movie title using a search [android.content.Intent].
 */
class WebSearchExtension : SeriesGuideExtension("WebSearchExtension") {

    override fun onRequest(episodeIdentifier: Int, episode: Episode) {
        // we need at least a show or an episode title
        if (episode.showTitle.isNullOrEmpty() || episode.title.isNullOrEmpty()) {
            return
        }
        publishWebSearchAction(
            episodeIdentifier,
            String.format("%s %s", episode.showTitle, episode.title)
        )
    }

    override fun onRequest(movieIdentifier: Int, movie: Movie) {
        // we need at least a movie title
        if (movie.title.isNullOrEmpty()) {
            return
        }
        publishWebSearchAction(movieIdentifier, movie.title)
    }

    private fun publishWebSearchAction(identifier: Int, searchTerm: String) {
        publishAction(
            Action.Builder(getString(R.string.web_search), identifier)
                .viewIntent(ServiceUtils.buildWebSearchIntent(searchTerm))
                .build()
        )
    }
}