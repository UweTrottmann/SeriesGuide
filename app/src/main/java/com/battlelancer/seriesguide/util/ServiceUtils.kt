package com.battlelancer.seriesguide.util

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import com.battlelancer.seriesguide.R

/**
 * Helper methods to interact with other apps or websites.
 */
object ServiceUtils {
    private const val IMDB_APP_TITLE_URI_POSTFIX = "/"
    private const val IMDB_APP_TITLE_URI = "imdb:///title/"
    private const val IMDB_TITLE_URL = "http://imdb.com/title/"
    private const val YOUTUBE_BASE_URL = "http://www.youtube.com/watch?v="
    private const val YOUTUBE_SEARCH = "http://www.youtube.com/results?search_query=%s"
    private const val YOUTUBE_PACKAGE = "com.google.android.youtube"

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
            // on failure, try launching the web page
            Utils.launchWebsite(context, imdbLink(imdbId))
        }
    }

    private fun imdbLink(imdbId: String): String = "$IMDB_TITLE_URL$imdbId"

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
        Utils.launchWebsite(context, YOUTUBE_BASE_URL + videoId)
    }

    /**
     * Builds a search [android.content.Intent] to open the YouTube application to search for
     * the [query]. If the YouTube app is unavailable, a view [android.content.Intent]
     * with the web search URL is returned instead.
     */
    fun buildYouTubeIntent(context: Context, query: String?): Intent {
        val pm = context.packageManager
        val hasYouTube: Boolean = try {
            pm.getPackageInfo(YOUTUBE_PACKAGE, PackageManager.GET_ACTIVITIES)
            true
        } catch (notInstalled: PackageManager.NameNotFoundException) {
            false
        }

        val intent: Intent = if (hasYouTube) {
            // Directly search the YouTube app
            Intent(Intent.ACTION_SEARCH)
                .setPackage(YOUTUBE_PACKAGE)
                .putExtra("query", query)
        } else {
            // Launch a web search
            Intent(Intent.ACTION_VIEW)
                .apply { data = Uri.parse(String.format(YOUTUBE_SEARCH, Uri.encode(query))) }
        }
        return intent
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