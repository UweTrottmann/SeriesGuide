package com.battlelancer.seriesguide.util;

import android.content.Context;
import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.TheTvdbAuthenticator;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * An {@link Authenticator} that can handle auth for all APIs used with our shared {@link
 * ServiceUtils#getCachingOkHttpClient(Context)}.
 */
public class AllApisAuthenticator implements Authenticator {

    private Context context;

    public AllApisAuthenticator(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Request authenticate(Route route, Response response) throws IOException {
        if (TheTvdb.API_HOST.equals(response.request().url().host())) {
            return TheTvdbAuthenticator.handleRequest(response, ServiceUtils.getTheTvdb(context));
        }
        return null;
    }
}
