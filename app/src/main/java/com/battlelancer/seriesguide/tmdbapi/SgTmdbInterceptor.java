package com.battlelancer.seriesguide.tmdbapi;

import android.support.annotation.NonNull;
import com.uwetrottmann.tmdb2.Tmdb;
import com.uwetrottmann.tmdb2.TmdbInterceptor;
import dagger.Lazy;
import java.io.IOException;
import javax.inject.Inject;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link TmdbInterceptor} which does not require a {@link Tmdb} instance until
 * intercepting.
 */
public class SgTmdbInterceptor implements Interceptor {

    private final Lazy<Tmdb> tmdb;

    @Inject
    public SgTmdbInterceptor(Lazy<Tmdb> tmdb) {
        this.tmdb = tmdb;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return TmdbInterceptor.handleIntercept(chain, tmdb.get());
    }
}
