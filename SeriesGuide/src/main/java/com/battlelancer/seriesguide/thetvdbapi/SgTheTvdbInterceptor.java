package com.battlelancer.seriesguide.thetvdbapi;

import android.support.annotation.NonNull;
import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.TheTvdbInterceptor;
import dagger.Lazy;
import java.io.IOException;
import javax.inject.Inject;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link TheTvdbInterceptor} which does not require a {@link
 * com.uwetrottmann.thetvdb.TheTvdb} instance until intercepting.
 */
public class SgTheTvdbInterceptor implements Interceptor {

    private final Lazy<TheTvdb> theTvdb;

    @Inject
    public SgTheTvdbInterceptor(Lazy<TheTvdb> theTvdb) {
        this.theTvdb = theTvdb;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return TheTvdbInterceptor.handleIntercept(chain, theTvdb.get().jsonWebToken());
    }
}
