// SPDX-License-Identifier: Apache-2.0
// Copyright 2017-2025 Uwe Trottmann
// Copyright 2013 Square, Inc.

package com.battlelancer.seriesguide.util

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.squareup.picasso.Downloader
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso.LoadedFrom.DISK
import com.squareup.picasso.Picasso.LoadedFrom.NETWORK
import com.squareup.picasso.Request
import com.squareup.picasso.RequestHandler
import kotlinx.coroutines.runBlocking
import okhttp3.CacheControl
import java.io.IOException

/**
 * This is mostly a copy of [com.squareup.picasso.NetworkRequestHandler] that is not visible.
 * Extended to fetch the image url from a given show TVDB id or movie TMDB id.
 */
class SgPicassoRequestHandler(
    private val downloader: Downloader,
    context: Context
) : RequestHandler() {

    private val context: Context = context.applicationContext

    override fun canHandleRequest(data: Request): Boolean {
        val scheme = data.uri.scheme
        return SCHEME_SHOW_TMDB == scheme || SCHEME_MOVIE_TMDB == scheme
    }

    @Throws(IOException::class)
    override fun load(request: Request, networkPolicy: Int): Result? {
        val scheme = request.uri.scheme
        val host = request.uri.host
            ?: return null

        if (SCHEME_SHOW_TMDB == scheme) {
            val showTmdbId = host.toInt()
            var language = request.uri.getQueryParameter(QUERY_LANGUAGE)

            if (language.isNullOrEmpty()) {
                language = LanguageTools.LANGUAGE_EN
            }

            val showDetails = TmdbTools2().getShowDetails(showTmdbId, language, context)
            if (showDetails != null) {
                val url = ImageTools.tmdbOrTvdbPosterUrl(showDetails.poster_path, context, false)
                if (url != null) {
                    return loadFromNetwork(url.toUri())
                }
            }
        }

        if (SCHEME_MOVIE_TMDB == scheme) {
            val movieTmdbId = host.toInt()

            val posterPath: String? = try {
                runBlocking {
                    SgApp.getServicesComponent(context).movieTools()
                        .getMoviePosterPath(movieTmdbId)
                }
            } catch (e: InterruptedException) {
                null // Do nothing
            }
            if (posterPath != null) {
                val imageUrl = TmdbSettings.getImageBaseUrl(context) +
                        TmdbSettings.POSTER_SIZE_SPEC_W342 + posterPath
                return loadFromNetwork(imageUrl.toUri())
            }
        }

        return null
    }

    @Throws(IOException::class)
    private fun loadFromNetwork(uri: Uri): Result {
        // because retry-count is fixed to 0 for custom request handlers
        // BitmapHunter forces the network policy to OFFLINE
        // https://github.com/square/picasso/issues/2038
        // until fixed, re-set the network policy here also (like ServiceUtils.loadWithPicasso)
        val networkPolicy = if (ImageTools.isAllowedLargeDataConnection(context)) {
            0 // no policy
        } else {
            // avoid the network, hit the cache immediately + accept stale images.
            1 shl 2 // NetworkPolicy.OFFLINE
        }

        val downloaderRequest = createRequest(uri, networkPolicy)
        val response = downloader.load(downloaderRequest)
        val body = response.body

        if (body == null || !response.isSuccessful) {
            body?.close()
            throw ResponseException(response.code)
        }

        // Cache response is only null when the response comes fully from the network. Both completely
        // cached and conditionally cached responses will have a non-null cache response.
        val loadedFrom = if (response.cacheResponse == null) {
            NETWORK
        } else {
            DISK
        }

        // Sometimes response content length is zero when requests are being replayed. Haven't found
        // root cause to this but retrying the request seems safe to do so.
        if (loadedFrom == DISK && body.contentLength() == 0L) {
            body.close()
            throw ContentLengthException("Received response with 0 content-length header.")
        }
        return Result(body.source(), loadedFrom)
    }

    private fun createRequest(uri: Uri, networkPolicy: Int): okhttp3.Request {
        var cacheControl: CacheControl? = null
        if (networkPolicy != 0) {
            cacheControl = if (NetworkPolicy.isOfflineOnly(networkPolicy)) {
                CacheControl.FORCE_CACHE
            } else {
                val builder = CacheControl.Builder()
                if (!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
                    builder.noCache()
                }
                if (!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
                    builder.noStore()
                }
                builder.build()
            }
        }

        val builder = okhttp3.Request.Builder().url(uri.toString())
        if (cacheControl != null) {
            builder.cacheControl(cacheControl)
        }
        return builder.build()
    }

    class ContentLengthException(message: String) : IOException(message)

    class ResponseException(val code: Int) : IOException("HTTP $code")

    companion object {
        const val SCHEME_SHOW_TMDB = "showtmdb"
        const val SCHEME_MOVIE_TMDB = "movietmdb"
        const val QUERY_LANGUAGE = "language"
    }
}
