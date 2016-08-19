package com.battlelancer.seriesguide.util;

import android.content.Context;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.uwetrottmann.thetvdb.TheTvdb;
import com.uwetrottmann.thetvdb.TheTvdbAuthenticator;
import com.uwetrottmann.trakt5.TraktV2;
import java.io.IOException;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import timber.log.Timber;

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
        String host = response.request().url().host();
        if (TheTvdb.API_HOST.equals(host)) {
            Timber.d("TVDB auth failed.");
            return TheTvdbAuthenticator.handleRequest(response, ServiceUtils.getTheTvdb(context));
        }
        if (TraktV2.API_HOST.equals(host)) {
            return handleTraktAuth(response);
        }
        return null;
    }

    private Request handleTraktAuth(Response response) {
        Timber.d("trakt auth failed.");

        if (responseCount(response) >= 2) {
            Timber.d("trakt auth failed 2 times, give up.");
            return null;
        }

        // verify the auth header contains our access token
        TraktCredentials credentials = TraktCredentials.get(context);
        String authHeader = "Bearer " + credentials.getAccessToken();
        if (authHeader.equals(response.request().header(TraktV2.HEADER_AUTHORIZATION))) {
            // refresh the token
            boolean successful = credentials
                    .refreshAccessToken(ServiceUtils.getTraktNoTokenRefresh(context));

            if (successful) {
                // retry the request
                return response.request().newBuilder()
                        .header(TraktV2.HEADER_AUTHORIZATION,
                                "Bearer " + credentials.getAccessToken())
                        .build();
            }
        }
        return null;
    }

    private static int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
