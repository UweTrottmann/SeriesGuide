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
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.MovieDetails;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.tasks.AddMovieToCollectionTask;
import com.battlelancer.seriesguide.util.tasks.AddMovieToWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.RateMovieTask;
import com.battlelancer.seriesguide.util.tasks.RemoveMovieFromCollectionTask;
import com.battlelancer.seriesguide.util.tasks.RemoveMovieFromWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.SetMovieUnwatchedTask;
import com.battlelancer.seriesguide.util.tasks.SetMovieWatchedTask;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList;
import com.uwetrottmann.tmdb.services.MoviesService;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseMovie;
import com.uwetrottmann.trakt.v2.entities.LastActivityMore;
import com.uwetrottmann.trakt.v2.entities.MovieIds;
import com.uwetrottmann.trakt.v2.entities.Ratings;
import com.uwetrottmann.trakt.v2.entities.SearchResult;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncMovie;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.IdType;
import com.uwetrottmann.trakt.v2.enums.Rating;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Movies;
import com.uwetrottmann.trakt.v2.services.Search;
import com.uwetrottmann.trakt.v2.services.Sync;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import android.support.annotation.NonNull;
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

    public enum Lists {
        COLLECTION(SeriesGuideContract.Movies.IN_COLLECTION),
        WATCHLIST(SeriesGuideContract.Movies.IN_WATCHLIST);

        public final String databaseColumn;

        private Lists(String databaseColumn) {
            this.databaseColumn = databaseColumn;
        }
    }

    /**
     * Deletes all movies which are not watched and not in any list.
     */
    public static void deleteUnusedMovies(Context context) {
        int rowsDeleted = context.getContentResolver()
                .delete(SeriesGuideContract.Movies.CONTENT_URI,
                        SeriesGuideContract.Movies.SELECTION_UNWATCHED
                                + " AND " + SeriesGuideContract.Movies.SELECTION_NOT_COLLECTION
                                + " AND " + SeriesGuideContract.Movies.SELECTION_NOT_WATCHLIST,
                        null);
        Timber.d("deleteUnusedMovies: removed " + rowsDeleted + " movies");
    }

    public static void addToCollection(Context context, int movieTmdbId) {
        AndroidUtils.executeOnPool(new AddMovieToCollectionTask(context, movieTmdbId));
    }

    public static void addToWatchlist(Context context, int movieTmdbId) {
        AndroidUtils.executeOnPool(new AddMovieToWatchlistTask(context, movieTmdbId));
    }

    /**
     * Adds the movie to the given list. If it was not in any list before, adds the movie to the
     * local database first.
     *
     * @return If the database operation was successful.
     */
    public static boolean addToList(Context context, int movieTmdbId, Lists list) {
        // do we have this movie in the database already?
        Boolean movieExists = isMovieInDatabase(context, movieTmdbId);
        if (movieExists == null) {
            return false;
        }
        if (movieExists) {
            return updateMovie(context, movieTmdbId, list.databaseColumn, true);
        } else {
            return addMovie(context, movieTmdbId, list);
        }
    }

    public static void removeFromCollection(Context context, int movieTmdbId) {
        AndroidUtils.executeOnPool(new RemoveMovieFromCollectionTask(context, movieTmdbId));
    }

    public static void removeFromWatchlist(Context context, int movieTmdbId) {
        AndroidUtils.executeOnPool(new RemoveMovieFromWatchlistTask(context, movieTmdbId));
    }

    /**
     * Removes the movie from the given list.
     *
     * <p>If it would not be on any list afterwards and is not watched, deletes the movie from the
     * local database.
     *
     * @return If the database operation was successful.
     */
    public static boolean removeFromList(Context context, int movieTmdbId, Lists listToRemoveFrom) {
        Lists otherListToCheck = listToRemoveFrom == Lists.COLLECTION
                ? Lists.WATCHLIST : Lists.COLLECTION;
        Boolean isInOtherList = isMovieInList(context, movieTmdbId, otherListToCheck);
        if (isInOtherList == null) {
            // query failed, or movie not in local database
            return false;
        }

        // if movie will not be in any list and is not watched, remove it completely
        if (!isInOtherList && deleteMovieIfUnwatched(context, movieTmdbId)) {
            return true;
        } else {
            // otherwise, just update
            return updateMovie(context, movieTmdbId, listToRemoveFrom.databaseColumn, false);
        }
    }

    public static void watchedMovie(Context context, int movieTmdbId) {
        AndroidUtils.executeOnPool(new SetMovieWatchedTask(context, movieTmdbId));
    }

    public static void unwatchedMovie(Context context, int movieTmdbId) {
        AndroidUtils.executeOnPool(new SetMovieUnwatchedTask(context, movieTmdbId));
    }

    /**
     * Set watched flag of movie in local database. If setting watched, but not in database, creates
     * a new movie watched shell.
     *
     * @return If the database operation was successful.
     */
    public static boolean setWatchedFlag(Context context, int movieTmdbId, boolean flag) {
        Boolean movieInDatabase = isMovieInDatabase(context, movieTmdbId);
        if (movieInDatabase == null) {
            return false;
        }
        if (!movieInDatabase && flag) {
            // Only add, never remove shells. Next trakt watched movie sync will take care of that.
            return addMovieWatchedShell(context, movieTmdbId);
        } else {
            return updateMovie(context, movieTmdbId, SeriesGuideContract.Movies.WATCHED, flag);
        }
    }

    /**
     * Store the rating for the given movie in the database (if it is in the database) and send it
     * to trakt.
     */
    public static void rate(Context context, int movieTmdbId, Rating rating) {
        AndroidUtils.executeOnPool(new RateMovieTask(context, rating, movieTmdbId));
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
     * Extracts ratings from trakt, all other properties from TMDb data.
     *
     * <p> If either movie data is null, will still extract the properties of others.
     */
    public static ContentValues buildBasicMovieContentValues(MovieDetails details) {
        ContentValues values = new ContentValues();

        // data from trakt
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
            values.put(SeriesGuideContract.Movies.RATING_VOTES_TMDB, details.tmdbMovie().vote_count);
            // if there is no release date, store Long.MAX as it is likely in the future
            // also helps correctly sorting movies by release date
            Date releaseDate = details.tmdbMovie().release_date;
            values.put(SeriesGuideContract.Movies.RELEASED_UTC_MS,
                    releaseDate == null ? Long.MAX_VALUE : releaseDate.getTime());
        }

        return values;
    }

    /**
     * Returns a set of the TMDb ids of all movies in the local database.
     *
     * @return null if there was an error, empty list if there are no movies.
     */
    public static HashSet<Integer> getMovieTmdbIdsAsSet(Context context) {
        HashSet<Integer> localMoviesIds = new HashSet<>();

        Cursor movies = context.getContentResolver().query(SeriesGuideContract.Movies.CONTENT_URI,
                new String[] { SeriesGuideContract.Movies.TMDB_ID },
                null, null, null);
        if (movies == null) {
            return null;
        }

        while (movies.moveToNext()) {
            localMoviesIds.add(movies.getInt(0));
        }

        movies.close();

        return localMoviesIds;
    }

    /**
     * Determines if the movie is in the given list.
     *
     * @return true if the movie is in the given list, false otherwise. Can return {@code null} if
     * the database could not be queried or the movie does not exist.
     */
    private static Boolean isMovieInList(Context context, int movieTmdbId, Lists list) {
        Cursor movie = context.getContentResolver()
                .query(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId),
                        new String[] { list.databaseColumn }, null, null, null);
        if (movie == null) {
            return null;
        }

        Boolean isInList = null;
        if (movie.moveToFirst()) {
            isInList = movie.getInt(0) == 1;
        }

        movie.close();

        return isInList;
    }

    private static Boolean isMovieInDatabase(Context context, int movieTmdbId) {
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

    private static boolean addMovie(Context context, int movieTmdbId, Lists listToAddTo) {
        // get movie info
        MovieDetails details = Download.getMovieDetails(context, movieTmdbId);
        if (details.tmdbMovie() == null) {
            // abort if minimal data failed to load
            return false;
        }

        // build values
        ContentValues values = buildBasicMovieContentValuesWithId(details);

        // set flags
        values.put(SeriesGuideContract.Movies.IN_COLLECTION,
                DBUtils.convertBooleanToInt(listToAddTo == Lists.COLLECTION));
        values.put(SeriesGuideContract.Movies.IN_WATCHLIST,
                DBUtils.convertBooleanToInt(listToAddTo == Lists.WATCHLIST));

        // add to database
        context.getContentResolver().insert(SeriesGuideContract.Movies.CONTENT_URI, values);

        // ensure ratings and watched flags are downloaded on next sync
        TraktSettings.resetMoviesLastActivity(context);

        return true;
    }

    /**
     * Inserts a movie shell into the database only holding TMDB id, list and watched states.
     */
    private static boolean addMovieWatchedShell(Context context, int movieTmdbId) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Movies.TMDB_ID, movieTmdbId);
        values.put(SeriesGuideContract.Movies.IN_COLLECTION, false);
        values.put(SeriesGuideContract.Movies.IN_WATCHLIST, false);
        values.put(SeriesGuideContract.Movies.WATCHED, true);

        Uri insert = context.getContentResolver().insert(SeriesGuideContract.Movies.CONTENT_URI,
                values);

        return insert != null;
    }

    private static boolean updateMovie(Context context, int movieTmdbId, String column,
            boolean value) {
        ContentValues values = new ContentValues();
        values.put(column, value);

        int rowsUpdated = context.getContentResolver().update(
                SeriesGuideContract.Movies.buildMovieUri(movieTmdbId), values, null, null);

        return rowsUpdated > 0;
    }

    /**
     * @return {@code true} if the movie was deleted (because it was not watched).
     */
    private static boolean deleteMovieIfUnwatched(Context context, int movieTmdbId) {
        int rowsDeleted = context.getContentResolver()
                .delete(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId),
                        SeriesGuideContract.Movies.SELECTION_UNWATCHED, null);
        Timber.d("deleteMovieIfUnwatched: deleted " + rowsDeleted + " movie");
        return rowsDeleted > 0;
    }

    public static Integer lookupTraktId(Search traktSearch, int movieTmdbId) {
        try {
            List<SearchResult> lookup = traktSearch.idLookup(IdType.TMDB,
                    String.valueOf(movieTmdbId), 1, 10);
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
            Timber.e(e, "Finding trakt movie failed");
        }

        return null;
    }

    public static class Download {

        /**
         * Downloads movies from hexagon, updates existing movies with new properties, removes
         * movies that are neither in collection or watchlist.
         *
         * <p> Adds movie tmdb ids to the respective collection or watchlist set.
         */
        public static boolean fromHexagon(Context context,
                @NonNull Set<Integer> newCollectionMovies, @NonNull Set<Integer> newWatchlistMovies,
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
         * Updates the local movie database against trakt movie watchlist and collection. Adds,
         * updates and removes movies in the database.
         *
         * <p> When syncing the first time, will upload any local movies missing from trakt
         * collection or watchlist instead of removing them locally.
         *
         * <p> Performs <b>synchronous network access</b>, make sure to run this on a background
         * thread.
         */
        public static UpdateResult syncMovieListsWithTrakt(Context context,
                LastActivityMore activity) {
            if (activity.collected_at == null) {
                Timber.e("syncMoviesWithTrakt: null collected_at");
                return UpdateResult.INCOMPLETE;
            }
            if (activity.watchlisted_at == null) {
                Timber.e("syncMoviesWithTrakt: null watchlisted_at");
                return UpdateResult.INCOMPLETE;
            }

            TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
            if (trakt == null) {
                return UpdateResult.INCOMPLETE;
            }

            final boolean merging = !TraktSettings.hasMergedMovies(context);
            if (!merging && !TraktSettings.isMovieListsChanged(context, activity.collected_at,
                    activity.watchlisted_at)) {
                Timber.d("syncMoviesWithTrakt: no changes");
                return UpdateResult.SUCCESS;
            }

            Sync sync = trakt.sync();

            // download collection and watchlist
            Set<Integer> collection;
            Set<Integer> watchlist;
            try {
                collection = buildTmdbIdSet(sync.collectionMovies(Extended.DEFAULT_MIN));
                if (collection == null) {
                    Timber.e("syncMoviesWithTrakt: null collection response");
                    return UpdateResult.INCOMPLETE;
                }

                watchlist = buildTmdbIdSet(sync.watchlistMovies(Extended.DEFAULT_MIN));
                if (watchlist == null) {
                    Timber.e("syncMoviesWithTrakt: null watchlist response");
                    return UpdateResult.INCOMPLETE;
                }
            } catch (RetrofitError e) {
                Timber.e(e, "syncMoviesWithTrakt: download failed");
                return UpdateResult.INCOMPLETE;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return UpdateResult.INCOMPLETE;
            }

            // build updates
            // loop through all local movies
            Set<Integer> moviesNotOnTraktCollection = new HashSet<>();
            Set<Integer> moviesNotOnTraktWatchlist = new HashSet<>();
            ArrayList<ContentProviderOperation> batch = new ArrayList<>();
            for (Integer tmdbId : getMovieTmdbIdsAsSet(context)) {
                // is local movie in trakt collection or watchlist?
                boolean inCollection = collection.remove(tmdbId);
                boolean inWatchlist = watchlist.remove(tmdbId);

                if (merging) {
                    // upload movie if missing from trakt collection or watchlist
                    if (!inCollection) {
                        moviesNotOnTraktCollection.add(tmdbId);
                    }
                    if (!inWatchlist) {
                        moviesNotOnTraktWatchlist.add(tmdbId);
                    }
                    // add to local collection or watchlist, but do NOT remove
                    if (inCollection || inWatchlist) {
                        ContentProviderOperation.Builder builder = ContentProviderOperation
                                .newUpdate(SeriesGuideContract.Movies.buildMovieUri(tmdbId));
                        if (inCollection) {
                            builder.withValue(SeriesGuideContract.Movies.IN_COLLECTION, true);
                        }
                        if (inWatchlist) {
                            builder.withValue(SeriesGuideContract.Movies.IN_WATCHLIST, true);
                        }
                        batch.add(builder.build());
                    }
                } else {
                    // mirror trakt collection and watchlist flag
                    // will take care of removing unneeded (not watched or in any list) movies
                    // in later sync step
                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(SeriesGuideContract.Movies.buildMovieUri(tmdbId))
                            .withValue(SeriesGuideContract.Movies.IN_COLLECTION, inCollection)
                            .withValue(SeriesGuideContract.Movies.IN_WATCHLIST, inWatchlist)
                            .build();
                    batch.add(op);
                }
            }

            // apply collection and watchlist updates to existing movies
            try {
                DBUtils.applyInSmallBatches(context, batch);
                Timber.d("syncMoviesWithTrakt: updated " + batch.size());
            } catch (OperationApplicationException e) {
                Timber.e(e, "syncMoviesWithTrakt: database updates failed");
                return UpdateResult.INCOMPLETE;
            }
            batch.clear();

            // merge on first run
            if (merging) {
                // upload movies not in trakt collection or watchlist
                if (Upload.toTrakt(context, sync, moviesNotOnTraktCollection,
                        moviesNotOnTraktWatchlist) != UpdateResult.SUCCESS) {
                    return UpdateResult.INCOMPLETE;
                } else {
                    // set merge successful
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, true)
                            .commit();
                }
            }

            // add movies from trakt missing locally
            // all local movies were removed from trakt collection and watchlist,
            // so they only contain movies missing locally
            UpdateResult result = addMovies(context, collection, watchlist);

            if (result == UpdateResult.SUCCESS) {
                // store last activity timestamps
                TraktSettings.storeLastMoviesChangedAt(context, activity.collected_at,
                        activity.watchlisted_at);
                // if movies were added,
                // ensure all movie ratings and watched flags are downloaded next
                if (collection.size() > 0 || watchlist.size() > 0) {
                    TraktSettings.resetMoviesLastActivity(context);
                }
            }

            return result;
        }

        private static Set<Integer> buildTmdbIdSet(List<BaseMovie> movies) {
            if (movies == null) {
                return null;
            }

            Set<Integer> tmdbIdSet = new HashSet<>();
            for (BaseMovie movie : movies) {
                if (movie.movie == null || movie.movie.ids == null
                        || movie.movie.ids.tmdb == null) {
                    continue; // skip invalid values
                }
                tmdbIdSet.add(movie.movie.ids.tmdb);
            }
            return tmdbIdSet;
        }

        /**
         * Adds new movies to the database.
         *
         * @param newCollectionMovies Movie TMDB ids to add to the collection.
         * @param newWatchlistMovies Movie TMDB ids to add to the watchlist.
         */
        public static UpdateResult addMovies(@NonNull Context context,
                @NonNull Set<Integer> newCollectionMovies,
                @NonNull Set<Integer> newWatchlistMovies) {
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

            TraktV2 trakt = ServiceUtils.getTraktV2(context);
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
                if (movieDetails.tmdbMovie() == null) {
                    // skip if minimal values failed to load
                    Timber.d("addMovies: downloaded movie " + tmdbId + " incomplete, skipping");
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
            TraktV2 trakt = ServiceUtils.getTraktV2(context);
            Movies traktMovies = trakt.movies();
            Search traktSearch = trakt.search();

            // TMDb
            MoviesService tmdbMovies = ServiceUtils.getTmdb(context).moviesService();
            String languageCode = DisplaySettings.getContentLanguage(context);

            return getMovieDetails(traktSearch, traktMovies, tmdbMovies, languageCode, movieTmdbId);
        }

        /**
         * Download movie data from trakt and TMDb.
         *
         * <p> <b>Always</b> supply trakt services <b>without</b> auth, as retrofit will crash on
         * auth errors.
         */
        public static MovieDetails getMovieDetails(Search traktSearch, Movies traktMovies,
                MoviesService tmdbMovies, String languageCode, int movieTmdbId) {
            MovieDetails details = new MovieDetails();

            // load ratings and release time from trakt
            Integer movieTraktId = lookupTraktId(traktSearch, movieTmdbId);
            if (movieTraktId != null) {
                details.traktRatings(loadRatingsFromTrakt(traktMovies, movieTraktId));
            }

            // load summary from tmdb
            details.tmdbMovie(loadSummaryFromTmdb(tmdbMovies, languageCode, movieTmdbId));

            return details;
        }

        private static Ratings loadRatingsFromTrakt(Movies traktMovies, int movieTraktId) {
            try {
                return traktMovies.ratings(String.valueOf(movieTraktId));
            } catch (RetrofitError e) {
                Timber.e(e, "Loading trakt movie ratings failed");
                return null;
            }
        }

        private static com.uwetrottmann.tmdb.entities.Movie loadSummaryFromTmdb(
                MoviesService moviesService, String languageCode, int movieTmdbId) {
            try {
                com.uwetrottmann.tmdb.entities.Movie movie = moviesService.summary(movieTmdbId,
                        languageCode, null);
                if (movie != null && TextUtils.isEmpty(movie.overview)) {
                    // fall back to English if TMDb has no localized text
                    movie = moviesService.summary(movieTmdbId, null, null);
                }
                return movie;
            } catch (RetrofitError e) {
                Timber.e(e, "Loading TMDb movie summary failed");
                return null;
            }
        }

        private static void buildMovieDeleteOps(Set<Integer> moviesToRemove,
                ArrayList<ContentProviderOperation> batch) {
            for (Integer movieTmdbId : moviesToRemove) {
                ContentProviderOperation op = ContentProviderOperation
                        .newDelete(SeriesGuideContract.Movies.buildMovieUri(movieTmdbId)).build();
                batch.add(op);
            }
        }
    }

    public static class Upload {

        private static final String[] PROJECTION_MOVIES_IN_LISTS = {
                SeriesGuideContract.Movies.TMDB_ID, // 0
                SeriesGuideContract.Movies.IN_COLLECTION, // 1
                SeriesGuideContract.Movies.IN_WATCHLIST // 2
        };

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

            // query for movies in lists (excluding movies that are only watched)
            Cursor moviesInLists = context.getContentResolver().query(
                    SeriesGuideContract.Movies.CONTENT_URI, PROJECTION_MOVIES_IN_LISTS,
                    SeriesGuideContract.Movies.SELECTION_IN_LIST, null, null);
            if (moviesInLists == null) {
                return null;
            }

            while (moviesInLists.moveToNext()) {
                com.uwetrottmann.seriesguide.backend.movies.model.Movie movie
                        = new com.uwetrottmann.seriesguide.backend.movies.model.Movie();
                movie.setTmdbId(moviesInLists.getInt(0));
                movie.setIsInCollection(moviesInLists.getInt(1) == 1);
                movie.setIsInWatchlist(moviesInLists.getInt(2) == 1);
                movies.add(movie);
            }

            moviesInLists.close();

            return movies;
        }

        /**
         * Checks if the given movies are in the local collection or watchlist, then uploads them to
         * the appropriate list(s) on trakt.
         */
        public static UpdateResult toTrakt(Context context, Sync sync,
                Set<Integer> moviesNotOnTraktCollection, Set<Integer> moviesNotOnTraktWatchlist) {
            if (moviesNotOnTraktCollection.size() == 0 && moviesNotOnTraktWatchlist.size() == 0) {
                // nothing to upload
                Timber.d("toTrakt: nothing to upload");
                return UpdateResult.SUCCESS;
            }

            // return if connectivity is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return UpdateResult.INCOMPLETE;
            }

            // query for movies in lists (excluding movies that are only watched)
            Cursor moviesInLists = context.getContentResolver()
                    .query(SeriesGuideContract.Movies.CONTENT_URI, PROJECTION_MOVIES_IN_LISTS,
                            SeriesGuideContract.Movies.SELECTION_IN_LIST, null, null);
            if (moviesInLists == null) {
                Timber.e("toTrakt: query failed");
                return UpdateResult.INCOMPLETE;
            }

            // build list of collected, watchlisted movies to upload
            List<SyncMovie> moviesToCollect = new LinkedList<>();
            List<SyncMovie> moviesToWatchlist = new LinkedList<>();
            while (moviesInLists.moveToNext()) {
                int tmdbId = moviesInLists.getInt(0);

                // in local collection, but not on trakt?
                if (moviesInLists.getInt(1) == 1 && moviesNotOnTraktCollection.contains(tmdbId)) {
                    moviesToCollect.add(new SyncMovie().id(MovieIds.tmdb(tmdbId)));
                }
                // in local watchlist, but not on trakt?
                if (moviesInLists.getInt(2) == 1 && moviesNotOnTraktWatchlist.contains(tmdbId)) {
                    moviesToWatchlist.add(new SyncMovie().id(MovieIds.tmdb(tmdbId)));
                }
            }

            // clean up
            moviesInLists.close();

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
                Timber.e(e, "toTrakt: upload failed");
                return UpdateResult.INCOMPLETE;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return UpdateResult.INCOMPLETE;
            }

            Timber.d("toTrakt: success, uploaded "
                    + moviesToCollect.size() + " to collection, "
                    + moviesToWatchlist.size() + " to watchlist");
            return UpdateResult.SUCCESS;
        }
    }
}
