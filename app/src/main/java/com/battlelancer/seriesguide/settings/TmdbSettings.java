package com.battlelancer.seriesguide.settings;

import android.content.Context;
import androidx.preference.PreferenceManager;

public class TmdbSettings {

    public static final String KEY_TMDB_BASE_URL = "com.battlelancer.seriesguide.tmdb.baseurl";

    public static final String POSTER_SIZE_SPEC_W154 = "w154";

    public static final String POSTER_SIZE_SPEC_W342 = "w342";

    private static final String STILL_SIZE_SPEC_W300 = "w300";

    public static final String IMAGE_SIZE_SPEC_ORIGINAL = "original";

    private static final String DEFAULT_BASE_URL = "https://image.tmdb.org/t/p/";

    public static String getImageBaseUrl(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_TMDB_BASE_URL, DEFAULT_BASE_URL);
    }

    public static String getImageOriginalUrl(Context context, String path) {
        return getImageBaseUrl(context) + TmdbSettings.IMAGE_SIZE_SPEC_ORIGINAL + path;
    }

    /**
     * Returns base image URL based on screen density.
     */
    public static String getPosterBaseUrl(Context context) {
        if (DisplaySettings.isVeryHighDensityScreen(context)) {
            return TmdbSettings.getImageBaseUrl(context) + TmdbSettings.POSTER_SIZE_SPEC_W342;
        } else {
            return TmdbSettings.getImageBaseUrl(context) + TmdbSettings.POSTER_SIZE_SPEC_W154;
        }
    }

    public static String getStillUrl(Context context, String path) {
        return getImageBaseUrl(context) + TmdbSettings.STILL_SIZE_SPEC_W300 + path;
    }
}
