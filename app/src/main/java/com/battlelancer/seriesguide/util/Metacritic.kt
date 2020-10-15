package com.battlelancer.seriesguide.util

import android.content.Context
import android.net.Uri

object Metacritic {

    /**
     * Starts VIEW Intent with Metacritic website movie search results URL.
     */
    fun searchForMovie(context: Context, title: String) {
        val url = "https://www.metacritic.com/search/movie/${Uri.encode(title)}/results"
        Utils.launchWebsite(context, url)
    }

    /**
     * Starts VIEW Intent with Metacritic website TV search results URL.
     */
    fun searchForTvShow(context: Context, title: String) {
        val url = "https://www.metacritic.com/search/tv/${Uri.encode(title)}/results"
        Utils.launchWebsite(context, url)
    }

}