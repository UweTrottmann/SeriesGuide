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
import android.text.TextUtils;
import com.battlelancer.seriesguide.loaders.TmdbMovieDetailsLoader.MovieDetails;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.Trailers;
import com.uwetrottmann.tmdb.services.MoviesService;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads details, trailers and cast of a movie from TMDb wrapped in a {@link MovieDetails} object.
 */
public class TmdbMovieDetailsLoader extends GenericSimpleLoader<MovieDetails> {

    private int mTmdbId;

    public TmdbMovieDetailsLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public MovieDetails loadInBackground() {
        String languageCode = DisplaySettings.getContentLanguage(getContext());

        try {
            MoviesService movieService = ServiceUtils.getTmdb(getContext())
                    .moviesService();

            MovieDetails details = new MovieDetails();
            details.tmdbMovie(movieService.summary(mTmdbId, languageCode));

            if (TextUtils.isEmpty(details.tmdbMovie().overview)) {
                // fall back to English content
                details.tmdbMovie(movieService.summary(mTmdbId));
            }

            details.trailers(movieService.trailers(mTmdbId));

            return details;

        } catch (RetrofitError e) {
            Timber.e(e, "Downloading movie info failed");
        }

        return null;
    }

    public static class MovieDetails {

        private com.jakewharton.trakt.entities.Movie mTraktMovie;

        private Movie mTmdbMovie;

        private Trailers mTrailers;

        private Credits mCredits;

        public com.jakewharton.trakt.entities.Movie traktOrLocalMovie() {
            return mTraktMovie;
        }

        public MovieDetails traktMovie(com.jakewharton.trakt.entities.Movie traktMovie) {
            mTraktMovie = traktMovie;
            return this;
        }

        public Movie tmdbMovie() {
            return mTmdbMovie;
        }

        public MovieDetails tmdbMovie(Movie movie) {
            mTmdbMovie = movie;
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
