package com.battlelancer.seriesguide.settings;

import android.content.Context;
import android.preference.PreferenceManager;

public class TmdbSettings {

    public static final String KEY_TMDB_BASE_URL = "com.battlelancer.seriesguide.tmdb.baseurl";

    public static final String POSTER_SIZE_SPEC_W154 = "w154";

    public static final String POSTER_SIZE_SPEC_W342 = "w342";

    private static final String DEFAULT_BASE_URL = "https://image.tmdb.org/t/p/";

    public static String getImageBaseUrl(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_TMDB_BASE_URL, DEFAULT_BASE_URL);
    }

}
