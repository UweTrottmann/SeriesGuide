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

package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

public class MovieTools {

    public static void addToWatchlist(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (!Utils.isConnected(context, true)) {
                return;
            }
            AndroidUtils.executeAsyncTask(
                    new TraktTask(context, null).watchlistMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        Boolean movieExists = isMovieExists(context, movieTmdbId);
        if (movieExists == null) {
            return;
        }
        if (movieExists) {
            updateMovie(context, movieTmdbId, Movies.IN_WATCHLIST, true);
        } else {
            addMovieAsync(context, movieTmdbId, AddMovieTask.AddTo.WATCHLIST);
        }
    }

    public static void removeFromWatchlist(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (!Utils.isConnected(context, true)) {
                return;
            }
            AndroidUtils.executeAsyncTask(
                    new TraktTask(context, null).unwatchlistMovie(movieTmdbId)
            );
        }

        Boolean isInCollection = isMovieInCollection(context, movieTmdbId);
        if (isInCollection == null) {
            return;
        }
        if (isInCollection) {
            // just update watchlist flag
            updateMovie(context, movieTmdbId, Movies.IN_WATCHLIST, false);
        } else {
            // remove from database
            deleteMovie(context, movieTmdbId);
        }
    }

    private static void addMovieAsync(Context context, int movieTmdbId, AddMovieTask.AddTo addTo) {
        new AddMovieTask(context, addTo).execute(movieTmdbId);
    }

    private static class AddMovieTask extends AsyncTask<Integer, Void, Integer> {

        private final Context mContext;

        private final AddTo mAddTo;

        public enum AddTo {
            COLLECTION,
            WATCHLIST
        }

        public AddMovieTask(Context context, AddTo addTo) {
            mContext = context;
            mAddTo = addTo;
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            int movieTmdbId = params[0];

            // get summary from trakt
            Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
            if (trakt == null) {
                // fall back
                trakt = ServiceUtils.getTrakt(mContext);
            }

            Movie movie = trakt.movieService().summary(movieTmdbId);

            // store in database
            ContentValues values = new ContentValues();
            values.put(Movies.TMDB_ID, movie.tmdbId);
            values.put(Movies.TITLE, movie.title);
            values.put(Movies.RELEASED_UTC_MS, movie.released.getTime());
            values.put(Movies.WATCHED, convertBooleanToInt(movie.watched));
            values.put(Movies.POSTER, movie.images == null ? "" : movie.images.poster);
            values.put(Movies.IN_COLLECTION, mAddTo == AddTo.COLLECTION ?
                    1 : convertBooleanToInt(movie.inCollection));
            values.put(Movies.IN_WATCHLIST, mAddTo == AddTo.WATCHLIST ?
                    1 : convertBooleanToInt(movie.inWatchlist));

            mContext.getContentResolver().insert(Movies.CONTENT_URI, values);

            return movieTmdbId;
        }

        private static int convertBooleanToInt(Boolean value) {
            if (value == null) {
                return 0;
            }
            return value ? 1 : 0;
        }
    }

    private static Boolean isMovieInCollection(Context context, int movieTmdbId) {
        Cursor movie = context.getContentResolver().query(Movies.buildMovieUri(movieTmdbId),
                new String[]{Movies.IN_COLLECTION}, null, null, null);
        if (movie == null || !movie.moveToFirst()) {
            return null;
        }

        boolean isInCollection = movie.getInt(0) == 1;

        movie.close();

        return isInCollection;
    }

    private static Boolean isMovieExists(Context context, int movieTmdbId) {
        Cursor movie = context.getContentResolver().query(Movies.CONTENT_URI, new String[]{
                Movies._ID}, Movies.TMDB_ID + "=" + movieTmdbId, null, null);
        if (movie == null) {
            return null;
        }

        boolean movieExists = movie.getCount() > 0;

        movie.close();

        return movieExists;
    }

    private static void deleteMovie(Context context, int movieTmdbId) {
        context.getContentResolver().delete(Movies.buildMovieUri(movieTmdbId), null, null);
    }

    private static void updateMovie(Context context, int movieTmdbId, String column,
            boolean value) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        context.getContentResolver().update(Movies.buildMovieUri(movieTmdbId), values, null, null);
    }

//    public static class Download {
//
//        public static void syncMoviesFromTrakt(Context context) {
//            Trakt trakt = ServiceUtils.getTraktWithAuth(context);
//            if (trakt == null) {
//                return;
//            }
//
//            HashSet<Integer> localMovies = getMovieTmdbIdsAsSet(context);
//
//            // integrate trakt movie watchlist
//            List<Movie> watchlistMovies = trakt.userService()
//                    .watchlistMovies(TraktCredentials.get(context).getUsername());
//
//            DBUtils.applyInSmallBatches(context, buildMovieOps(watchlistMovies,
//                    localMovies));
//
//            // integrate trakt movie collection
//            List<Movie> collectionMovies = trakt.userService()
//                    .libraryMoviesCollection(TraktCredentials.get(context).getUsername(),
//                            Extended.EXTENDED);
//
//            DBUtils.applyInSmallBatches(context,
//                    buildMovieOps(collectionMovies, localMovies));
//        }
//
//        private static ArrayList<ContentProviderOperation> buildMovieOps(
//                List<Movie> remoteMovies, HashSet<Integer> localMovies) {
//            ArrayList<ContentProviderOperation> batch = new ArrayList<>();
//
//            for (Movie movie : remoteMovies) {
//                if (localMovies.contains(movie.tmdbId)) {
//                    // update existing movie
//
//                } else {
//                    // insert new movie
//                    localMovies.add(movie.tmdbId);
//                }
//            }
//
//            return batch;
//        }
//
//    }
//
//    /**
//     * Returns a set of the TMDb ids of all movies in the local database.
//     *
//     * @return null if there was an error, empty list if there are no movies.
//     */
//    public static HashSet<Integer> getMovieTmdbIdsAsSet(Context context) {
//        HashSet<Integer> localMoviesIds = new HashSet<>();
//
//        Cursor movies = context.getContentResolver().query(Movies.CONTENT_URI,
//                new String[]{Movies._ID, Movies.TMDB_ID}, null, null, null);
//        if (movies == null) {
//            return null;
//        }
//
//        while (movies.moveToNext()) {
//            localMoviesIds.add(movies.getInt(1));
//        }
//
//        movies.close();
//
//        return localMoviesIds;
//    }

}
