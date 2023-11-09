// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.util

import android.content.Context
import android.util.Base64
import android.widget.ImageView
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.AppSettings
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import timber.log.Timber
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Tools for working with the image cache server.
 */
object ImageTools {

    const val TVDB_LEGACY_CACHE_PREFIX = "_cache/"
    private const val TVDB_MIRROR_BANNERS = "https://artworks.thetvdb.com/banners/"
    private const val TVDB_LEGACY_MIRROR_BANNERS = "https://www.thetvdb.com/banners/"

    private var sha256_hmac: Mac? = null
    private val cacheKey: String
        get() = BuildConfig.IMAGE_CACHE_SECRET

    /**
     * Build Picasso [com.squareup.picasso.RequestCreator] which respects user requirement of
     * only loading images over WiFi.
     *
     * If [Utils.isAllowedLargeDataConnection] is false, will set [com.squareup.picasso.NetworkPolicy.OFFLINE]
     * (which will set [okhttp3.CacheControl.FORCE_CACHE] on requests) to skip the network and
     * accept stale images.
     *
     * Always uses [context.getApplicationContext()][Context.getApplicationContext].
     */
    @JvmStatic
    fun loadWithPicasso(context: Context, path: String?): RequestCreator {
        val requestCreator = Picasso.get().load(path)
        if (!Utils.isAllowedLargeDataConnection(context.applicationContext)) {
            // avoid the network, hit the cache immediately + accept stale images.
            requestCreator.networkPolicy(NetworkPolicy.OFFLINE)
        }
        return requestCreator
    }

    /**
     * Like [tmdbOrTvdbPosterUrl], only if [imagePath] is empty, will return an URL
     * that will be resolved to a poster by [SgPicassoRequestHandler].
     */
    @JvmStatic
    fun posterUrlOrResolve(
        imagePath: String?,
        showTmdbId: Int,
        language: String?,
        context: Context
    ): String? {
        if (imagePath.isNullOrEmpty()) {
            var url = "${SgPicassoRequestHandler.SCHEME_SHOW_TMDB}://$showTmdbId"
            if (!language.isNullOrEmpty()) {
                url += "?${SgPicassoRequestHandler.QUERY_LANGUAGE}=$language"
            }
            return url
        }
        return tmdbOrTvdbPosterUrl(imagePath, context)
    }

    @JvmStatic
    fun tmdbOrTvdbPosterUrl(
        imagePath: String?,
        context: Context,
        originalSize: Boolean = false
    ): String? {
        return if (imagePath.isNullOrEmpty()) {
            null
        } else {
            if (AppSettings.isDemoModeEnabled(context)) {
                return pickDemoPosterUrl(imagePath)
            }

            // If the path contains the legacy TVDB cache prefix, use the www subdomain as it has
            // a redirect to the new thumbnail URL set up (artworks subdomain + file name postfix).
            // E.g. https://www.thetvdb.com/banners/_cache/posters/example.jpg redirects to
            // https://artworks.thetvdb.com/banners/posters/example_t.jpg
            // Using the artworks subdomain with the legacy cache prefix is not supported.
            val imageUrl = when {
                imagePath.contains(TVDB_LEGACY_CACHE_PREFIX, false) -> {
                    "${TVDB_LEGACY_MIRROR_BANNERS}$imagePath"
                }

                imagePath.startsWith("/") -> {
                    // TMDB images have no path at all, but always start with /.
                    // Use small size based on density, or original size (as large as possible).
                    if (originalSize) {
                        TmdbSettings.getImageOriginalUrl(context, imagePath)
                    } else {
                        "${TmdbSettings.getPosterBaseUrl(context)}$imagePath"
                    }
                }

                else -> {
                    "${TVDB_MIRROR_BANNERS}$imagePath"
                }
            }
            buildImageCacheUrl(imageUrl)
        }
    }

    private val demoPosterUrls = listOf(
        "https://seriesgui.de/demo/anime.jpg",
        "https://seriesgui.de/demo/crime.jpg",
        "https://seriesgui.de/demo/fantasy-2.jpg",
        "https://seriesgui.de/demo/fantasy.jpg",
        "https://seriesgui.de/demo/medical.jpg",
        "https://seriesgui.de/demo/scifi-3.jpg",
        "https://seriesgui.de/demo/scifi.jpg",
        "https://seriesgui.de/demo/sitcom.jpg",
    )

    private val demoStillUrl = "https://seriesgui.de/demo/episode-anime.jpg"

    private fun pickDemoPosterUrl(imagePath: String): String {
        // Map an image path always to the same image
        return demoPosterUrls[imagePath.hashCode().mod(demoPosterUrls.size)]
    }

