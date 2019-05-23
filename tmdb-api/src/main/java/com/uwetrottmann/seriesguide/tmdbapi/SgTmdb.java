package com.uwetrottmann.seriesguide.tmdbapi;

import androidx.annotation.NonNull;
import com.uwetrottmann.tmdb2.Tmdb;
import okhttp3.OkHttpClient;

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

    @NonNull
    @Override
    protected synchronized OkHttpClient okHttpClient() {
        return okHttpClient;
    }
}
