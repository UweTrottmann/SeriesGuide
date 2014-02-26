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
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.entities.Trailers;
import com.uwetrottmann.tmdb.services.MoviesService;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads a list of movie trailers from TMDb.
 */
public class MovieTrailersLoader extends GenericSimpleLoader<Trailers> {

    private int mTmdbId;

    public MovieTrailersLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public Trailers loadInBackground() {
        try {
            MoviesService movieService = ServiceUtils.getTmdb(getContext()).moviesService();
            return movieService.trailers(mTmdbId);
        } catch (RetrofitError e) {
            Timber.e(e, "Downloading movie trailers failed");
        }

        return null;
    }
}