    @JvmStatic
    fun tmdbOrTvdbStillUrl(
        imagePath: String?,
        context: Context,
        originalSize: Boolean = false
    ): String? {
        return if (imagePath.isNullOrEmpty()) {
            null
        } else {
            if (AppSettings.isDemoModeEnabled(context)) {
                return demoStillUrl
            }

            // If the path contains the legacy TVDB cache prefix, use the www subdomain as it has
            // a redirect to the new thumbnail URL set up (artworks subdomain + file name postfix).
            // E.g. https://www.thetvdb.com/banners/_cache/posters/example.jpg redirects to
            // https://artworks.thetvdb.com/banners/posters/example_t.jpg
            // Using the artworks subdomain with the legacy cache prefix is not supported.
            val imageUrl = when {
                imagePath.contains(TVDB_LEGACY_CACHE_PREFIX, false) -> {
                    "${TVDB_LEGACY_MIRROR_BANNERS}$imagePath"
                }

                imagePath.startsWith("/") -> {
                    // TMDB images have no path at all, but always start with /.
                    // Use small size based on density, or original size (as large as possible).
                    if (originalSize) {
                        TmdbSettings.getImageOriginalUrl(context, imagePath)
                    } else {
                        TmdbSettings.getStillUrl(context, imagePath)
                    }
                }

                else -> {
                    "${TVDB_MIRROR_BANNERS}$imagePath"
                }
            }
            buildImageCacheUrl(imageUrl)
        }
    }

    /**
     * [posterUrl] must not be empty.
     */
    fun buildImageCacheUrl(posterUrl: String): String? {
        @Suppress("SENSELESS_COMPARISON")
        if (BuildConfig.IMAGE_CACHE_URL == null) {
            return posterUrl // no cache
        }

        val mac = encodeImageUrl(posterUrl)
        return if (mac != null) {
            String.format("%s/s%s/%s", BuildConfig.IMAGE_CACHE_URL, mac, posterUrl)
        } else {
            null
        }
    }

    @Synchronized
    private fun encodeImageUrl(data: String): String? {
        return try {
            val mac = sha256_hmac ?: Mac.getInstance("HmacSHA256").also {
                val secretKey = SecretKeySpec(cacheKey.toByteArray(), "HmacSHA256")
                it.init(secretKey)
            }

            Base64.encodeToString(
                mac.doFinal(data.toByteArray()),
                Base64.NO_WRAP or Base64.URL_SAFE
            )
        } catch (e: Exception) {
            Timber.e(e, "Signing image URL failed.")
            null
        }
    }

    /**
     * Tries to load the given show poster into the given [ImageView]
     * without any resizing or cropping.
     */
    fun loadShowPoster(
        context: Context,
        imageView: ImageView,
        posterPath: String?
    ) {
        loadWithPicasso(context, tmdbOrTvdbPosterUrl(posterPath, context))
            .noFade()
            .into(imageView)
    }

    /**
     * Tries to load the given TVDb show poster into the given [ImageView] without any
     * resizing or cropping. In addition sets alpha on the view.
     */
    @JvmStatic
    fun loadShowPosterAlpha(
        context: Context,
        imageView: ImageView,
        posterPath: String?
    ) {
        imageView.imageAlpha = 30
        loadShowPoster(context, imageView, posterPath)
    }

    /**
     * Tries to load a resized, center cropped version of the show poster into the given
     * [ImageView]. On failure displays an error drawable (ensure image view is set to center
     * inside).
     *
     * The resize dimensions are those used for posters in the show list and change depending on
     * screen size.
     */
    @JvmStatic
    fun loadShowPosterResizeCrop(
        context: Context,
        imageView: ImageView,
        posterPath: String?
    ) {
        loadShowPosterUrlResizeCrop(context, imageView, tmdbOrTvdbPosterUrl(posterPath, context))
    }

    @JvmStatic
    fun loadShowPosterUrlResizeCrop(
        context: Context,
        imageView: ImageView,
        url: String?
    ) {
        loadWithPicasso(context, url)
            .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
            .centerCrop()
            .error(R.drawable.ic_photo_gray_24dp)
            .into(imageView)
    }

    fun loadShowPosterResizeSmallCrop(
        context: Context,
        imageView: ImageView,
        posterPath: String?
    ) {
        loadShowPosterUrlResizeSmallCrop(
            context,
            imageView,
            tmdbOrTvdbPosterUrl(posterPath, context)
        )
    }

    /**
     * Tries to load a resized, center cropped version of the show poster into the given
     * [ImageView]. On failure displays an error drawable (ensure image view is set to center
     * inside).
     *
     * The resize dimensions are fixed for all screen sizes. Like for items using the show list
     * layout, use [loadShowPosterResizeCrop].
     *
     * @param posterUrl This should already be a built poster URL, not just a poster path!
     */
    @JvmStatic
    fun loadShowPosterUrlResizeSmallCrop(
        context: Context,
        imageView: ImageView,
        posterUrl: String?
    ) {
        loadWithPicasso(context, posterUrl)
            .resizeDimen(R.dimen.show_poster_width_default, R.dimen.show_poster_height_default)
            .centerCrop()
            .error(R.drawable.ic_photo_gray_24dp)
            .into(imageView)
    }

    /**
     * Tries to load a resized, center cropped version of the show poster into the given
     * [ImageView]. On failure displays an error drawable (ensure image view is set to center
     * inside).
     *
     * The resize dimensions are determined based on the image view size.
     */
    fun loadShowPosterFitCrop(
        posterPath: String?,
        imageView: ImageView,
        context: Context
    ) {
        loadWithPicasso(context, tmdbOrTvdbPosterUrl(posterPath, context))
            .fit()
            .centerCrop()
            .error(R.drawable.ic_photo_gray_24dp)
            .into(imageView)
    }

}