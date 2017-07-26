package com.battlelancer.seriesguide.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.MovieDetails;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.tmdbapi.SgTmdb;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.tasks.AddMovieToCollectionTask;
import com.battlelancer.seriesguide.util.tasks.AddMovieToWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.RemoveMovieFromCollectionTask;
import com.battlelancer.seriesguide.util.tasks.RemoveMovieFromWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.SetMovieUnwatchedTask;
import com.battlelancer.seriesguide.util.tasks.SetMovieWatchedTask;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.trakt5.entities.Ratings;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.enums.IdType;
import com.uwetrottmann.trakt5.enums.Type;
import com.uwetrottmann.trakt5.services.Movies;
import com.uwetrottmann.trakt5.services.Search;
import dagger.Lazy;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import retrofit2.Response;
import timber.log.Timber;

public class MovieTools {

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

        Lists(String databaseColumn) {
            this.databaseColumn = databaseColumn;
        }
    }

    private final Context context;
    private final Lazy<MoviesService> tmdbMovies;
    private final Lazy<Movies> traktMovies;
    private final Lazy<Search> traktSearch;

    @Inject
    public MovieTools(
            @ApplicationContext Context context,
            Lazy<MoviesService> tmdbMovies,
            Lazy<Movies> traktMovies,
            Lazy<Search> traktSearch
    ) {
        this.context = context;
        this.tmdbMovies = tmdbMovies;
        this.traktMovies = traktMovies;
        this.traktSearch = traktSearch;
    }

    /**
     * Date format using only numbers.
     */
    public static DateFormat getMovieShortDateFormat() {
        // use SHORT as in some languages (Portuguese) the MEDIUM string is longer than expected
        return DateFormat.getDateInstance(DateFormat.SHORT);
    }

    /**
     * Return release date or null if unknown from millisecond value stored in the database as
     * {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies#RELEASED_UTC_MS}.
     */
    @Nullable
    public static Date movieReleaseDateFrom(long releaseDateMs) {
        return releaseDateMs == Long.MAX_VALUE ? null : new Date(releaseDateMs);
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
        Timber.d("deleteUnusedMovies: removed %s movies", rowsDeleted);
    }

    public static void addToCollection(Context context, int movieTmdbId) {
        AsyncTaskCompat.executeParallel(new AddMovieToCollectionTask(context, movieTmdbId));
    }

    public static void addToWatchlist(Context context, int movieTmdbId) {
        AsyncTaskCompat.executeParallel(new AddMovieToWatchlistTask(context, movieTmdbId));
    }

    /**
     * Adds the movie to the given list. If it was not in any list before, adds the movie to the
     * local database first.
     *
     * @return If the database operation was successful.
     */
    public boolean addToList(int movieTmdbId, Lists list) {
        // do we have this movie in the database already?
        Boolean movieExists = isMovieInDatabase(context, movieTmdbId);
        if (movieExists == null) {
            return false;
        }
        if (movieExists) {
            return updateMovie(context, movieTmdbId, list.databaseColumn, true);
        } else {
            return addMovie(movieTmdbId, list);
        }
    }

    public static void removeFromCollection(Context context, int movieTmdbId) {
        AsyncTaskCompat.executeParallel(new RemoveMovieFromCollectionTask(context, movieTmdbId));
    }

    public static void removeFromWatchlist(Context context, int movieTmdbId) {
        AsyncTaskCompat.executeParallel(new RemoveMovieFromWatchlistTask(context, movieTmdbId));
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
        //noinspection SimplifiableIfStatement
        if (!isInOtherList && deleteMovieIfUnwatched(context, movieTmdbId)) {
            return true;
        } else {
            // otherwise, just update
            return updateMovie(context, movieTmdbId, listToRemoveFrom.databaseColumn, false);
        }
    }

    public static void watchedMovie(Context context, int movieTmdbId) {
        AsyncTaskCompat.executeParallel(new SetMovieWatchedTask(context, movieTmdbId));
    }

    public static void unwatchedMovie(Context context, int movieTmdbId) {
        AsyncTaskCompat.executeParallel(new SetMovieUnwatchedTask(context, movieTmdbId));
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
            values.put(SeriesGuideContract.Movies.RATING_VOTES_TMDB,
                    details.tmdbMovie().vote_count);
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

    private boolean addMovie(int movieTmdbId, Lists listToAddTo) {
        // get movie info
        MovieDetails details = getMovieDetails(movieTmdbId);
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
        Timber.d("deleteMovieIfUnwatched: deleted %s movies", rowsDeleted);
        return rowsDeleted > 0;
    }

    /**
     * @return {@code null} if looking up the id failed, -1 if the movie was not found or the movie
     * id if it was found.
     */
    @Nullable
    public Integer lookupTraktId(int movieTmdbId) {
        try {
            Response<List<SearchResult>> response = traktSearch.get().idLookup(IdType.TMDB,
                    String.valueOf(movieTmdbId), Type.MOVIE, null, 1, 1).execute();
            if (response.isSuccessful()) {
                List<SearchResult> results = response.body();
                if (results == null || results.size() != 1) {
                    Timber.e("Finding trakt movie failed (no results)");
                    return -1;
                }
                SearchResult result = results.get(0);
                if (result.movie != null && result.movie.ids != null) {
                    return result.movie.ids.trakt;
                }
                Timber.e("Finding trakt movie failed (not in results)");
                return -1;
            } else {
                SgTrakt.trackFailedRequest(context, "movie trakt id lookup", response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "movie trakt id lookup", e);
        }
        return null;
    }

    /**
     * Adds new movies to the database.
     *
     * @param newCollectionMovies Movie TMDB ids to add to the collection.
     * @param newWatchlistMovies Movie TMDB ids to add to the watchlist.
     */
    public boolean addMovies(@NonNull Set<Integer> newCollectionMovies,
            @NonNull Set<Integer> newWatchlistMovies) {
        Timber.d("addMovies: %s to collection, %s to watchlist", newCollectionMovies.size(),
                newWatchlistMovies.size());

        // build a single list of tmdb ids
        Set<Integer> newMovies = new HashSet<>();
        for (Integer tmdbId : newCollectionMovies) {
            newMovies.add(tmdbId);
        }
        for (Integer tmdbId : newWatchlistMovies) {
            newMovies.add(tmdbId);
        }

        String languageCode = DisplaySettings.getMoviesLanguage(context);
        List<MovieDetails> movies = new LinkedList<>();

        // loop through ids
        for (Iterator<Integer> iterator = newMovies.iterator(); iterator.hasNext(); ) {
            int tmdbId = iterator.next();
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("addMovies: no network connection");
                return false;
            }

            // download movie data
            MovieDetails movieDetails = getMovieDetails(languageCode, tmdbId);
            if (movieDetails.tmdbMovie() == null) {
                // skip if minimal values failed to load
                Timber.d("addMovies: downloaded movie %s incomplete, skipping", tmdbId);
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

        return true;
    }

    /**
     * Download movie data from trakt and TMDb using the {@link DisplaySettings#getMoviesLanguage(Context)}.
     */
    public MovieDetails getMovieDetails(int movieTmdbId) {
        String languageCode = DisplaySettings.getMoviesLanguage(context);
        return getMovieDetails(languageCode, movieTmdbId);
    }

    /**
     * Download movie data from trakt and TMDb.
     */
    private MovieDetails getMovieDetails(String languageCode, int movieTmdbId) {
        MovieDetails details = new MovieDetails();

        // load ratings from trakt
        Integer movieTraktId = lookupTraktId(movieTmdbId);
        if (movieTraktId != null) {
            details.traktRatings(loadRatingsFromTrakt(movieTraktId));
        }

        // load summary from tmdb
        details.tmdbMovie(loadSummaryFromTmdb(languageCode, movieTmdbId));

        return details;
    }

    private Ratings loadRatingsFromTrakt(int movieTraktId) {
        try {
            Response<Ratings> response = traktMovies.get()
                    .ratings(String.valueOf(movieTraktId))
                    .execute();
            if (response.isSuccessful()) {
                return response.body();
            }
            SgTrakt.trackFailedRequest(context, "get movie rating", response);
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get movie rating", e);
        }
        return null;
    }

    @Nullable
    private com.uwetrottmann.tmdb2.entities.Movie loadSummaryFromTmdb(String languageCode,
            int movieTmdbId) {
        // try to get local movie summary
        Movie movie = getMovieSummary("get local movie summary", languageCode, movieTmdbId);
        if (movie != null && !TextUtils.isEmpty(movie.overview)) {
            return movie;
        }

        // fall back to default language if TMDb has no localized text
        movie = getMovieSummary("get default movie summary", null, movieTmdbId);
        if (movie != null) {
            // add note about non-translated or non-existing overview
            String untranslatedOverview = movie.overview;
            movie.overview = context.getString(R.string.no_translation,
                    LanguageTools.getMovieLanguageStringFor(context, languageCode),
                    context.getString(R.string.tmdb));
            if (!TextUtils.isEmpty(untranslatedOverview)) {
                movie.overview += "\n\n" + untranslatedOverview;
            }
        }
        return movie;
    }

    @Nullable
    public Movie getMovieSummary(int movieTmdbId) {
        String languageCode = DisplaySettings.getMoviesLanguage(context);
        return getMovieSummary("get local movie summary", languageCode, movieTmdbId);
    }

    @Nullable
    private Movie getMovieSummary(@NonNull String action, @Nullable String language,
            int movieTmdbId) {
        try {
            Response<Movie> response = tmdbMovies.get()
                    .summary(movieTmdbId, language, null)
                    .execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTmdb.trackFailedRequest(context, action, response);
            }
        } catch (IOException e) {
            SgTmdb.trackFailedRequest(context, action, e);
        }
        return null;
    }
}
