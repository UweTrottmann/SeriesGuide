package com.battlelancer.seriesguide.util

import android.content.Context
import android.util.Base64
import android.widget.ImageView
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.TmdbSettings
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools
import timber.log.Timber
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Tools for working with the image cache server.
 */
object ImageTools {

    private const val TVDB_MIRROR_BANNERS = "https://artworks.thetvdb.com/banners/"
    private const val TVDB_LEGACY_MIRROR_BANNERS = "https://www.thetvdb.com/banners/"

    private var sha256_hmac: Mac? = null
    private val cacheKey: String
        get() = BuildConfig.IMAGE_CACHE_SECRET

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
    fun tmdbOrTvdbPosterUrl(imagePath: String?, context: Context): String? {
        return if (imagePath.isNullOrEmpty()) {
            null
        } else {
            // If the path contains the legacy TVDB cache prefix, use the www subdomain as it has
            // a redirect to the new thumbnail URL set up (artworks subdomain + file name postfix).
            // E.g. https://www.thetvdb.com/banners/_cache/posters/example.jpg redirects to
            // https://artworks.thetvdb.com/banners/posters/example_t.jpg
            // Using the artworks subdomain with the legacy cache prefix is not supported.
            val imageUrl = when {
                imagePath.contains(TvdbImageTools.TVDB_LEGACY_CACHE_PREFIX, false) -> {
                    "${TVDB_LEGACY_MIRROR_BANNERS}$imagePath"
                }
                imagePath.startsWith("posters/") -> {
                    "${TVDB_MIRROR_BANNERS}$imagePath"
                }
                else -> {
                    // TMDB images have no path at all.
                    "${TmdbSettings.getPosterBaseUrl(context)}$imagePath"
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

    @JvmStatic
    fun loadAndResizeAndCrop(
        url: String?,
        imageView: ImageView,
        context: Context
    ) {
        ServiceUtils.loadWithPicasso(context, url)
            .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
            .centerCrop()
            .error(R.drawable.ic_photo_gray_24dp)
            .into(imageView)
    }

}