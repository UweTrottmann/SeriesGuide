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

import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Movie;
import com.jakewharton.trakt.enumerations.Extended;
import com.jakewharton.trakt.services.MovieService;
import com.jakewharton.trakt.services.UserService;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import retrofit.RetrofitError;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import static com.battlelancer.seriesguide.sync.SgSyncAdapter.UpdateResult;

public class MovieTools {

    public static void addToCollection(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (!Utils.isConnected(context, true)) {
                return;
            }
            // add to trakt collection
            AndroidUtils.executeAsyncTask(
                    new TraktTask(context).collectionAddMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        addToList(context, movieTmdbId, Movies.IN_COLLECTION, AddMovieTask.AddTo.COLLECTION);
    }

    public static void addToWatchlist(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (!Utils.isConnected(context, true)) {
                return;
            }
            // add to trakt watchlist
            AndroidUtils.executeAsyncTask(
                    new TraktTask(context).watchlistMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        addToList(context, movieTmdbId, Movies.IN_WATCHLIST, AddMovieTask.AddTo.WATCHLIST);
    }

    private static void addToList(Context context, int movieTmdbId, String listColumn,
            AddMovieTask.AddTo list) {
        // do we have this movie in the database already?
        Boolean movieExists = isMovieExists(context, movieTmdbId);
        if (movieExists == null) {
            return;
        }
        if (movieExists) {
            updateMovie(context, movieTmdbId, listColumn, true);
        } else {
            addMovieAsync(context, movieTmdbId, list);
        }
    }

    public static void removeFromCollection(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (!Utils.isConnected(context, true)) {
                return;
            }
            // remove from trakt collection
            AndroidUtils.executeAsyncTask(
                    new TraktTask(context).collectionRemoveMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        Boolean isInWatchlist = isMovieInList(context, movieTmdbId, Movies.IN_WATCHLIST);
        removeFromList(context, movieTmdbId, isInWatchlist, Movies.IN_COLLECTION);
    }

    public static void removeFromWatchlist(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (!Utils.isConnected(context, true)) {
                return;
            }
            // remove from trakt watchlist
            AndroidUtils.executeAsyncTask(
                    new TraktTask(context).unwatchlistMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        Boolean isInCollection = isMovieInList(context, movieTmdbId, Movies.IN_COLLECTION);
        removeFromList(context, movieTmdbId, isInCollection, Movies.IN_WATCHLIST);
    }

    private static void removeFromList(Context context, int movieTmdbId, Boolean isInOtherList,
            String listColumn) {
        if (isInOtherList == null) {
            return;
        }
        if (isInOtherList) {
            // just update list flag
            updateMovie(context, movieTmdbId, listColumn, false);
        } else {
            // completely remove from database
            deleteMovie(context, movieTmdbId);
        }
    }

    private static void addMovieAsync(Context context, int movieTmdbId, AddMovieTask.AddTo addTo) {
        new AddMovieTask(context, addTo).execute(movieTmdbId);
    }

    private static ContentValues[] buildMoviesContentValues(List<Movie> movies) {
        ContentValues[] valuesArray = new ContentValues[movies.size()];
        int index = 0;
        for (Movie movie : movies) {
            valuesArray[index] = buildMovieContentValues(movie);
            index++;
        }
        return valuesArray;
    }

    private static ContentValues buildMovieContentValues(Movie movie) {
        ContentValues values = buildBasicMovieContentValues(movie);

        values.put(Movies.IN_COLLECTION, convertBooleanToInt(movie.inCollection));
        values.put(Movies.IN_WATCHLIST, convertBooleanToInt(movie.inWatchlist));

        return values;
    }

    /**
     * Extracts basic properties, except in_watchlist and in_collection.
     */
    private static ContentValues buildBasicMovieContentValues(Movie movie) {
        ContentValues values = new ContentValues();

        values.put(Movies.TMDB_ID, movie.tmdbId);
        values.put(Movies.TITLE, movie.title);
        values.put(Movies.RELEASED_UTC_MS, movie.released.getTime());
        values.put(Movies.WATCHED, convertBooleanToInt(movie.watched));
        values.put(Movies.POSTER, movie.images == null ? "" : movie.images.poster);

        return values;
    }

    private static int convertBooleanToInt(Boolean value) {
        if (value == null) {
            return 0;
        }
        return value ? 1 : 0;
    }

    private static void deleteMovie(Context context, int movieTmdbId) {
        context.getContentResolver().delete(Movies.buildMovieUri(movieTmdbId), null, null);
    }

    /**
     * Returns a set of the TMDb ids of all movies in the local database.
     *
     * @return null if there was an error, empty list if there are no movies.
     */
    private static HashSet<Integer> getMovieTmdbIdsAsSet(Context context) {
        HashSet<Integer> localMoviesIds = new HashSet<>();

        Cursor movies = context.getContentResolver().query(Movies.CONTENT_URI,
                new String[]{Movies._ID, Movies.TMDB_ID}, null, null, null);
        if (movies == null) {
            return null;
        }

        while (movies.moveToNext()) {
            localMoviesIds.add(movies.getInt(1));
        }

        movies.close();

        return localMoviesIds;
    }

    /**
     * Determines if the given movie is in the list determined by the given database column name.
     *
     * @return true if the movie is in the given list, false otherwise. Can return {@code null} if
     * the database could not be queried or the movie does not exist.
     */
    private static Boolean isMovieInList(Context context, int movieTmdbId, String listColumn) {
        Cursor movie = context.getContentResolver().query(Movies.buildMovieUri(movieTmdbId),
                new String[]{listColumn}, null, null, null);
        if (movie == null || !movie.moveToFirst()) {
            return null;
        }

        boolean isInList = movie.getInt(0) == 1;

        movie.close();

        return isInList;
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

    private static void updateMovie(Context context, int movieTmdbId, String column,
            boolean value) {
        ContentValues values = new ContentValues();
        values.put(column, value);
        context.getContentResolver().update(Movies.buildMovieUri(movieTmdbId), values, null, null);
    }

    private static class AddMovieTask extends AsyncTask<Integer, Void, Void> {

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
        protected Void doInBackground(Integer... params) {
            int movieTmdbId = params[0];

            // get summary from trakt
            Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
            if (trakt == null) {
                // fall back
                trakt = ServiceUtils.getTrakt(mContext);
            }

            Movie movie;
            try {
                movie = trakt.movieService().summary(movieTmdbId);
            } catch (RetrofitError e) {
                // didn't work :(
                return null;
            }

            // store in database
            ContentValues values = buildBasicMovieContentValues(movie);
            values.put(Movies.IN_COLLECTION, mAddTo == AddTo.COLLECTION ?
                    1 : convertBooleanToInt(movie.inCollection));
            values.put(Movies.IN_WATCHLIST, mAddTo == AddTo.WATCHLIST ?
                    1 : convertBooleanToInt(movie.inWatchlist));

            mContext.getContentResolver().insert(Movies.CONTENT_URI, values);

            return null;
        }

    }

    public static class Download {

        /**
         * Updates the movie local database against trakt movie watchlist and collection, therefore
         * adds, updates and removes movies in the database.<br/>Performs <b>synchronous network
         * access</b>, so make sure to run this on a background thread!
         */
        public static UpdateResult syncMoviesFromTrakt(Context context) {
            Trakt trakt = ServiceUtils.getTraktWithAuth(context);
            if (trakt == null) {
                // trakt is not connected, we are done here
                return UpdateResult.SUCCESS;
            }
            UserService userService = trakt.userService();

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            HashSet<Integer> localMovies = getMovieTmdbIdsAsSet(context);
            HashSet<Integer> moviesToRemove = new HashSet<>(localMovies);
            HashSet<Integer> moviesToAdd = new HashSet<>();
            ArrayList<ContentProviderOperation> batch = new ArrayList<>();

            // get trakt watchlist
            List<Movie> watchlistMovies;
            try {
                watchlistMovies = userService
                        .watchlistMovies(TraktCredentials.get(context).getUsername());
            } catch (RetrofitError e) {
                return UpdateResult.INCOMPLETE;
            }

            // build watchlist updates
            ContentValues values = new ContentValues();
            values.put(Movies.IN_WATCHLIST, true);
            buildMovieUpdateOps(watchlistMovies, localMovies, moviesToAdd, moviesToRemove, batch,
                    values);

            // apply watchlist updates
            DBUtils.applyInSmallBatches(context, batch);
            batch.clear();
            values.clear();

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            // get trakt collection
            List<Movie> collectionMovies;
            try {
                collectionMovies = userService.libraryMoviesCollection(
                        TraktCredentials.get(context).getUsername(), Extended.MIN);
            } catch (RetrofitError e) {
                return UpdateResult.INCOMPLETE;
            }

            // build collection updates
            values.put(Movies.IN_COLLECTION, true);
            buildMovieUpdateOps(collectionMovies, localMovies, moviesToAdd, moviesToRemove, batch,
                    values);

            // apply collection updates
            DBUtils.applyInSmallBatches(context, batch);
            batch.clear();

            // merge on first run, delete on consequent runs
            if (TraktSettings.hasMergedMovies(context)) {
                // remove movies not on trakt
                buildMovieDeleteOps(moviesToRemove, batch);
                DBUtils.applyInSmallBatches(context, batch);
            } else {
                // upload movies not on trakt
                UpdateResult result = Upload.uploadMovies(context, trakt, moviesToRemove);
                if (result != UpdateResult.SUCCESS) {
                    // abort here if there were issues
                    return result;
                }
            }

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            // add movies new from trakt
            return addMovies(context, trakt, moviesToAdd.toArray(new Integer[moviesToAdd.size()]));
        }

        /**
         * Downloads movie summaries from trakt and adds them to the database.
         */
        private static UpdateResult addMovies(Context context, Trakt trakt,
                Integer... movieTmdbIds) {
            MovieService movieService = trakt.movieService();
            List<Movie> movies = new LinkedList<>();

            for (int i = 0; i < movieTmdbIds.length; i++) {
                // get summary from trakt
                try {
                    Movie movie = movieService.summary(movieTmdbIds[i]);
                    movies.add(movie);
                } catch (RetrofitError e) {
                    return UpdateResult.INCOMPLETE;
                }

                // process in batches of at most 10
                if (i % 10 == 0 || i == movieTmdbIds.length - 1) {
                    // insert into database
                    context.getContentResolver()
                            .bulkInsert(Movies.CONTENT_URI, buildMoviesContentValues(movies));

                    // reset
                    movies.clear();
                }
            }

            return UpdateResult.SUCCESS;
        }

        private static void buildMovieUpdateOps(List<Movie> remoteMovies,
                HashSet<Integer> localMovies, HashSet<Integer> moviesToAdd,
                HashSet<Integer> moviesToRemove, ArrayList<ContentProviderOperation> batch,
                ContentValues values) {
            for (Movie movie : remoteMovies) {
                if (localMovies.contains(movie.tmdbId)) {
                    // update existing movie
                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(Movies.buildMovieUri(movie.tmdbId))
                            .withValues(values).build();
                    batch.add(op);

                    // prevent movie from getting removed
                    moviesToRemove.remove(movie.tmdbId);
                } else {
                    // insert new movie
                    moviesToAdd.add(movie.tmdbId);
                }
            }
        }

        private static void buildMovieDeleteOps(HashSet<Integer> moviesToRemove,
                ArrayList<ContentProviderOperation> batch) {
            for (Integer movieTmdbId : moviesToRemove) {
                ContentProviderOperation op = ContentProviderOperation
                        .newDelete(Movies.buildMovieUri(movieTmdbId)).build();
                batch.add(op);
            }
        }

    }

    private static class Upload {

        /**
         * Uploads the given movies to the appropriate list(s) on trakt.
         */
        public static UpdateResult uploadMovies(Context context, Trakt trakt,
                HashSet<Integer> moviesToUpload) {
            if (moviesToUpload.size() == 0) {
                // nothing to upload
                return UpdateResult.SUCCESS;
            }

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            Cursor localMovies = context.getContentResolver().query(Movies.CONTENT_URI,
                    new String[]{Movies._ID, Movies.TMDB_ID, Movies.IN_COLLECTION,
                            Movies.IN_WATCHLIST}, null, null, null);
            if (localMovies == null) {
                return UpdateResult.INCOMPLETE;
            }

            // build list of collected, watchlisted movies to upload
            List<MovieService.SeenMovie> moviesToCollect = new LinkedList<>();
            List<MovieService.SeenMovie> moviesToWatchlist = new LinkedList<>();
            while (localMovies.moveToNext()) {
                int tmdbId = localMovies.getInt(1);
                if (!moviesToUpload.contains(tmdbId)) {
                    continue;
                }

                MovieService.SeenMovie movie = new MovieService.SeenMovie(tmdbId);

                // in collection?
                if (localMovies.getInt(2) == 1) {
                    moviesToCollect.add(movie);
                }
                // in watchlist?
                if (localMovies.getInt(3) == 1) {
                    moviesToWatchlist.add(movie);
                }
            }

            // clean up
            localMovies.close();

            // upload
            try {
                MovieService movieService = trakt.movieService();
                if (moviesToCollect.size() > 0) {
                    movieService.library(new MovieService.Movies(moviesToCollect));
                }
                if (moviesToWatchlist.size() > 0) {
                    movieService.watchlist(new MovieService.Movies(moviesToWatchlist));
                }
            } catch (RetrofitError e) {
                return UpdateResult.INCOMPLETE;
            }

            // flag that we ran a successful merge
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, true).commit();

            return UpdateResult.SUCCESS;
        }

    }

}
