/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.loaders;

import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.Tmdb;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.ResultsPage;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import retrofit.RetrofitError;

/**
 * Loads a list of movies from TMDb.
 */
public class TmdbMoviesLoader extends GenericSimpleLoader<List<Movie>> {

    private static final String TAG = "TmdbMoviesLoader";

    private String mQuery;

    public TmdbMoviesLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<Movie> loadInBackground() {
        Tmdb tmdb = ServiceUtils.getTmdbServiceManager(getContext());

        try {
            ResultsPage page;

            if (TextUtils.isEmpty(mQuery)) {
                page = tmdb.moviesService().nowPlaying();
            } else {
                page = tmdb.searchService().movie(mQuery);
            }
            if (page != null && page.results != null) {
                return page.results;
            }
        } catch (RetrofitError e) {
            Utils.trackException(getContext(), TAG, e);
            Log.w(TAG, e);
        }

        return null;
    }

}
