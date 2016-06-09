/*
 * Copyright 2016 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        TraktV2 trakt = ServiceUtils.getTrakt(context);
        return TraktV2Interceptor.handleIntercept(chain, trakt.apiKey(), trakt.accessToken());
    }
}
