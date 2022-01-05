package com.battlelancer.seriesguide.extensions

import android.content.Intent
import android.net.Uri
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.api.Action
import com.battlelancer.seriesguide.api.Episode
import com.battlelancer.seriesguide.api.Movie
import com.battlelancer.seriesguide.api.SeriesGuideExtension
import com.battlelancer.seriesguide.settings.AmazonSettings

/**
 * Provides a search link to the users preferred Amazon website.
 */
class AmazonExtension : SeriesGuideExtension("AmazonExtension") {

    override fun onRequest(episodeIdentifier: Int, episode: Episode) {
        // we need at least a show or an episode title
        if (episode.showTitle.isNullOrEmpty() || episode.title.isNullOrEmpty()) {
            return
        }
        publishAmazonAction(episodeIdentifier, "${episode.showTitle} ${episode.title}")
    }

    override fun onRequest(movieIdentifier: Int, movie: Movie) {
        // we need at least a movie title
        if (movie.title.isNullOrEmpty()) {
            return
        }
        publishAmazonAction(movieIdentifier, movie.title)
    }

    private fun publishAmazonAction(identifier: Int, searchTerm: String) {
        val domain = AmazonSettings.getAmazonCountryDomain(applicationContext)
        val uri = "https://$domain/s?k=$searchTerm"
        publishAction(
            Action.Builder(getString(R.string.extension_amazon), identifier)
                .viewIntent(Intent(Intent.ACTION_VIEW).setData(Uri.parse(uri)))
                .build()
        )
    }

}