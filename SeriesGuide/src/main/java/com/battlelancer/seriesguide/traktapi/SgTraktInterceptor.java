package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.TraktV2Interceptor;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * A custom {@link com.uwetrottmann.trakt5.TraktV2Interceptor} which does not require a {@link
 * com.uwetrottmann.trakt5.TraktV2} instance until intercepting.
 */
public class SgTraktInterceptor implements Interceptor {

    private final Context context;

    public SgTraktInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        TraktV2 trakt = ServiceUtils.getTraktNoTokenRefresh(context);
        return TraktV2Interceptor.handleIntercept(chain, trakt.apiKey(), trakt.accessToken());
    }
}
