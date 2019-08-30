package com.battlelancer.seriesguide.thetvdbapi;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.SgPicassoRequestHandler;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import timber.log.Timber;

public class TvdbImageTools {

    private static final String TVDB_MIRROR_BANNERS = "https://www.thetvdb.com/banners/";
    public static final String TVDB_CACHE_PREFIX = "_cache/";
    private static Mac sha256_hmac;

    // prevent init
    private TvdbImageTools() {
    }

    /**
     * Builds a url for a TVDb poster or screenshot (episode still) using the given image path.
     */
    @Nullable
    public static String artworkUrl(@Nullable String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            return null;
        } else {
            return buildImageCacheUrl(TVDB_MIRROR_BANNERS + imagePath);
        }
    }

    /**
     * Builds a full url for a TVDb show poster using the given image path.
     *
     * @param imagePath If empty, will return an URL that will be resolved to the highest rated
     *                  small poster using additional network requests.
     */
    @Nullable
    public static String posterUrlOrResolve(@Nullable String imagePath, int showTvdbId,
            @Nullable String language) {
        if (TextUtils.isEmpty(imagePath)) {
            String url = SgPicassoRequestHandler.SCHEME_SHOW_TVDB + "://" + showTvdbId;
            if (!TextUtils.isEmpty(language)) {
                url += "?" + SgPicassoRequestHandler.QUERY_LANGUAGE + "=" + language;
            }
            return url;
        }
        return artworkUrl(imagePath);
    }

    /**
     * Tries to load the given TVDb show poster into the given {@link ImageView}
     * without any resizing or cropping.
     */
    public static void loadShowPoster(Context context, ImageView imageView,
            @Nullable String posterPath) {
        ServiceUtils.loadWithPicasso(context, artworkUrl(posterPath))
                .noFade()
                .into(imageView);
    }

    /**
     * Tries to load the given TVDb show poster into the given {@link ImageView} without any
     * resizing or cropping. In addition sets alpha on the view.
     */
    public static void loadShowPosterAlpha(Context context, ImageView imageView,
            @Nullable String posterPath) {
        imageView.setImageAlpha(30);
        loadShowPoster(context, imageView, posterPath);
    }

    /**
     * Tries to load a resized, center cropped version of the show poster into the given {@link
     * ImageView}. On failure displays an error drawable (ensure image view is set to center
     * inside).
     *
     * <p>The resize dimensions are those used for posters in the show list and change depending on
     * screen size.
     */
    public static void loadShowPosterResizeCrop(Context context, ImageView imageView,
            @Nullable String posterPath) {
        ServiceUtils.loadWithPicasso(context, artworkUrl(posterPath))
                .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                .centerCrop()
                .error(R.drawable.ic_photo_gray_24dp)
                .into(imageView);
    }

    public static void loadUrlResizeCrop(Context context, ImageView imageView,
            @Nullable String url) {
        ServiceUtils.loadWithPicasso(context, url)
                .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                .centerCrop()
                .error(R.drawable.ic_photo_gray_24dp)
                .into(imageView);
    }

    /**
     * Tries to load a resized, center cropped version of the show poster into the given {@link
     * ImageView}. On failure displays an error drawable (ensure image view is set to center
     * inside).
     *
     * <p>The resize dimensions are fixed for all screen sizes. Like for items using the show list
     * layout, use {@link TvdbImageTools#loadShowPosterResizeCrop(Context, ImageView, String)}.
     *
     * @param posterUrl This should already be a built TVDB poster URL, not just a poster path!
     */
    public static void loadShowPosterResizeSmallCrop(Context context, ImageView imageView,
            @Nullable String posterUrl) {
        ServiceUtils.loadWithPicasso(context, posterUrl)
                .resizeDimen(R.dimen.show_poster_width_default, R.dimen.show_poster_height_default)
                .centerCrop()
                .error(R.drawable.ic_photo_gray_24dp)
                .into(imageView);
    }

    /**
     * Tries to load a resized, center cropped version of the show poster into the given {@link
     * ImageView}. On failure displays an error drawable (ensure image view is set to center
     * inside).
     *
     * <p>The resize dimensions are determined based on the image view size.
     */
    public static void loadShowPosterFitCrop(Context context, ImageView imageView,
            @Nullable String posterPath) {
        ServiceUtils.loadWithPicasso(context, artworkUrl(posterPath))
                .fit()
                .centerCrop()
                .error(R.drawable.ic_photo_gray_24dp)
                .into(imageView);
    }

    /**
     * @param posterUrlTvdb Expected to be not empty.
     */
    @Nullable
    private static String buildImageCacheUrl(@NonNull String posterUrlTvdb) {
        //noinspection ConstantConditions
        if (BuildConfig.IMAGE_CACHE_URL == null) {
            return posterUrlTvdb; // no cache
        }

        String mac = encodeImageUrl(BuildConfig.IMAGE_CACHE_SECRET, posterUrlTvdb);
        if (mac != null) {
            return String.format("%s/s%s/%s", BuildConfig.IMAGE_CACHE_URL, mac, posterUrlTvdb);
        } else {
            return null;
        }
    }

    @Nullable
    private static synchronized String encodeImageUrl(@NonNull String key, @NonNull String data) {
        try {
            if (sha256_hmac == null) {
                sha256_hmac = Mac.getInstance("HmacSHA256");
                SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
                sha256_hmac.init(secret_key);
            }

            return Base64.encodeToString(sha256_hmac.doFinal(data.getBytes()),
                    Base64.NO_WRAP | Base64.URL_SAFE);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Timber.e(e, "Signing image URL failed.");
            return null;
        }
    }
}
