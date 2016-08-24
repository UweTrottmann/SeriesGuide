package com.battlelancer.seriesguide.thetvdbapi;

import com.battlelancer.seriesguide.SgApp;
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

    @Inject Lazy<TheTvdb> theTvdb;

    public SgTheTvdbInterceptor(SgApp app) {
        app.getServicesComponent().inject(this);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return TheTvdbInterceptor.handleIntercept(chain, theTvdb.get().jsonWebToken());
    }
}
