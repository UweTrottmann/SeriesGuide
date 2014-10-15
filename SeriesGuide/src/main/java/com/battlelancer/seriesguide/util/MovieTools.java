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

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.MovieDetails;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.tasks.HexagonAddMovieToCollectionTask;
import com.battlelancer.seriesguide.util.tasks.HexagonAddMovieToWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.HexagonRemoveMovieFromCollectionTask;
import com.battlelancer.seriesguide.util.tasks.HexagonRemoveMovieFromWatchlistTask;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList;
import com.uwetrottmann.tmdb.services.MoviesService;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseMovie;
import com.uwetrottmann.trakt.v2.entities.Movie;
import com.uwetrottmann.trakt.v2.entities.MovieIds;
import com.uwetrottmann.trakt.v2.entities.Ratings;
import com.uwetrottmann.trakt.v2.entities.SearchResult;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncMovie;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.IdType;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Movies;
import com.uwetrottmann.trakt.v2.services.Search;
import com.uwetrottmann.trakt.v2.services.Sync;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import retrofit.RetrofitError;
import timber.log.Timber;

import static com.battlelancer.seriesguide.sync.SgSyncAdapter.UpdateResult;

public class MovieTools {

    private static final int MOVIES_MAX_BATCH_SIZE = 100;

    public static class MovieChangedEvent {
        public int movieTmdbId;

        public MovieChangedEvent(int movieTmdbId) {
            this.movieTmdbId = movieTmdbId;
        }
    }

    public static void addToCollection(Context context, int movieTmdbId) {
        if (HexagonTools.isSignedIn(context)) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            AndroidUtils.executeOnPool(
                    new HexagonAddMovieToCollectionTask(context, movieTmdbId)
            );
        }
        if (TraktCredentials.get(context).hasCredentials()) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            // add to trakt collection
            AndroidUtils.executeOnPool(
                    new TraktTask(context).collectionAddMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        addToList(context, movieTmdbId, SeriesGuideContract.Movies.IN_COLLECTION,
                AddMovieTask.AddTo.COLLECTION);
    }

    public static void addToWatchlist(Context context, int movieTmdbId) {
        if (HexagonTools.isSignedIn(context)) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            AndroidUtils.executeOnPool(
                    new HexagonAddMovieToWatchlistTask(context, movieTmdbId)
            );
        }
        if (TraktCredentials.get(context).hasCredentials()) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            // add to trakt watchlist
            AndroidUtils.executeOnPool(
                    new TraktTask(context).watchlistMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        addToList(context, movieTmdbId, SeriesGuideContract.Movies.IN_WATCHLIST,
                AddMovieTask.AddTo.WATCHLIST);
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
        if (HexagonTools.isSignedIn(context)) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            AndroidUtils.executeOnPool(
                    new HexagonRemoveMovieFromCollectionTask(context, movieTmdbId)
            );
        }
        if (TraktCredentials.get(context).hasCredentials()) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            // remove from trakt collection
            AndroidUtils.executeOnPool(
                    new TraktTask(context).collectionRemoveMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        Boolean isInWatchlist = isMovieInList(context, movieTmdbId,
                SeriesGuideContract.Movies.IN_WATCHLIST);
        removeFromList(context, movieTmdbId, isInWatchlist,
                SeriesGuideContract.Movies.IN_COLLECTION);
    }

