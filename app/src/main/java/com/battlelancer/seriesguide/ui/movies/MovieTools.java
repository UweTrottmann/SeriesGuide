package com.battlelancer.seriesguide.ui.movies;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.jobs.FlagJobAsyncTask;
import com.battlelancer.seriesguide.jobs.movies.MovieCollectionJob;
import com.battlelancer.seriesguide.jobs.movies.MovieWatchedJob;
import com.battlelancer.seriesguide.jobs.movies.MovieWatchlistJob;
import com.battlelancer.seriesguide.model.SgMovieFlags;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.LanguageTools;
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
        WATCHLIST(SeriesGuideContract.Movies.IN_WATCHLIST),
        WATCHED(SeriesGuideContract.Movies.WATCHED);

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
        FlagJobAsyncTask.executeJob(context, new MovieCollectionJob(movieTmdbId, true));
    }

    public static void addToWatchlist(Context context, int movieTmdbId) {
        FlagJobAsyncTask.executeJob(context, new MovieWatchlistJob(movieTmdbId, true));
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
            return false; // query failed
        }
        if (movieExists) {
            return updateMovie(context, movieTmdbId, list.databaseColumn, true);
        } else {
            return addMovie(movieTmdbId, list);
        }
    }

    public static void removeFromCollection(Context context, int movieTmdbId) {
        FlagJobAsyncTask.executeJob(context, new MovieCollectionJob(movieTmdbId, false));
    }

    public static void removeFromWatchlist(Context context, int movieTmdbId) {
        FlagJobAsyncTask.executeJob(context, new MovieWatchlistJob(movieTmdbId, false));
    }

    /**
     * Removes the movie from the given list.
     *
     * <p>If it would not be on any list afterwards, deletes the movie from the local database.
     *
     * @return If the database operation was successful.
     */
    public static boolean removeFromList(Context context, int movieTmdbId, Lists listToRemoveFrom) {
        SgMovieFlags movieFlags = SgRoomDatabase.getInstance(context).movieHelper()
                .getMovieFlags(movieTmdbId);
        if (movieFlags == null) {
            return false; // query failed
        }

        boolean removeMovie = false;
        if (listToRemoveFrom == Lists.COLLECTION) {
            removeMovie = !movieFlags.getInWatchlist() && !movieFlags.getWatched();
        } else if (listToRemoveFrom == Lists.WATCHLIST) {
            removeMovie = !movieFlags.getInCollection() && !movieFlags.getWatched();
        } else if (listToRemoveFrom == Lists.WATCHED) {
            removeMovie = !movieFlags.getInCollection() && !movieFlags.getInWatchlist();
        }

        // if movie will not be in any list, remove it completely
        if (removeMovie) {
            return deleteMovie(context, movieTmdbId);
        } else {
            // otherwise, just update
            return updateMovie(context, movieTmdbId, listToRemoveFrom.databaseColumn, false);
        }
    }

    static void watchedMovie(Context context, int movieTmdbId, boolean inWatchlist) {
        FlagJobAsyncTask.executeJob(context, new MovieWatchedJob(movieTmdbId, true));
        // trakt removes from watchlist automatically, but app would not show until next sync
        // and not mirror on hexagon, so do it manually
        if (inWatchlist) {
            removeFromWatchlist(context, movieTmdbId);
        }
    }

    static void unwatchedMovie(Context context, int movieTmdbId) {
        FlagJobAsyncTask.executeJob(context, new MovieWatchedJob(movieTmdbId, false));
    }

    private static ContentValues[] buildMoviesContentValues(List<MovieDetails> movies) {
        ContentValues[] valuesArray = new ContentValues[movies.size()];
        int index = 0;
        for (MovieDetails movie : movies) {
            valuesArray[index] = movie.toContentValuesInsert();
            index++;
        }
        return valuesArray;
    }

    /**
     * Returns a set of the TMDb ids of all movies in the local database.
     *
     * @return null if there was an error, empty list if there are no movies.
     */
    public static HashSet<Integer> getMovieTmdbIdsAsSet(Context context) {
        HashSet<Integer> localMoviesIds = new HashSet<>();

        Cursor movies = context.getContentResolver().query(SeriesGuideContract.Movies.CONTENT_URI,
                new String[]{SeriesGuideContract.Movies.TMDB_ID},
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

    private static Boolean isMovieInDatabase(Context context, int movieTmdbId) {
        Cursor movie = context.getContentResolver()
                .query(SeriesGuideContract.Movies.CONTENT_URI, new String[]{
                                SeriesGuideContract.Movies._ID},
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
        MovieDetails details = getMovieDetails(movieTmdbId, false);
        if (details.tmdbMovie() == null) {
            // abort if minimal data failed to load
            return false;
        }

        // build values
        details.setInCollection(listToAddTo == Lists.COLLECTION);
        details.setInWatchlist(listToAddTo == Lists.WATCHLIST);
        details.setWatched(listToAddTo == Lists.WATCHED);
        ContentValues values = details.toContentValuesInsert();

        // add to database
        context.getContentResolver().insert(SeriesGuideContract.Movies.CONTENT_URI, values);

        // ensure ratings for new movie are downloaded on next sync
        TraktSettings.resetMoviesLastRatedAt(context);

        return true;
    }

    /**
     * Returns {@code true} if the movie was updated.
     */
    private static boolean updateMovie(Context context, int movieTmdbId, String column,
            boolean value) {
        ContentValues values = new ContentValues();
        values.put(column, value ? 1 : 0);

        int rowsUpdated = context.getContentResolver().update(
                SeriesGuideContract.Movies.buildMovieUri(movieTmdbId), values, null, null);

        return rowsUpdated > 0;
    }

    public void updateMovie(MovieDetails details, int tmdbId) {
        ContentValues values = details.toContentValuesUpdate();
        if (values.size() == 0) {
            return; // nothing to update, downloading probably failed :(
        }

        values.put(SeriesGuideContract.Movies.LAST_UPDATED, System.currentTimeMillis());

        // if movie does not exist in database, will do nothing
        context.getContentResolver().update(SeriesGuideContract.Movies.buildMovieUri(tmdbId),
                values, null, null);
    }

    /**
     * Returns {@code true} if the movie was deleted.
     */
    private static boolean deleteMovie(Context context, int movieTmdbId) {
        int rowsDeleted = SgRoomDatabase.getInstance(context).movieHelper()
                .deleteMovie(movieTmdbId);
        Timber.d("deleteMovie: deleted %s movies", rowsDeleted);
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
                Errors.logAndReport("movie trakt id lookup", response);
            }
        } catch (Exception e) {
            Errors.logAndReport("movie trakt id lookup", e);
        }
        return null;
    }

    /**
     * Adds new movies to the database.
     *
     * @param newCollectionMovies Movie TMDB ids to add to the collection.
     * @param newWatchlistMovies Movie TMDB ids to add to the watchlist.
     * @param newWatchedMovies Movie TMDB ids to set watched.
     */
    public boolean addMovies(
            @NonNull Set<Integer> newCollectionMovies,
            @NonNull Set<Integer> newWatchlistMovies,
            @Nullable Set<Integer> newWatchedMovies
    ) {
        Timber.d("addMovies: %s to collection, %s to watchlist", newCollectionMovies.size(),
                newWatchlistMovies.size());
        if (newWatchedMovies != null) {
            Timber.d("addMovies: %s to watched", newWatchedMovies.size());
        }

        // build a single list of tmdb ids
        Set<Integer> newMovies = new HashSet<>();
        newMovies.addAll(newCollectionMovies);
        newMovies.addAll(newWatchlistMovies);
        if (newWatchedMovies != null) {
            newMovies.addAll(newWatchedMovies);
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
            MovieDetails movieDetails = getMovieDetails(languageCode, tmdbId, false);
            if (movieDetails.tmdbMovie() == null) {
                // skip if minimal values failed to load
                Timber.d("addMovies: downloaded movie %s incomplete, skipping", tmdbId);
                continue;
            }

            // set flags
            movieDetails.setInCollection(newCollectionMovies.contains(tmdbId));
            movieDetails.setInWatchlist(newWatchlistMovies.contains(tmdbId));
            if (newWatchedMovies != null) {
                movieDetails.setWatched(newWatchedMovies.contains(tmdbId));
            }

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
     * Download movie data from TMDB (and trakt) using the {@link DisplaySettings#getMoviesLanguage(Context)}.
     *
     * @param getTraktRating Rating from TMDB is always fetched. Fetching trakt rating involves
     * looking up the trakt id first, so skip if not necessary.
     */
    public MovieDetails getMovieDetails(int movieTmdbId, boolean getTraktRating) {
        String languageCode = DisplaySettings.getMoviesLanguage(context);
        return getMovieDetails(languageCode, movieTmdbId, getTraktRating);
    }

    /**
     * Download movie data from trakt and TMDb.
     */
    private MovieDetails getMovieDetails(String languageCode, int movieTmdbId,
            boolean getTraktRating) {
        MovieDetails details = new MovieDetails();

        // load ratings from trakt
        if (getTraktRating) {
            Integer movieTraktId = lookupTraktId(movieTmdbId);
            if (movieTraktId != null) {
                details.traktRatings(loadRatingsFromTrakt(movieTraktId));
            }
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
            Errors.logAndReport("get movie rating", response);
        } catch (Exception e) {
            Errors.logAndReport("get movie rating", e);
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
                Errors.logAndReport(action, response);
            }
        } catch (Exception e) {
            Errors.logAndReport(action, e);
        }
        return null;
    }
}
