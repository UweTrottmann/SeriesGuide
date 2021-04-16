package com.battlelancer.seriesguide.util;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.uwetrottmann.trakt5.TraktV2;
import dagger.Lazy;
import javax.inject.Inject;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import timber.log.Timber;

/**
 * An {@link Authenticator} that can handle auth for all APIs used with our shared {@link
 * com.battlelancer.seriesguide.modules.HttpClientModule}.
 */
public class AllApisAuthenticator implements Authenticator {

    private final Context context;
    private final Lazy<TraktV2> trakt;

    @Inject
    public AllApisAuthenticator(@ApplicationContext Context context, Lazy<TraktV2> trakt) {
        this.context = context;
        this.trakt = trakt;
    }

    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {
        String host = response.request().url().host();
        if (TraktV2.API_HOST.equals(host)) {
            return handleTraktAuth(response);
        }
        return null;
    }

    private Request handleTraktAuth(Response response) {
        Timber.d("trakt requires auth.");

        if (responseCount(response) >= 2) {
            Timber.d("trakt auth failed 2 times, give up.");
            return null;
        }

        // verify that we have existing credentials
        TraktCredentials credentials = TraktCredentials.get(context);
        if (credentials.hasCredentials()) {
            // refresh the token
            boolean successful = credentials.refreshAccessToken(trakt.get());

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
