package com.battlelancer.seriesguide.extensions

import android.content.Intent
import android.net.Uri
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.SeriesGuideExtension

/**
 * Searches vodster.de for shows and movies.
 */
class VodsterExtension : SeriesGuideExtension("VodsterExtension") {

    override fun onRequest(episodeIdentifier: Int, episode: Episode) {
        val showTvdbId = episode.showTvdbId
        if (showTvdbId != 0) {
            publishVodsterAction(episodeIdentifier, "tvdb=$showTvdbId")
        }
    }

    override fun onRequest(movieIdentifier: Int, movie: Movie) {
        publishVodsterAction(movieIdentifier, "tmdb=${movie.tmdbId}")
    }

    private fun publishVodsterAction(identifier: Int, query: String) {
        val uri = VODSTER_SEARCH_URL + query
        publishAction(
            Action.Builder(getString(R.string.extension_vodster), identifier)
                .viewIntent(Intent(Intent.ACTION_VIEW).setData(Uri.parse(uri)))
                .build()
        )
    }

    companion object {
        private const val VODSTER_SEARCH_URL = "https://www.vodster.de?"
    }
}