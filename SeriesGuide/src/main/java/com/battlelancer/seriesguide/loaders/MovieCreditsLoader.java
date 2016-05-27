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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb2.entities.Credits;
import java.io.IOException;
import retrofit2.Response;

/**
 * Loads movie credits from TMDb.
 */
public class MovieCreditsLoader extends GenericSimpleLoader<Credits> {

    private final int mTmdbId;

    public MovieCreditsLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public Credits loadInBackground() {
        try {
            Response<Credits> response = ServiceUtils.getTmdb(getContext())
                    .moviesService()
                    .credits(mTmdbId)
                    .execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTmdb.trackFailedRequest(getContext(), "get movie credits", response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(getContext(), "get movie credits", e);
        }

        return null;
    }
}
