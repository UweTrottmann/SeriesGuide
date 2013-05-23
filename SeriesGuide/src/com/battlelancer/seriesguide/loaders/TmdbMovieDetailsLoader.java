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

import android.content.Context;
import android.util.Log;

import com.battlelancer.seriesguide.loaders.TmdbMovieDetailsLoader.MovieDetails;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.apibuilder.ApiException;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.ServiceManager;
import com.uwetrottmann.tmdb.TmdbException;
import com.uwetrottmann.tmdb.entities.Casts;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.Trailers;

/**
 * Loads details, trailers and cast of a movie from TMDb wrapped in a
 * {@link MovieDetails} object.
 */
public class TmdbMovieDetailsLoader extends GenericSimpleLoader<MovieDetails> {

    private static final String TAG = "TmdbMovieDetailsLoader";

    private int mTmdbId;

    public TmdbMovieDetailsLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public MovieDetails loadInBackground() {
        ServiceManager manager = ServiceUtils.getTmdbServiceManager(getContext());

        try {
            MovieDetails details = new MovieDetails();
            details.movie(manager.moviesService().summary(mTmdbId).fire());
            details.trailers(manager.moviesService().trailers(mTmdbId).fire());
            details.casts(manager.moviesService().casts(mTmdbId).fire());
            return details;
        } catch (TmdbException e) {
            Utils.trackException(getContext(), TAG, e);
            Log.w(TAG, e);
        } catch (ApiException e) {
            Utils.trackException(getContext(), TAG, e);
            Log.w(TAG, e);
        }

        return null;
    }

    public static class MovieDetails {
        private Movie mMovie;
        private Trailers mTrailers;
        private Casts mCasts;

        public Movie movie() {
            return mMovie;
        }

        public MovieDetails movie(Movie movie) {
            mMovie = movie;
            return this;
        }

        public Trailers trailers() {
            return mTrailers;
        }

        public MovieDetails trailers(Trailers trailers) {
            mTrailers = trailers;
            return this;
        }

        public Casts casts() {
            return mCasts;
        }

        public MovieDetails casts(Casts casts) {
            mCasts = casts;
            return this;
        }
    }

}
