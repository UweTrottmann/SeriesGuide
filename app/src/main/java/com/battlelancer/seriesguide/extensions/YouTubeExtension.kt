package com.battlelancer.seriesguide.extensions

import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.util.ServiceUtils

/**
 * Searches YouTube for an episode or movie title. Useful for web shows and trailers!
 */
class YouTubeExtension : SeriesGuideExtension("YouTubeExtension") {

    override fun onRequest(episodeIdentifier: Int, episode: Episode) {
        // we need at least a show or an episode title
        if (episode.showTitle.isNullOrEmpty() || episode.title.isNullOrEmpty()) {
            return
        }
        publishYoutubeAction(
            episodeIdentifier,
            String.format("%s %s", episode.showTitle, episode.title)
        )
    }

    override fun onRequest(movieIdentifier: Int, movie: Movie) {
        // we need a title to search for
        if (movie.title.isNullOrEmpty()) {
            return
        }
        publishYoutubeAction(movieIdentifier, movie.title)
    }

    private fun publishYoutubeAction(identifier: Int, searchTerm: String) {
        publishAction(
            Action.Builder(getString(R.string.extension_youtube), identifier)
                .viewIntent(ServiceUtils.buildYouTubeIntent(applicationContext, searchTerm))
                .build()
        )
    }
}