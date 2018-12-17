package com.battlelancer.seriesguide.tmdbapi;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.AnalyticsEvents;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.tmdb2.Tmdb;
import okhttp3.OkHttpClient;
import retrofit2.Response;

/**
 * Custom {@link Tmdb} which uses the app OkHttp instance.
 */
public class SgTmdb extends Tmdb {

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
        Utils.trackFailedRequest(context, AnalyticsEvents.TMDB_ERROR, action, response.code(),
                response.message());
    }

    public static void trackFailedRequest(Context context, String action,
            @NonNull Throwable throwable) {
        Utils.trackFailedRequest(context, AnalyticsEvents.TMDB_ERROR, action, throwable);
    }
}
