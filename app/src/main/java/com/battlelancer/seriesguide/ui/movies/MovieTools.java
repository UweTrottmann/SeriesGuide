package com.battlelancer.seriesguide.ui.movies;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.jobs.FlagJobExecutor;
import com.battlelancer.seriesguide.jobs.movies.MovieCollectionJob;
import com.battlelancer.seriesguide.jobs.movies.MovieWatchedJob;
import com.battlelancer.seriesguide.jobs.movies.MovieWatchlistJob;
import com.battlelancer.seriesguide.model.SgMovieFlags;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.MovieHelper;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.TextTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb2.entities.AppendToResponse;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem;
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
import java.util.Map;
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
    private final MovieTools2 movieTools2;

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
        this.movieTools2 = new MovieTools2();
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
        FlagJobExecutor.execute(context, new MovieCollectionJob(movieTmdbId, true));
    }

    public static void addToWatchlist(Context context, int movieTmdbId) {
        FlagJobExecutor.execute(context, new MovieWatchlistJob(movieTmdbId, true));
    }

    /**
     * Adds the movie to the given list. If it was not in any list before, adds the movie to the
     * local database first. Returns if the database operation was successful.
     */
    public boolean addToList(int movieTmdbId, Lists list) {
        boolean movieExists = isMovieInDatabase(movieTmdbId);
        if (movieExists) {
            return updateMovie(context, movieTmdbId, list, true);
        } else {
            return addMovie(movieTmdbId, list);
        }
    }

    private boolean isMovieInDatabase(int movieTmdbId) {
        int count = SgRoomDatabase.getInstance(context).movieHelper().getCount(movieTmdbId);
        return count > 0;
    }

    public static void removeFromCollection(Context context, int movieTmdbId) {
        FlagJobExecutor.execute(context, new MovieCollectionJob(movieTmdbId, false));
    }

    public static void removeFromWatchlist(Context context, int movieTmdbId) {
        FlagJobExecutor.execute(context, new MovieWatchlistJob(movieTmdbId, false));
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
            return updateMovie(context, movieTmdbId, listToRemoveFrom, false);
        }
    }

    static void watchedMovie(
            Context context,
            int movieTmdbId,
            int currentPlays,
            boolean inWatchlist
    ) {
        FlagJobExecutor.execute(
                context,
                new MovieWatchedJob(movieTmdbId, true, currentPlays)
        );
        // trakt removes from watchlist automatically, but app would not show until next sync
        // and not mirror on hexagon, so do it manually
        if (inWatchlist) {
            removeFromWatchlist(context, movieTmdbId);
        }
    }

    static void unwatchedMovie(Context context, int movieTmdbId) {
        FlagJobExecutor.execute(context, new MovieWatchedJob(movieTmdbId, false, 0));
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
        boolean isWatched = listToAddTo == Lists.WATCHED;
        details.setWatched(isWatched);
        details.setPlays(isWatched ? 1 : 0);
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
    private static boolean updateMovie(
            Context context,
            int movieTmdbId,
            Lists list,
            boolean value
    ) {
        MovieHelper helper = SgRoomDatabase.getInstance(context).movieHelper();

        int rowsUpdated;
        switch (list) {
            case COLLECTION:
                rowsUpdated = helper.updateInCollection(movieTmdbId, value);
                break;
            case WATCHLIST:
                rowsUpdated = helper.updateInWatchlist(movieTmdbId, value);
                break;
            case WATCHED:
                if (value) {
                    rowsUpdated = helper.setWatchedAndAddPlay(movieTmdbId);
                } else {
                    rowsUpdated = helper.setNotWatchedAndRemovePlays(movieTmdbId);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported Lists type " + list);
        }

        return rowsUpdated > 0;
    }

    /**
     * Updates existing movie. If movie does not exist in database, will do nothing.
     */
    public void updateMovie(MovieDetails details, int tmdbId) {
        ContentValues values = details.toContentValuesUpdate();
        if (values.size() == 0) {
            return; // nothing to update, downloading probably failed :(
        }

        values.put(SeriesGuideContract.Movies.LAST_UPDATED, System.currentTimeMillis());

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
     * @param newWatchedMoviesToPlays Movie TMDB ids to set watched mapped to play count.
     */
    public boolean addMovies(
            @NonNull Set<Integer> newCollectionMovies,
            @NonNull Set<Integer> newWatchlistMovies,
            @NonNull Map<Integer, Integer> newWatchedMoviesToPlays
    ) {
        Timber.d(
                "addMovies: %s to collection, %s to watchlist, %s to watched",
                newCollectionMovies.size(),
                newWatchlistMovies.size(),
                newWatchedMoviesToPlays.size()
        );

        // build a single list of tmdb ids
        Set<Integer> newMovies = new HashSet<>();
        newMovies.addAll(newCollectionMovies);
        newMovies.addAll(newWatchlistMovies);
        newMovies.addAll(newWatchedMoviesToPlays.keySet());

        String languageCode = DisplaySettings.getMoviesLanguage(context);
        String regionCode = DisplaySettings.getMoviesRegion(context);
        List<MovieDetails> movies = new LinkedList<>();

        // loop through ids
        for (Iterator<Integer> iterator = newMovies.iterator(); iterator.hasNext(); ) {
            int tmdbId = iterator.next();
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("addMovies: no network connection");
                return false;
            }

            // download movie data
            MovieDetails movieDetails = getMovieDetails(languageCode, regionCode, tmdbId, false);
            if (movieDetails.tmdbMovie() == null) {
                // skip if minimal values failed to load
                Timber.d("addMovies: downloaded movie %s incomplete, skipping", tmdbId);
                continue;
            }

            // set flags
            movieDetails.setInCollection(newCollectionMovies.contains(tmdbId));
            movieDetails.setInWatchlist(newWatchlistMovies.contains(tmdbId));
            Integer plays = newWatchedMoviesToPlays.get(tmdbId);
            boolean isWatched = plays != null;
            movieDetails.setWatched(isWatched);
            movieDetails.setPlays(isWatched ? plays : 0);

            movies.add(movieDetails);

            // Already add to the database if we have 10 movies so UI can already update.
            if (movies.size() == 10) {
                context.getContentResolver().bulkInsert(SeriesGuideContract.Movies.CONTENT_URI,
                        buildMoviesContentValues(movies));
                movies.clear(); // Start a new batch.
            }
        }

        // Insert remaining new movies into the database.
        if (!movies.isEmpty()) {
            context.getContentResolver().bulkInsert(SeriesGuideContract.Movies.CONTENT_URI,
                    buildMoviesContentValues(movies));
        }

        return true;
    }

    /**
     * Download movie data from TMDB (and trakt) using
     * {@link DisplaySettings#getMoviesLanguage(Context)}
     * and {@link DisplaySettings#getMoviesRegion(Context)}.
     *
     * @param getTraktRating Rating from TMDB is always fetched. Fetching trakt rating involves
     * looking up the trakt id first, so skip if not necessary.
     */
    public MovieDetails getMovieDetails(int movieTmdbId, boolean getTraktRating) {
        String languageCode = DisplaySettings.getMoviesLanguage(context);
        String regionCode = DisplaySettings.getMoviesRegion(context);
        return getMovieDetails(languageCode, regionCode, movieTmdbId, getTraktRating);
    }

    /**
     * Download movie data from TMDB (and trakt).
     *
     * @param getTraktRating Rating from TMDB is always fetched. Fetching trakt rating involves
     * looking up the trakt id first, so skip if not necessary.
     */
    public MovieDetails getMovieDetails(String languageCode, String regionCode, int movieTmdbId,
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
        details.tmdbMovie(loadSummaryFromTmdb(languageCode, regionCode, movieTmdbId));

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
    private com.uwetrottmann.tmdb2.entities.Movie loadSummaryFromTmdb(
            @Nullable String languageCode,
            String regionCode,
            int movieTmdbId
    ) {
        // try to get local movie summary
        Movie movie = getMovieSummary("get local movie summary",
                languageCode, movieTmdbId, true);
        if (movie != null && !TextUtils.isEmpty(movie.overview)) {
            movieTools2.updateReleaseDateForRegion(movie, movie.release_dates, regionCode);
            return movie;
        }

        // fall back to default language if TMDb has no localized text
        Movie movieFallback = getMovieSummary("get default movie summary",
                null, movieTmdbId, false);
        if (movieFallback != null) {
            // add note about non-translated or non-existing overview
            String untranslatedOverview = movieFallback.overview;
            movieFallback.overview = TextTools
                    .textNoTranslationMovieLanguage(context, languageCode);
            if (!TextUtils.isEmpty(untranslatedOverview)) {
                movieFallback.overview += "\n\n" + untranslatedOverview;
            }
            if (movie != null) {
                movieTools2.updateReleaseDateForRegion(movie, movie.release_dates, regionCode);
            }
        }
        return movieFallback;
    }

    @Nullable
    public Movie getMovieSummary(int movieTmdbId) {
        String languageCode = DisplaySettings.getMoviesLanguage(context);
        return getMovieSummary("get local movie summary", languageCode, movieTmdbId, false);
    }

    @Nullable
    private Movie getMovieSummary(@NonNull String action, @Nullable String language,
            int movieTmdbId, boolean includeReleaseDates) {
        try {
            Response<Movie> response = tmdbMovies.get()
                    .summary(
                            movieTmdbId,
                            language,
                            includeReleaseDates
                                    ? new AppendToResponse(AppendToResponseItem.RELEASE_DATES)
                                    : null
                    )
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
