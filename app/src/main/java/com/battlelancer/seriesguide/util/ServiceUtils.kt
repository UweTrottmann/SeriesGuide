// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.database.SgEpisode2
import com.battlelancer.seriesguide.shows.database.SgShow2
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Helper methods to interact with other apps or websites.
 */
object ServiceUtils {
    private const val IMDB_APP_TITLE_URI_POSTFIX = "/"
    private const val IMDB_APP_TITLE_URI = "imdb:///title/"
    private const val IMDB_TITLE_URL = "http://imdb.com/title/"
    private const val YOUTUBE_BASE_URL = "http://www.youtube.com/watch?v="
    private const val YOUTUBE_SEARCH = "http://www.youtube.com/results?search_query=%s"

    /**
     * Displays the IMDb page for the given id (show or episode) in the IMDb app or on the imdb.com
     * web page. If the IMDb id is empty, disables the button.
     */
    fun setUpImdbButton(imdbId: String?, imdbButton: View?) {
        if (imdbButton == null) {
            return
        }
        if (imdbId.isNullOrEmpty()) {
            imdbButton.isEnabled = false
        } else {
            imdbButton.isEnabled = true
            imdbButton.setOnClickListener(View.OnClickListener { v: View ->
                openImdb(imdbId, v.context)
            })
            imdbButton.copyTextToClipboardOnLongClick(imdbLink(imdbId))
        }
    }

    /**
     * Open the IMDb app or web page for the given IMDb id.
     */
    fun openImdb(imdbId: String?, context: Context?) {
        if (context == null || imdbId.isNullOrEmpty()) {
            return
        }

        // try launching the IMDb app
        val intent = Intent(
            Intent.ACTION_VIEW, Uri
                .parse("$IMDB_APP_TITLE_URI$imdbId$IMDB_APP_TITLE_URI_POSTFIX")
        )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        if (!Utils.tryStartActivity(context, intent, false)) {
            // If the app is not available, open website instead.
            WebTools.openInApp(context, imdbLink(imdbId))
        }
    }

    private fun imdbLink(imdbId: String): String = "$IMDB_TITLE_URL$imdbId"

    /**
     * Configures button to open IMDB, if needed fetches ID from network while disabling button.
     */
    fun configureImdbButton(
        button: View,
        coroutineScope: CoroutineScope,
        context: Context,
        show: SgShow2?,
        episode: SgEpisode2
    ) {
        button.apply {
            isEnabled = true
            setOnClickListener { button ->
                // Disable button to prevent multiple presses.
                button.isEnabled = false
                coroutineScope.launch {
                    if (show?.tmdbId == null) {
                        button.isEnabled = true
                        return@launch
                    }
                    val episodeImdbId = if (!episode.imdbId.isNullOrEmpty()) {
                        episode.imdbId
                    } else {
                        withContext(Dispatchers.IO) {
                            TmdbTools2().getImdbIdForEpisode(
                                SgApp.getServicesComponent(context).tmdb().tvEpisodesService(),
                                show.tmdbId, episode.season, episode.number
                            )?.also {
                                SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                                    .updateImdbId(episode.id, it)
                            }
                        }
                    }
                    val imdbId = if (episodeImdbId.isNullOrEmpty()) {
                        show.imdbId // Fall back to show IMDb id.
                    } else {
                        episodeImdbId
                    }
                    // Leave button disabled if no id found.
                    if (!imdbId.isNullOrEmpty()) {
                        button.isEnabled = true
                        openImdb(imdbId, context)
                    }
                }
            }
        }
    }

    /**
     * Returns a view [android.content.Intent] for a search of Google Play's movies category
     * (includes TV shows).
     */
    @SuppressLint("StringFormatInvalid")
    fun buildGooglePlayIntent(title: String, context: Context): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        val playStoreQuery = String.format(
            context.getString(R.string.url_movies_search),
            Uri.encode(title)
        )
        intent.data = Uri.parse(playStoreQuery)
        return intent
    }

    /**
     * Opens the YouTube app or web page for the given video.
     */
    fun openYoutube(videoId: String, context: Context) {
        WebTools.openInApp(context, YOUTUBE_BASE_URL + videoId)
    }

    /**
     * Builds an intent to search YouTube for the [query].
     */
    fun buildYouTubeSearchIntent(query: String?): Intent {
            return Intent(Intent.ACTION_VIEW)
                .apply { data = Uri.parse(String.format(YOUTUBE_SEARCH, Uri.encode(query))) }
    }

    /**
     * Builds a search [android.content.Intent] using [Intent.ACTION_WEB_SEARCH] and
     * [query] as [android.app.SearchManager.QUERY] extra.
     */
    fun buildWebSearchIntent(query: String): Intent =
        Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)

    /**
     * Attempts to search the web for [query].
     *
     * @param context The [Context] to use
     * @param query The search query
     */
    fun performWebSearch(context: Context, query: String) {
        Utils.openNewDocument(context, buildWebSearchIntent(query))
    }

    /**
     * Set up the [button] to search the web for [query] with [performWebSearch].
     */
    fun setUpWebSearchButton(query: String?, button: View?) {
        if (button == null) {
            // Return if the button isn't initialized
            return
        } else if (query.isNullOrEmpty()) {
            // Disable the button if there's nothing to search for
            button.isEnabled = false
            return
        }
        button.setOnClickListener(View.OnClickListener { v: View ->
            performWebSearch(v.context, query)
        })
    }
}