package com.battlelancer.seriesguide.tmdbapi;

import android.content.Context;
import com.battlelancer.seriesguide.util.ServiceUtils;
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

    private Context context;

    public SgTmdbInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return TmdbInterceptor.handleIntercept(chain, ServiceUtils.getTmdb(context).apiKey());
    }
}
