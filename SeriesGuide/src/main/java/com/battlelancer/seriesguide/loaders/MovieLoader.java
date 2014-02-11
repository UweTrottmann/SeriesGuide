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
import android.database.Cursor;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Images;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.tmdb.Tmdb;
import java.util.Date;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

/**
 * Tries to load movie details from the local database, when failing connects to trakt and TMDb to
 * get them.
 */
public class MovieLoader extends GenericSimpleLoader<Movie> {

    private int mTmdbId;

    public MovieLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public Movie loadInBackground() {
        // movie in local database?
        Cursor movieQuery = getContext().getContentResolver()
                .query(Movies.buildMovieUri(mTmdbId), MovieQuery.PROJECTION, null, null, null);
        if (movieQuery == null || !movieQuery.moveToFirst() || movieQuery.getCount() < 1) {
            if (movieQuery != null) {
                movieQuery.close();
            }
            // load from network instead
            return loadFromNetwork();
        }

        // map to movie object
        Movie movie = new Movie();
        movie.tmdbId = mTmdbId;
        movie.title = movieQuery.getString(MovieQuery.TITLE);
        movie.overview = movieQuery.getString(MovieQuery.OVERVIEW);
        movie.released = new Date(movieQuery.getLong(MovieQuery.RELEASED_UTC_MS));
        movie.images = new Images();
        movie.images.poster = movieQuery.getString(MovieQuery.POSTER);
        movie.watched = DBUtils.restoreBooleanFromInt(movieQuery.getInt(MovieQuery.WATCHED));
        movie.inCollection = DBUtils.restoreBooleanFromInt(
                movieQuery.getInt(MovieQuery.IN_COLLECTION));
        movie.inWatchlist = DBUtils.restoreBooleanFromInt(
                movieQuery.getInt(MovieQuery.IN_WATCHLIST));

        return movie;
    }

    private Movie loadFromNetwork() {
        if (!AndroidUtils.isNetworkConnected(getContext())) {
            return null;
        }

        String languageCode = DisplaySettings.getContentLanguage(getContext());
        Tmdb tmdb = ServiceUtils.getTmdb(getContext());
        Trakt trakt = ServiceUtils.getTraktWithAuth(getContext());
        if (trakt == null) {
            // fall back to unauthenticated version
            trakt = ServiceUtils.getTrakt(getContext());
        }

        return MovieTools.Download.getMovie(trakt.movieService(), tmdb.moviesService(),
                languageCode, mTmdbId);
    }

    private interface MovieQuery {

        public String[] PROJECTION = {
                Movies.TITLE,
                Movies.OVERVIEW,
                Movies.RELEASED_UTC_MS,
                Movies.POSTER,
                Movies.WATCHED,
                Movies.IN_COLLECTION,
                Movies.IN_WATCHLIST
        };

        int TITLE = 0;
        int OVERVIEW = 1;
        int RELEASED_UTC_MS = 2;
        int POSTER = 3;
        int WATCHED = 4;
        int IN_COLLECTION = 5;
        int IN_WATCHLIST = 6;
    }
}
