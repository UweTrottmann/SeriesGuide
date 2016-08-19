package com.battlelancer.seriesguide.tmdbapi;

import android.content.Context;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.tmdb2.Tmdb;
import okhttp3.OkHttpClient;
import retrofit2.Response;

/**
 * Custom {@link Tmdb} which uses the app OkHttp instance.
 */
public class SgTmdb extends Tmdb {

    private static final String TAG_TMDB_ERROR = "TMDB Error";

    private final OkHttpClient okHttpClient;

    /**
     * Create a new manager instance.
     *
     * @param apiKey Your TMDB API key.
     */
    public SgTmdb(OkHttpClient okHttpClient, String apiKey) {
        super(apiKey);
        this.okHttpClient = okHttpClient;
    }

    @Override
    protected synchronized OkHttpClient okHttpClient() {
        return okHttpClient;
    }

    public static void trackFailedRequest(Context context, String action, Response response) {
        Utils.trackFailedRequest(context, TAG_TMDB_ERROR, action, response);
    }

    public static void trackFailedRequest(Context context, String action, Throwable throwable) {
        Utils.trackFailedRequest(context, TAG_TMDB_ERROR, action, throwable);
    }
}
