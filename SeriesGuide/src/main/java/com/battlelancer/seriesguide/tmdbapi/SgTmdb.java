/*
 * Copyright 2014 Uwe Trottmann
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
import com.uwetrottmann.tmdb.Tmdb;
import retrofit.RestAdapter;
import retrofit.client.OkClient;

public class SgTmdb extends Tmdb {

    private final Context context;

    public SgTmdb(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected RestAdapter.Builder newRestAdapterBuilder() {
        return new RestAdapter.Builder().setClient(
                new OkClient(ServiceUtils.getCachingOkHttpClient(context)));
    }
}
