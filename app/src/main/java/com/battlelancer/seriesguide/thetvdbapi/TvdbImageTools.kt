package com.battlelancer.seriesguide.thetvdbapi

import android.content.Context
import android.widget.ImageView
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.util.ImageTools
import com.battlelancer.seriesguide.util.ServiceUtils
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler

object TvdbImageTools {

    private const val TVDB_MIRROR_BANNERS = "https://artworks.thetvdb.com/banners/"
    private const val TVDB_LEGACY_MIRROR_BANNERS = "https://www.thetvdb.com/banners/"
    const val TVDB_THUMBNAIL_POSTFIX = "_t.jpg"
    const val TVDB_LEGACY_CACHE_PREFIX = "_cache/"

    /**
     * Builds a url for a TVDb poster or screenshot (episode still) using the given image path.
     */
    @JvmStatic
    @Deprecated("Use ImageTools instead")
    fun artworkUrl(imagePath: String?): String? {
        return if (imagePath.isNullOrEmpty()) {
            null
        } else {
            // If the path contains the legacy cache prefix, use the www subdomain as it has
            // a redirect to the new thumbnail URL set up (artworks subdomain + file name postfix).
            // E.g. https://www.thetvdb.com/banners/_cache/posters/example.jpg redirects to
            // https://artworks.thetvdb.com/banners/posters/example_t.jpg
            // Using the artworks subdomain with the legacy cache prefix is not supported.
            val imageUrl = if (imagePath.contains(TVDB_LEGACY_CACHE_PREFIX, false)) {
                "$TVDB_LEGACY_MIRROR_BANNERS$imagePath"
            } else {
                "$TVDB_MIRROR_BANNERS$imagePath"
            }
            ImageTools.buildImageCacheUrl(imageUrl)
        }
    }

    /**
     * Builds a full url for a TVDb show poster using the given image path.
     *
     * @param imagePath If empty, will return an URL that will be resolved to the highest rated
     * small poster using additional network requests.
     */
    @JvmStatic
    @Deprecated("Use ImageTools instead")
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
    @Deprecated("Use ImageTools instead")
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

}
