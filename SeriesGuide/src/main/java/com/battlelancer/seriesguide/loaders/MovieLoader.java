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
import com.battlelancer.seriesguide.items.MovieDetails;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.MovieTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.entities.Ratings;
import org.joda.time.DateTime;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

/**
 * Tries to load current movie details from trakt and TMDb, if failing tries to fall back to local
 * database copy.
 */
public class MovieLoader extends GenericSimpleLoader<MovieDetails> {

    private int mTmdbId;

    public MovieLoader(Context context, int tmdbId) {
        super(context);
        mTmdbId = tmdbId;
    }

    @Override
    public MovieDetails loadInBackground() {
        MovieDetails details = new MovieDetails();

        // try loading from trakt and tmdb
        if (AndroidUtils.isNetworkConnected(getContext())) {
            details = MovieTools.Download.getMovieDetails(getContext(), mTmdbId);
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
            // ensure list flags are false on failure
            // (assumption: movie is not in db, it has the truth, so can't be in any lists)
            details.inCollection = false;
            details.inWatchlist = false;
            return details;
        }

        // set local state for watched, collected and watchlist status
        // assumption: local db has the truth for these
        details.inCollection = DBUtils.restoreBooleanFromInt(
                movieQuery.getInt(MovieQuery.IN_COLLECTION));
        details.inWatchlist = DBUtils.restoreBooleanFromInt(
                movieQuery.getInt(MovieQuery.IN_WATCHLIST));
        details.watched = DBUtils.restoreBooleanFromInt(movieQuery.getInt(MovieQuery.WATCHED));
        // also use local state of user rating
        details.userRating = movieQuery.getInt(MovieQuery.RATING_USER);

        // only overwrite other info if remote data failed to load
        if (details.released == null) {
            details.released = new DateTime(movieQuery.getLong(MovieQuery.RELEASED_UTC_MS));
        }
        if (details.traktRatings() == null) {
            details.traktRatings(new Ratings());
            details.traktRatings().rating = (double) movieQuery.getInt(MovieQuery.RATING_TRAKT);
            details.traktRatings().votes = movieQuery.getInt(MovieQuery.RATING_VOTES_TRAKT);
        }
        if (details.tmdbMovie() == null) {
            details.tmdbMovie(new com.uwetrottmann.tmdb.entities.Movie());
            details.tmdbMovie().imdb_id = movieQuery.getString(MovieQuery.IMDB_ID);
            details.tmdbMovie().title = movieQuery.getString(MovieQuery.TITLE);
            details.tmdbMovie().overview = movieQuery.getString(MovieQuery.OVERVIEW);
            details.tmdbMovie().poster_path = movieQuery.getString(MovieQuery.POSTER);
            details.tmdbMovie().runtime = movieQuery.getInt(MovieQuery.RUNTIME_MIN);
            details.tmdbMovie().vote_average = movieQuery.getDouble(MovieQuery.RATING_TMDB);
        }

        // clean up
        movieQuery.close();

        return details;
    }

    private static void updateLocalMovie(Context context,
            MovieDetails details, int tmdbId) {
        ContentValues values = MovieTools.buildBasicMovieContentValues(details);
        if (values.size() == 0) {
            // nothing to update, downloading probably failed :(
            return;
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
                Movies.IMDB_ID,
                Movies.RUNTIME_MIN,
                Movies.RATING_TMDB,
                Movies.RATING_TRAKT,
                Movies.RATING_VOTES_TRAKT,
                Movies.RATING_USER
        };

        int TITLE = 0;
        int OVERVIEW = 1;
        int RELEASED_UTC_MS = 2;
        int POSTER = 3;
        int WATCHED = 4;
        int IN_COLLECTION = 5;
        int IN_WATCHLIST = 6;
        int IMDB_ID = 7;
        int RUNTIME_MIN = 8;
        int RATING_TMDB = 9;
        int RATING_TRAKT = 10;
        int RATING_VOTES_TRAKT = 11;
        int RATING_USER = 12;
    }
}
