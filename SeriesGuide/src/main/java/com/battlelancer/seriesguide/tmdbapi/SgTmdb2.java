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

package com.battlelancer.seriesguide.tmdbapi;

import android.content.Context;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.tmdb2.Tmdb;
import okhttp3.OkHttpClient;

/**
 * Custom {@link Tmdb} which uses the app OkHttp instance.
 */
public class SgTmdb2 extends Tmdb {

    private final Context context;

    /**
     * Create a new manager instance.
     *
     * @param apiKey Your TMDB API key.
     */
    public SgTmdb2(Context context, String apiKey) {
        super(apiKey);
        this.context = context.getApplicationContext();
    }

    @Override
    protected synchronized OkHttpClient okHttpClient() {
        return ServiceUtils.getCachingOkHttpClient(context);
    }
}
