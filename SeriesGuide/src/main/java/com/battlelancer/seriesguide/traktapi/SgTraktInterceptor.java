package com.battlelancer.seriesguide.traktapi;

import com.battlelancer.seriesguide.SgApp;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.TraktV2Interceptor;
import dagger.Lazy;
import java.io.IOException;
import javax.inject.Inject;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link com.uwetrottmann.trakt5.TraktV2Interceptor} which does not require a {@link
 * com.uwetrottmann.trakt5.TraktV2} instance until intercepting.
 */
public class SgTraktInterceptor implements Interceptor {

    @Inject Lazy<TraktV2> trakt;

    public SgTraktInterceptor(SgApp app) {
        app.getServicesComponent().inject(this);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        return TraktV2Interceptor.handleIntercept(chain, trakt.get().apiKey(),
                trakt.get().accessToken());
    }
}
