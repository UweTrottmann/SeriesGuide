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

import com.battlelancer.seriesguide.loaders.TmdbMovieDetailsLoader.MovieDetails;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.Trailers;
import com.uwetrottmann.tmdb.services.MoviesService;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import retrofit.RetrofitError;

/**
 * Loads details, trailers and cast of a movie from TMDb wrapped in a {@link MovieDetails} object.
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
        String languageCode = DisplaySettings.getContentLanguage(getContext());

        try {
            MoviesService movieService = ServiceUtils.getTmdbServiceManager(getContext())
                    .moviesService();

            MovieDetails details = new MovieDetails();
            details.movie(movieService.summary(mTmdbId, languageCode));

            if (TextUtils.isEmpty(details.movie().overview)) {
                // fall back to English content
                details.movie(movieService.summary(mTmdbId));
            }

            details.trailers(movieService.trailers(mTmdbId));

            return details;

        } catch (RetrofitError e) {
            Utils.trackException(getContext(), TAG, e);
            Log.w(TAG, e);
        }

        return null;
    }

    public static class MovieDetails {

        private Movie mMovie;

        private Trailers mTrailers;

        private Credits mCredits;

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

        public Credits credits() {
            return mCredits;
        }

        public MovieDetails credits(Credits credits) {
            mCredits = credits;
            return this;
        }
    }

}
