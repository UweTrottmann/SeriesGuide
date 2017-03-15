package com.battlelancer.seriesguide.thetvdbapi;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ImageView;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import timber.log.Timber;

public class TvdbImageTools {

    private static final String TVDB_MIRROR_BANNERS = "http://thetvdb.com/banners/";
    private static final String TVDB_MIRROR_BANNERS_CACHE = TVDB_MIRROR_BANNERS + "_cache/";
    private static Mac sha256_hmac;

    // prevent init
    private TvdbImageTools() {
    }

    /**
     * Builds a full size url for a TVDb poster or screenshot (episode still) using the given image
     * path.
     *
     * <p>Posters probably should use {@link #smallSizeUrl(String)} which downloads a much smaller
     * version.
     */
    @Nullable
    public static String fullSizeUrl(@Nullable String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            return null;
        } else {
            return buildImageCacheUrl(TVDB_MIRROR_BANNERS + imagePath);
        }
    }

    /**
     * Builds a full url for a TVDb show poster using the given image path.
     */
    @Nullable
    public static String smallSizeUrl(@Nullable String imagePath) {
        if (TextUtils.isEmpty(imagePath)) {
            return null;
        } else {
            return buildImageCacheUrl(TVDB_MIRROR_BANNERS_CACHE + imagePath);
        }
    }

    /**
     * Builds a full url for a TVDb show poster using the given image path.
     *
     * @param imagePath If empty, will return the URL of the first uploaded poster.
     */
    @Nullable
    public static String smallSizeOrFirstUrl(@Nullable String imagePath, int showTvdbId) {
        if (TextUtils.isEmpty(imagePath)) {
            imagePath = firstPosterPath(showTvdbId);
        }
        return smallSizeUrl(imagePath);
    }

    /**
     * Builds a fall-back path for a TVDb show poster using the TVDB id, equals the first image
     * uploaded.
     */
    @NonNull
    public static String firstPosterPath(int showTvdbId) {
        return "posters/" + showTvdbId + "-1.jpg";
    }

    /**
     * Tries to load the given TVDb show poster into the given {@link ImageView}
     * without any resizing or cropping.
     */
    public static void loadShowPoster(Context context, ImageView imageView, String posterPath) {
        ServiceUtils.loadWithPicasso(context, smallSizeUrl(posterPath))
                .noFade()
                .into(imageView);
    }

    /**
     * Tries to load the given TVDb show poster into the given {@link ImageView} without any
     * resizing or cropping. In addition sets alpha on the view.
     */
    public static void loadShowPosterAlpha(Context context, ImageView imageView,
            String posterPath) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            imageView.setImageAlpha(30);
        } else {
            //noinspection deprecation
            imageView.setAlpha(30);
        }

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
            String posterPath) {
        ServiceUtils.loadWithPicasso(context, smallSizeUrl(posterPath))
                .resizeDimen(R.dimen.show_poster_width, R.dimen.show_poster_height)
                .centerCrop()
                .error(R.drawable.ic_image_missing)
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
            String posterUrl) {
        ServiceUtils.loadWithPicasso(context, posterUrl)
                .resizeDimen(R.dimen.show_poster_width_default, R.dimen.show_poster_height_default)
                .centerCrop()
                .error(R.drawable.ic_image_missing)
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
            String posterPath) {
        ServiceUtils.loadWithPicasso(context, smallSizeUrl(posterPath))
                .fit()
                .centerCrop()
                .error(R.drawable.ic_image_missing)
                .into(imageView);
    }

    /**
     * @param posterUrlTvdb Expected to be not empty.
     */
    @Nullable
    private static String buildImageCacheUrl(@NonNull String posterUrlTvdb) {
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
