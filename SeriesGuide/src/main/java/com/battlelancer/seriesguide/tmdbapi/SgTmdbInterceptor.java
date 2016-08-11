package com.battlelancer.seriesguide.tmdbapi;

import com.battlelancer.seriesguide.BuildConfig;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.TmdbInterceptor;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link TmdbInterceptor} which does not require a {@link Tmdb} instance until
 * intercepting.
 */
public class SgTmdbInterceptor implements Interceptor {

    public SgTmdbInterceptor() {
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return TmdbInterceptor.handleIntercept(chain, BuildConfig.TMDB_API_KEY);
    }
}
