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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.MovieDetailsFragment;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.services.MovieService;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.services.MoviesService;
import java.util.Date;
import retrofit.RetrofitError;
import timber.log.Timber;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

/**
 * Tries to load current movie details from trakt and TMDb, if failing tries to fall back to local
 * database copy.
 */
public class MovieLoader extends GenericSimpleLoader<MovieDetailsFragment.MovieDetails> {

    private int mTmdbId;

    public MovieLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public MovieDetailsFragment.MovieDetails loadInBackground() {
        MovieDetailsFragment.MovieDetails details = new MovieDetailsFragment.MovieDetails();

        // try loading from trakt and tmdb
        if (AndroidUtils.isNetworkConnected(getContext())) {
            details.traktMovie(loadFromTrakt(getContext(), mTmdbId));
            details.tmdbMovie(loadFromTmdb(getContext(), mTmdbId));
        }

        // update local database
        updateLocalMovie(getContext(), details, mTmdbId);

        // fill in details from local database
        Cursor movieQuery = getContext().getContentResolver()
                .query(Movies.buildMovieUri(mTmdbId), MovieQuery.PROJECTION, null, null, null);
        if (movieQuery == null || !movieQuery.moveToFirst() || movieQuery.getCount() < 1) {
            if (movieQuery != null) {
                movieQuery.close();
            }
            // ensure flags are all false on failure
            // (assumption: movie is not in db, it has the truth, so can't be in any lists)
            if (details.traktMovie() == null) {
                details.traktMovie(new Movie());
            }
            details.traktMovie().watched = false;
            details.traktMovie().inCollection = false;
            details.traktMovie().inWatchlist = false;
            return details;
        }

        // map to objects
        if (details.traktMovie() == null) {
            details.traktMovie(new Movie());
            details.traktMovie().released = new Date(
                    movieQuery.getLong(MovieQuery.RELEASED_UTC_MS));
            details.traktMovie().imdb_id = movieQuery.getString(MovieQuery.IMDB_ID);
        }
        // prefer local state for watched, collected and watchlist status
        // assumption: local db has the truth
        details.traktMovie().watched = DBUtils.restoreBooleanFromInt(
                movieQuery.getInt(MovieQuery.WATCHED));
        details.traktMovie().inCollection = DBUtils.restoreBooleanFromInt(
                movieQuery.getInt(MovieQuery.IN_COLLECTION));
        details.traktMovie().inWatchlist = DBUtils.restoreBooleanFromInt(
                movieQuery.getInt(MovieQuery.IN_WATCHLIST));

        if (details.tmdbMovie() == null) {
            details.tmdbMovie(new com.uwetrottmann.tmdb.entities.Movie());
            details.tmdbMovie().title = movieQuery.getString(MovieQuery.TITLE);
            details.tmdbMovie().overview = movieQuery.getString(MovieQuery.OVERVIEW);
            details.tmdbMovie().poster_path = movieQuery.getString(MovieQuery.POSTER);
        }

        // clean up
        movieQuery.close();

        return details;
    }

    private static Movie loadFromTrakt(Context context, int movieTmdbId) {
        Trakt trakt = ServiceUtils.getTraktWithAuth(context);
        if (trakt == null) {
            trakt = ServiceUtils.getTrakt(context);
        }
        MovieService movieService = trakt.movieService();
        try {
            return movieService.summary(movieTmdbId);
        } catch (RetrofitError e) {
            Timber.e(e, "Loading trakt movie summary failed");
            return null;
        }
    }

    private static com.uwetrottmann.tmdb.entities.Movie loadFromTmdb(Context context,
            int movieTmdbId) {
        MoviesService moviesService = ServiceUtils.getTmdb(context).moviesService();
        String languageCode = DisplaySettings.getContentLanguage(context);

        try {
            com.uwetrottmann.tmdb.entities.Movie movie = moviesService.summary(movieTmdbId,
                    languageCode);
            // check if there actually is local content, fall back to English
            if (TextUtils.isEmpty(movie.title) || TextUtils.isEmpty(movie.overview)) {
                movie = moviesService.summary(movieTmdbId);
            }
            return movie;
        } catch (RetrofitError e) {
            Timber.e(e, "Loading TMDb movie summary failed");
            return null;
        }
    }

    private static void updateLocalMovie(Context context,
            MovieDetailsFragment.MovieDetails details, int tmdbId) {
        ContentValues values = new ContentValues();

        if (details.traktMovie() != null) {
            values.put(Movies.RELEASED_UTC_MS, details.traktMovie().released.getTime());
            values.put(Movies.IMDB_ID, details.traktMovie().imdb_id);
        }
        if (details.tmdbMovie() != null) {
            values.put(Movies.TITLE, details.tmdbMovie().title);
            values.put(Movies.OVERVIEW, details.tmdbMovie().overview);
            values.put(Movies.POSTER, details.tmdbMovie().poster_path);
        }

        // if movie does not exist in database, will do nothing
        context.getContentResolver().update(Movies.buildMovieUri(tmdbId), values, null, null);
    }

    private interface MovieQuery {

        public String[] PROJECTION = {
                Movies.TITLE,
                Movies.OVERVIEW,
                Movies.RELEASED_UTC_MS,
                Movies.POSTER,
                Movies.WATCHED,
                Movies.IN_COLLECTION,
                Movies.IN_WATCHLIST,
                Movies.IMDB_ID
        };

        int TITLE = 0;
        int OVERVIEW = 1;
        int RELEASED_UTC_MS = 2;
        int POSTER = 3;
        int WATCHED = 4;
        int IN_COLLECTION = 5;
        int IN_WATCHLIST = 6;
        int IMDB_ID = 7;
    }
}