    public static void removeFromWatchlist(Context context, int movieTmdbId) {
        if (HexagonTools.isSignedIn(context)) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            AndroidUtils.executeOnPool(
                    new HexagonRemoveMovieFromWatchlistTask(context, movieTmdbId)
            );
        }
        if (TraktCredentials.get(context).hasCredentials()) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            // remove from trakt watchlist
            AndroidUtils.executeOnPool(
                    new TraktTask(context).unwatchlistMovie(movieTmdbId)
            );
        }

        // make modifications to local database
        Boolean isInCollection = isMovieInList(context, movieTmdbId,
                SeriesGuideContract.Movies.IN_COLLECTION);
        removeFromList(context, movieTmdbId, isInCollection,
                SeriesGuideContract.Movies.IN_WATCHLIST);
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

    public static void watchedMovie(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            // remove from trakt watchlist
            AndroidUtils.executeOnPool(
                    new TraktTask(context).watchedMovie(movieTmdbId)
            );
        }

        // try updating local movie (if any)
        updateMovie(context, movieTmdbId, SeriesGuideContract.Movies.WATCHED, true);
    }

    public static void unwatchedMovie(Context context, int movieTmdbId) {
        if (TraktCredentials.get(context).hasCredentials()) {
            if (Utils.isNotConnected(context, true)) {
                return;
            }
            // remove from trakt watchlist
            AndroidUtils.executeOnPool(
                    new TraktTask(context).unwatchedMovie(movieTmdbId)
            );
        }

        // try updating local movie (if any)
        updateMovie(context, movieTmdbId, SeriesGuideContract.Movies.WATCHED, false);
    }

    private static void addMovieAsync(Context context, int movieTmdbId, AddMovieTask.AddTo addTo) {
        Utils.executeInOrder(new AddMovieTask(context, addTo), movieTmdbId);
    }

    private static ContentValues[] buildMoviesContentValues(List<MovieDetails> movies) {
        ContentValues[] valuesArray = new ContentValues[movies.size()];
        int index = 0;
        for (MovieDetails movie : movies) {
            valuesArray[index] = buildMovieContentValues(movie);
            index++;
        }
        return valuesArray;
    }

    private static ContentValues buildMovieContentValues(MovieDetails details) {
        ContentValues values = buildBasicMovieContentValuesWithId(details);

        values.put(SeriesGuideContract.Movies.IN_COLLECTION,
                DBUtils.convertBooleanToInt(details.inCollection));
        values.put(SeriesGuideContract.Movies.IN_WATCHLIST,
                DBUtils.convertBooleanToInt(details.inWatchlist));

        return values;
    }

    /**
     * Extracts basic properties, except in_watchlist and in_collection from trakt. Also includes
     * the TMDb id and watched state as value.
     */
    private static ContentValues buildBasicMovieContentValuesWithId(MovieDetails details) {
        ContentValues values = buildBasicMovieContentValues(details);
        values.put(SeriesGuideContract.Movies.TMDB_ID, details.tmdbMovie().id);
        return values;
    }

    /**
     * Extracts ratings and release time from trakt, all other properties from TMDb data.
     *
     * <p> If either movie data is null, will still extract the properties of others.
     */
    public static ContentValues buildBasicMovieContentValues(MovieDetails details) {
        ContentValues values = new ContentValues();

        // data from trakt
        if (details.released != null) {
            values.put(SeriesGuideContract.Movies.RELEASED_UTC_MS, details.released.getMillis());
        }
        if (details.traktRatings() != null) {
            values.put(SeriesGuideContract.Movies.RATING_TRAKT,
                    details.traktRatings().rating);
            values.put(SeriesGuideContract.Movies.RATING_VOTES_TRAKT,
                    details.traktRatings().votes);
        }

        // data from TMDb
        if (details.tmdbMovie() != null) {
            values.put(SeriesGuideContract.Movies.IMDB_ID, details.tmdbMovie().imdb_id);
            values.put(SeriesGuideContract.Movies.TITLE, details.tmdbMovie().title);
            values.put(SeriesGuideContract.Movies.TITLE_NOARTICLE,
                    DBUtils.trimLeadingArticle(details.tmdbMovie().title));
            values.put(SeriesGuideContract.Movies.OVERVIEW, details.tmdbMovie().overview);
            values.put(SeriesGuideContract.Movies.POSTER, details.tmdbMovie().poster_path);
            values.put(SeriesGuideContract.Movies.RUNTIME_MIN, details.tmdbMovie().runtime);
            values.put(SeriesGuideContract.Movies.RATING_TMDB, details.tmdbMovie().vote_average);
        }

        return values;
    }

    private static void deleteMovie(Context context, int movieTmdbId) {
        context.getContentResolver()
                .delete(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId), null, null);

        EventBus.getDefault().post(new MovieChangedEvent(movieTmdbId));
    }

    /**
     * Returns a set of the TMDb ids of all movies in the local database.
     *
     * @return null if there was an error, empty list if there are no movies.
     */
    private static HashSet<Integer> getMovieTmdbIdsAsSet(Context context) {
        HashSet<Integer> localMoviesIds = new HashSet<>();

        Cursor movies = context.getContentResolver().query(SeriesGuideContract.Movies.CONTENT_URI,
                new String[] { SeriesGuideContract.Movies._ID, SeriesGuideContract.Movies.TMDB_ID },
                null, null, null);
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
        Cursor movie = context.getContentResolver()
                .query(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId),
                        new String[] { listColumn }, null, null, null);
        if (movie == null || !movie.moveToFirst()) {
            return null;
        }

        boolean isInList = movie.getInt(0) == 1;

        movie.close();

        return isInList;
    }

    private static Boolean isMovieExists(Context context, int movieTmdbId) {
        Cursor movie = context.getContentResolver()
                .query(SeriesGuideContract.Movies.CONTENT_URI, new String[] {
                                SeriesGuideContract.Movies._ID },
                        SeriesGuideContract.Movies.TMDB_ID + "=" + movieTmdbId, null, null);
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
        context.getContentResolver()
                .update(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId), values, null, null);

        EventBus.getDefault().post(new MovieChangedEvent(movieTmdbId));
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

            // get movie info
            MovieDetails details = Download.getMovieDetails(mContext, movieTmdbId);
            if (details.tmdbMovie() == null || details.released == null) {
                // abort if minimal data failed to load
                return null;
            }

            // build values
            ContentValues values = buildBasicMovieContentValuesWithId(details);

            // set flags
            values.put(SeriesGuideContract.Movies.IN_COLLECTION,
                    DBUtils.convertBooleanToInt(mAddTo == AddTo.COLLECTION));
            values.put(SeriesGuideContract.Movies.IN_WATCHLIST,
                    DBUtils.convertBooleanToInt(mAddTo == AddTo.WATCHLIST));

            // add to database
            mContext.getContentResolver().insert(SeriesGuideContract.Movies.CONTENT_URI, values);

            return movieTmdbId;
        }

        @Override
        protected void onPostExecute(Integer movieTmdbId) {
            // guard against NPE https://github.com/UweTrottmann/SeriesGuide/issues/371
            if (movieTmdbId != null) {
                EventBus.getDefault().post(new MovieChangedEvent(movieTmdbId));
            }
        }
    }

    public static class Download {

        /**
         * Downloads movies from hexagon, updates existing movies with new properties, removes
         * movies that are neither in collection or watchlist.
         *
         * <p> Adds movie tmdb ids to the respective collection or watchlist set.
         */
        public static boolean fromHexagon(Context context,
                @Nonnull Set<Integer> newCollectionMovies, @Nonnull Set<Integer> newWatchlistMovies,
                boolean hasMergedMovies) {
            List<com.uwetrottmann.seriesguide.backend.movies.model.Movie> movies;
            boolean hasMoreMovies = true;
            String cursor = null;
            long currentTime = System.currentTimeMillis();
            DateTime lastSyncTime = new DateTime(HexagonSettings.getLastMoviesSyncTime(context));
            HashSet<Integer> localMovies = getMovieTmdbIdsAsSet(context);

            if (hasMergedMovies) {
                Timber.d("fromHexagon: downloading movies changed since " + lastSyncTime);
            } else {
                Timber.d("fromHexagon: downloading all movies");
            }

            while (hasMoreMovies) {
                // abort if connection is lost
                if (!AndroidUtils.isNetworkConnected(context)) {
                    Timber.e("fromHexagon: no network connection");
                    return false;
                }

                try {
                    com.uwetrottmann.seriesguide.backend.movies.Movies.Get request
                            = HexagonTools.getMoviesService(context).get()
                            .setLimit(MOVIES_MAX_BATCH_SIZE);
                    if (hasMergedMovies) {
                        request.setUpdatedSince(lastSyncTime);
                    }
                    if (!TextUtils.isEmpty(cursor)) {
                        request.setCursor(cursor);
                    }

                    MovieList response = request.execute();
                    if (response == null) {
                        // nothing more to do
                        Timber.d("fromHexagon: response was null, done here");
                        break;
                    }

                    movies = response.getMovies();

                    if (response.getCursor() != null) {
                        cursor = response.getCursor();
                    } else {
                        hasMoreMovies = false;
                    }
                } catch (IOException e) {
                    Timber.e(e, "fromHexagon: failed to download movies");
                    return false;
                }

                if (movies == null || movies.size() == 0) {
                    // nothing more to do
                    break;
                }

                ArrayList<ContentProviderOperation> batch = new ArrayList<>();
                for (com.uwetrottmann.seriesguide.backend.movies.model.Movie movie : movies) {
                    if (localMovies.contains(movie.getTmdbId())) {
                        // movie is in database
                        if (movie.getIsInCollection() != null && movie.getIsInWatchlist() != null
                                && !movie.getIsInCollection() && !movie.getIsInWatchlist()) {
                            // if neither in watchlist or collection: remove movie
                            batch.add(ContentProviderOperation.newDelete(
                                    SeriesGuideContract.Movies.buildMovieUri(movie.getTmdbId()))
                                    .build());
                        } else {
                            // update movie properties
                            ContentValues values = new ContentValues();
                            if (movie.getIsInCollection() != null) {
                                values.put(SeriesGuideContract.Movies.IN_COLLECTION,
                                        movie.getIsInCollection());
                            }
                            if (movie.getIsInWatchlist() != null) {
                                values.put(SeriesGuideContract.Movies.IN_WATCHLIST,
                                        movie.getIsInWatchlist());
                            }
                            batch.add(ContentProviderOperation.newUpdate(
                                    SeriesGuideContract.Movies.buildMovieUri(movie.getTmdbId()))
                                    .withValues(values).build());
                        }
                    } else {
                        // schedule movie to be added
                        if (movie.getIsInCollection() != null && movie.getIsInCollection()) {
                            newCollectionMovies.add(movie.getTmdbId());
                        }
                        if (movie.getIsInWatchlist() != null && movie.getIsInWatchlist()) {
                            newWatchlistMovies.add(movie.getTmdbId());
                        }
                    }
                }

                try {
                    DBUtils.applyInSmallBatches(context, batch);
                } catch (OperationApplicationException e) {
                    Timber.e(e, "fromHexagon: applying movie updates failed");
                    return false;
                }
            }

            // set new last sync time
            if (hasMergedMovies) {
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putLong(HexagonSettings.KEY_LAST_SYNC_MOVIES, currentTime)
                        .commit();
            }

            return true;
        }

        /**
         * Updates the movie local database against trakt movie watchlist and collection, therefore
         * adds, updates and removes movies in the database.<br/>Performs <b>synchronous network
         * access</b>, so make sure to run this on a background thread!
         */
        public static UpdateResult syncMoviesFromTrakt(Context context) {
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
            if (trakt == null) {
                // trakt is not connected, we are done here
                return UpdateResult.SUCCESS;
            }

            Sync sync = trakt.sync();

            HashSet<Integer> localMovies = getMovieTmdbIdsAsSet(context);
            HashSet<Integer> moviesToRemove = new HashSet<>(localMovies);
            Set<Integer> newCollectionMovies = new HashSet<>();
            Set<Integer> newWatchlistMovies = new HashSet<>();
            ArrayList<ContentProviderOperation> batch = new ArrayList<>();

            // get trakt watchlist
            List<BaseMovie> watchlistMovies;
            try {
                watchlistMovies = sync.watchlistMovies(Extended.DEFAULT_MIN);
            } catch (RetrofitError e) {
                return UpdateResult.INCOMPLETE;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return UpdateResult.INCOMPLETE;
            }
            if (watchlistMovies == null) {
                return UpdateResult.INCOMPLETE;
            }

            // build watchlist updates
            ContentValues values = new ContentValues();
            values.put(SeriesGuideContract.Movies.IN_WATCHLIST, true);
            buildMovieUpdateOps(watchlistMovies, localMovies, newWatchlistMovies, moviesToRemove,
                    batch, values);

            // apply watchlist updates
            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e(e, "Applying watchlist updates failed");
                return UpdateResult.INCOMPLETE;
            }
            batch.clear();
            values.clear();

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            // get trakt collection
            List<BaseMovie> collectionMovies;
            try {
                collectionMovies = sync.collectionMovies(Extended.DEFAULT_MIN);
            } catch (RetrofitError e) {
                return UpdateResult.INCOMPLETE;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return UpdateResult.INCOMPLETE;
            }
            if (collectionMovies == null) {
                return UpdateResult.INCOMPLETE;
            }

            // build collection updates
            values.put(SeriesGuideContract.Movies.IN_COLLECTION, true);
            buildMovieUpdateOps(collectionMovies, localMovies, newCollectionMovies, moviesToRemove,
                    batch, values);

            // apply collection updates
            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e(e, "Applying collection updates failed");
                return UpdateResult.INCOMPLETE;
            }
            batch.clear();

            // merge on first run, delete on consequent runs
            if (TraktSettings.hasMergedMovies(context)) {
                Timber.d("syncMoviesFromTrakt: remove " + moviesToRemove.size());
                // remove movies not on trakt
                buildMovieDeleteOps(moviesToRemove, batch);
                try {
                    DBUtils.applyInSmallBatches(context, batch);
                } catch (OperationApplicationException e) {
                    Timber.e(e, "Removing movies failed");
                    return UpdateResult.INCOMPLETE;
                }
            } else {
                // upload movies not on trakt
                UpdateResult result = Upload.toTrakt(context, sync, moviesToRemove);
                if (result != UpdateResult.SUCCESS) {
                    // abort here if there were issues
                    return result;
                } else {
                    // flag that we ran a successful merge
                    PreferenceManager.getDefaultSharedPreferences(context).edit()
                            .putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, true).commit();
                }
            }

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            // add movies new from trakt
            return addMovies(context, trakt, newCollectionMovies, newWatchlistMovies);
        }

        /**
         * Adds new movies to the database.
         *
         * @param trakt Requires a trakt with user auth, except when supplying movies from hexagon.
         * @param newCollectionMovies Movie TMDB ids to add to the collection.
         * @param newWatchlistMovies Movie TMDB ids to add to the watchlist.
         */
        public static UpdateResult addMovies(@Nonnull Context context, @Nonnull TraktV2 trakt,
                @Nonnull Set<Integer> newCollectionMovies,
                @Nonnull Set<Integer> newWatchlistMovies) {
            Timber.d("addMovies: " + newCollectionMovies.size() + " to collection, "
                    + newWatchlistMovies.size() + " to watchlist");

            // build a single list of tmdb ids
            Set<Integer> newMovies = new HashSet<>();
            for (Integer tmdbId : newCollectionMovies) {
                newMovies.add(tmdbId);
            }
            for (Integer tmdbId : newWatchlistMovies) {
                newMovies.add(tmdbId);
            }

            Search traktSearch = trakt.search();
            Movies traktMovies = trakt.movies();
            MoviesService tmdbMovies = ServiceUtils.getTmdb(context).moviesService();
            String languageCode = DisplaySettings.getContentLanguage(context);
            List<MovieDetails> movies = new LinkedList<>();

            // loop through ids
            for (Iterator<Integer> iterator = newMovies.iterator(); iterator.hasNext(); ) {
                int tmdbId = iterator.next();
                if (!AndroidUtils.isNetworkConnected(context)) {
                    Timber.e("addMovies: no network connection");
                    return UpdateResult.INCOMPLETE;
                }

                // download movie data
                MovieDetails movieDetails = getMovieDetails(traktSearch, traktMovies, tmdbMovies,
                        languageCode, tmdbId);
                if (movieDetails.tmdbMovie() == null || movieDetails.released == null) {
                    // skip if minimal values failed to load
                    Timber.d("addMovies: downloaded movie was incomplete, skipping");
                    continue;
                }

                // set flags
                movieDetails.inCollection = newCollectionMovies.contains(tmdbId);
                movieDetails.inWatchlist = newWatchlistMovies.contains(tmdbId);

                movies.add(movieDetails);

                // add to database in batches of at most 10
                if (movies.size() == 10 || !iterator.hasNext()) {
                    // insert into database
                    context.getContentResolver().bulkInsert(SeriesGuideContract.Movies.CONTENT_URI,
                            buildMoviesContentValues(movies));

                    // start new batch
                    movies.clear();
                }
            }

            return UpdateResult.SUCCESS;
        }

        /**
         * Download movie data from trakt and TMDb. If you plan on calling this multiple times, use
         * {@link #getMovieDetails(com.uwetrottmann.trakt.v2.services.Search,
         * com.uwetrottmann.trakt.v2.services.Movies, com.uwetrottmann.tmdb.services.MoviesService,
         * String, int)} instead.
         */
        public static MovieDetails getMovieDetails(Context context, int movieTmdbId) {
            // trakt
            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
            if (trakt == null) {
                trakt = ServiceUtils.getTraktV2(context);
            }
            Movies traktMovies = trakt.movies();
            Search traktSearch = trakt.search();

            // TMDb
            MoviesService tmdbMovies = ServiceUtils.getTmdb(context).moviesService();
            String languageCode = DisplaySettings.getContentLanguage(context);

            return getMovieDetails(traktSearch, traktMovies, tmdbMovies, languageCode, movieTmdbId);
        }

        /**
         * Download movie data from trakt and TMDb.
         */
        public static MovieDetails getMovieDetails(Search traktSearch, Movies traktMovies,
                MoviesService tmdbMovies, String languageCode, int movieTmdbId) {
            MovieDetails details = new MovieDetails();

            // load ratings and release time from trakt
            Integer movieTraktId = lookupTraktId(traktSearch, movieTmdbId);
            if (movieTraktId != null) {
                details.traktRatings(loadRatingsFromTrakt(traktMovies, movieTraktId));
                Movie movie = loadSummaryFromTrakt(traktMovies, movieTraktId);
                if (movie != null) {
                    details.released = movie.released;
                }
            }

            // load summary from tmdb
            details.tmdbMovie(loadSummaryFromTmdb(tmdbMovies, languageCode, movieTmdbId));

            return details;
        }

        private static Integer lookupTraktId(Search traktSearch, int movieTmdbId) {
            try {
                List<SearchResult> lookup = traktSearch.idLookup(IdType.TMDB,
                        String.valueOf(movieTmdbId));
                if (lookup == null || lookup.size() == 0) {
                    Timber.e("Finding trakt movie failed (no results)");
                    return null;
                }
                for (SearchResult result : lookup) {
                    // find movie (tmdb ids are not unique for tv and movies)
                    if (result.movie != null && result.movie.ids != null) {
                        return result.movie.ids.trakt;
                    }
                }

                Timber.e("Finding trakt movie failed (not in results)");
            } catch (RetrofitError e) {
                Timber.e(e, "Finding trakt movie failed " + e.getUrl());
            }

            return null;
        }

        private static Movie loadSummaryFromTrakt(Movies traktMovies, int movieTraktId) {
            try {
                return traktMovies.summary(String.valueOf(movieTraktId), Extended.FULL);
            } catch (RetrofitError e) {
                Timber.e(e, "Loading trakt movie summary failed " + e.getUrl());
                return null;
            }
        }

        private static Ratings loadRatingsFromTrakt(Movies traktMovies, int movieTraktId) {
            try {
                Ratings ratings = traktMovies.ratings(String.valueOf(movieTraktId));
                // ensure rating is between 0 and 100
                if (ratings != null && ratings.rating != null) {
                    ratings.rating *= 10;
                }
                return ratings;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading trakt movie ratings failed " + e.getUrl());
                return null;
            }
        }

        private static com.uwetrottmann.tmdb.entities.Movie loadSummaryFromTmdb(
                MoviesService moviesService, String languageCode, int movieTmdbId) {
            try {
                com.uwetrottmann.tmdb.entities.Movie movie = moviesService.summary(movieTmdbId,
                        languageCode);
                if (movie != null && TextUtils.isEmpty(movie.overview)) {
                    // fall back to English if TMDb has no localized text
                    movie = moviesService.summary(movieTmdbId);
                }
                return movie;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading TMDb movie summary failed");
                return null;
            }
        }

        private static void buildMovieUpdateOps(List<BaseMovie> remoteMovies,
                HashSet<Integer> localMovies, Set<Integer> moviesToAdd,
                HashSet<Integer> moviesToRemove, ArrayList<ContentProviderOperation> batch,
                ContentValues values) {
            for (BaseMovie movie : remoteMovies) {
                if (movie.movie == null || movie.movie.ids == null
                        || movie.movie.ids.tmdb == null) {
                    continue; // skip invalid values
                }

                int movieTmdbId = movie.movie.ids.tmdb;

                if (localMovies.contains(movieTmdbId)) {
                    // update existing movie
                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId))
                            .withValues(values).build();
                    batch.add(op);

                    // prevent movie from getting removed
                    moviesToRemove.remove(movieTmdbId);
                } else {
                    // insert new movie
                    moviesToAdd.add(movieTmdbId);
                }
            }
        }

        private static void buildMovieDeleteOps(HashSet<Integer> moviesToRemove,
                ArrayList<ContentProviderOperation> batch) {
            for (Integer movieTmdbId : moviesToRemove) {
                ContentProviderOperation op = ContentProviderOperation
                        .newDelete(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId)).build();
                batch.add(op);
            }
        }
    }

    public static class Upload {

        /**
         * Uploads all local movies to Hexagon.
         */
        public static boolean toHexagon(Context context) {
            Timber.d("toHexagon: uploading all movies");

            List<com.uwetrottmann.seriesguide.backend.movies.model.Movie> movies = buildMovieList(
                    context);
            if (movies == null) {
                Timber.e("toHexagon: movie query was null");
                return false;
            }
            if (movies.size() == 0) {
                // nothing to do
                Timber.d("toHexagon: no movies to upload");
                return true;
            }

            MovieList movieList = new MovieList();
            movieList.setMovies(movies);

            try {
                HexagonTools.getMoviesService(context).save(movieList).execute();
            } catch (IOException e) {
                Timber.e(e, "toHexagon: failed to upload movies");
                return false;
            }

            return true;
        }

        private static List<com.uwetrottmann.seriesguide.backend.movies.model.Movie> buildMovieList(
                Context context) {
            List<com.uwetrottmann.seriesguide.backend.movies.model.Movie> movies
                    = new ArrayList<>();

            Cursor query = context.getContentResolver()
                    .query(SeriesGuideContract.Movies.CONTENT_URI,
                            new String[] {
                                    SeriesGuideContract.Movies.TMDB_ID,
                                    SeriesGuideContract.Movies.IN_COLLECTION,
                                    SeriesGuideContract.Movies.IN_WATCHLIST
                            }, null, null, null
                    );
            if (query == null) {
                return null;
            }

            while (query.moveToNext()) {
                com.uwetrottmann.seriesguide.backend.movies.model.Movie movie
                        = new com.uwetrottmann.seriesguide.backend.movies.model.Movie();
                movie.setTmdbId(query.getInt(0));
                movie.setIsInCollection(query.getInt(1) == 1);
                movie.setIsInWatchlist(query.getInt(2) == 1);
                movies.add(movie);
            }

            query.close();

            return movies;
        }

        /**
         * Uploads the given movies to the appropriate list(s) on trakt.
         */
        public static UpdateResult toTrakt(Context context, Sync sync,
                HashSet<Integer> moviesToUpload) {
            if (moviesToUpload.size() == 0) {
                // nothing to upload
                return UpdateResult.SUCCESS;
            }

            Timber.d("toTrakt: upload " + moviesToUpload.size());

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            Cursor localMovies = context.getContentResolver()
                    .query(SeriesGuideContract.Movies.CONTENT_URI,
                            new String[] { SeriesGuideContract.Movies._ID,
                                    SeriesGuideContract.Movies.TMDB_ID,
                                    SeriesGuideContract.Movies.IN_COLLECTION,
                                    SeriesGuideContract.Movies.IN_WATCHLIST }, null, null, null
                    );
            if (localMovies == null) {
                return UpdateResult.INCOMPLETE;
            }

            // build list of collected, watchlisted movies to upload
            List<SyncMovie> moviesToCollect = new LinkedList<>();
            List<SyncMovie> moviesToWatchlist = new LinkedList<>();
            while (localMovies.moveToNext()) {
                int tmdbId = localMovies.getInt(1);
                if (!moviesToUpload.contains(tmdbId)) {
                    continue;
                }

                SyncMovie movie = new SyncMovie().id(MovieIds.tmdb(tmdbId));

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
                SyncItems items = new SyncItems();
                if (moviesToCollect.size() > 0) {
                    items.movies(moviesToCollect);
                    sync.addItemsToCollection(items);
                }
                if (moviesToWatchlist.size() > 0) {
                    items.movies(moviesToWatchlist);
                    sync.addItemsToWatchlist(items);
                }
            } catch (RetrofitError e) {
                Timber.e(e, "Uploading movies to watchlist or collection failed");
                return UpdateResult.INCOMPLETE;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return UpdateResult.INCOMPLETE;
            }

            return UpdateResult.SUCCESS;
        }
    }
}
