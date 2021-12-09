package com.battlelancer.seriesguide.extensions

import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.util.ServiceUtils

/**
 * Searches the Google Play TV and movies section for an episode or movie.
 */
class GooglePlayExtension : SeriesGuideExtension("GooglePlayExtension") {

    override fun onRequest(episodeIdentifier: Int, episode: Episode) {
        // we need at least a show or an episode title
        if (episode.showTitle.isNullOrEmpty() || episode.title.isNullOrEmpty()) {
            return
        }
        publishGooglePlayAction(
            episodeIdentifier,
            String.format("%s %s", episode.showTitle, episode.title)
        )
    }

    override fun onRequest(movieIdentifier: Int, movie: Movie) {
        // we need at least a movie title
        if (movie.title.isNullOrEmpty()) {
            return
        }
        publishGooglePlayAction(movieIdentifier, movie.title)
    }

    private fun publishGooglePlayAction(identifier: Int, searchTerm: String) {
        publishAction(
            Action.Builder(getString(R.string.extension_google_play), identifier)
                .viewIntent(
                    ServiceUtils.buildGooglePlayIntent(searchTerm, applicationContext)
                )
                .build()
        )
    }
}