package com.battlelancer.seriesguide.thetvdbapi

import android.content.Context
import android.util.Base64
import android.widget.ImageView
import com.battlelancer.seriesguide.BuildConfig
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler
import timber.log.Timber
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TvdbImageTools {

    private const val TVDB_MIRROR_BANNERS = "https://artworks.thetvdb.com/banners/"
    const val TVDB_CACHE_PREFIX = "_cache/"
    private var sha256_hmac: Mac? = null

    /**
     * Builds a url for a TVDb poster or screenshot (episode still) using the given image path.
     */
    @JvmStatic
    fun artworkUrl(imagePath: String?): String? {
        return if (imagePath.isNullOrEmpty()) {
            null
        } else {
            buildImageCacheUrl("$TVDB_MIRROR_BANNERS$imagePath")
        }
    }

    /**
     * Builds a full url for a TVDb show poster using the given image path.
     *
     * @param imagePath If empty, will return an URL that will be resolved to the highest rated
     * small poster using additional network requests.
     */
    @JvmStatic
    fun posterUrlOrResolve(
        imagePath: String?,
        showTvdbId: Int,
        language: String?
    ): String? {
        if (imagePath.isNullOrEmpty()) {
            var url = "${SgPicassoRequestHandler.SCHEME_SHOW_TVDB}://$showTvdbId"
            if (!language.isNullOrEmpty()) {
                url += "?${SgPicassoRequestHandler.QUERY_LANGUAGE}=$language"
            }
            return url
        }
        return artworkUrl(imagePath)
    }

    /**
     * Tries to load the given TVDb show poster into the given [ImageView]
     * without any resizing or cropping.
     */
    fun loadShowPoster(
        context: Context,
        imageView: ImageView,
        posterPath: String?
    ) {
        ServiceUtils.loadWithPicasso(context, artworkUrl(posterPath))
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
        ServiceUtils.loadWithPicasso(context, artworkUrl(posterPath))
            .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
            .centerCrop()
            .error(R.drawable.ic_photo_gray_24dp)
            .into(imageView)
    }

    @JvmStatic
    fun loadUrlResizeCrop(
        context: Context,
        imageView: ImageView,
        url: String?
    ) {
        ServiceUtils.loadWithPicasso(context, url)
            .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
            .centerCrop()
            .error(R.drawable.ic_photo_gray_24dp)
            .into(imageView)
    }

    /**
     * Tries to load a resized, center cropped version of the show poster into the given
     * [ImageView]. On failure displays an error drawable (ensure image view is set to center
     * inside).
     *
     * The resize dimensions are fixed for all screen sizes. Like for items using the show list
     * layout, use [TvdbImageTools.loadShowPosterResizeCrop].
     *
     * @param posterUrl This should already be a built TVDB poster URL, not just a poster path!
     */
    @JvmStatic
    fun loadShowPosterResizeSmallCrop(
        context: Context,
        imageView: ImageView,
        posterUrl: String?
    ) {
        ServiceUtils.loadWithPicasso(context, posterUrl)
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
        context: Context,
        imageView: ImageView,
        posterPath: String?
    ) {
        ServiceUtils.loadWithPicasso(context, artworkUrl(posterPath))
            .fit()
            .centerCrop()
            .error(R.drawable.ic_photo_gray_24dp)
            .into(imageView)
    }

    /**
     * @param posterUrlTvdb Expected to be not empty.
     */
    private fun buildImageCacheUrl(posterUrlTvdb: String): String? {
        @Suppress("SENSELESS_COMPARISON")
        if (BuildConfig.IMAGE_CACHE_URL == null) {
            return posterUrlTvdb // no cache
        }

        val mac = encodeImageUrl(BuildConfig.IMAGE_CACHE_SECRET, posterUrlTvdb)
        return if (mac != null) {
            String.format("%s/s%s/%s", BuildConfig.IMAGE_CACHE_URL, mac, posterUrlTvdb)
        } else {
            null
        }
    }

    @Synchronized
    private fun encodeImageUrl(
        @Suppress("SameParameterValue") key: String,
        data: String
    ): String? {
        try {
            if (sha256_hmac == null) {
                sha256_hmac = Mac.getInstance("HmacSHA256")
                val secretKey = SecretKeySpec(key.toByteArray(), "HmacSHA256")
                sha256_hmac!!.init(secretKey)
            }

            return Base64.encodeToString(
                sha256_hmac!!.doFinal(data.toByteArray()),
                Base64.NO_WRAP or Base64.URL_SAFE
            )
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e, "Signing image URL failed.")
            return null
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Signing image URL failed.")
            return null
        }

    }
}
