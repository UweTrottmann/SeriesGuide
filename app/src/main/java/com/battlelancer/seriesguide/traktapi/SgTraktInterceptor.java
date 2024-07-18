// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2024 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi;

import androidx.annotation.NonNull;
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

    private final Lazy<SgTrakt> trakt;

    @Inject
    public SgTraktInterceptor(Lazy<SgTrakt> trakt) {
        this.trakt = trakt;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return TraktV2Interceptor.handleIntercept(chain, trakt.get().apiKey(),
                trakt.get().accessToken());
    }
}
