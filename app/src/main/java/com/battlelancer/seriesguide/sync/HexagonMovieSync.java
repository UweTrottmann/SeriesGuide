package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.movies.Movies;
import com.uwetrottmann.seriesguide.backend.movies.model.Movie;
import com.uwetrottmann.seriesguide.backend.movies.model.MovieList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import timber.log.Timber;

public class HexagonMovieSync {

    private Context context;
    private HexagonTools hexagonTools;

    HexagonMovieSync(Context context, HexagonTools hexagonTools) {
        this.context = context;
        this.hexagonTools = hexagonTools;
    }

    /**
     * Downloads movies from hexagon, updates existing movies with new properties, removes
     * movies that are neither in collection or watchlist.
     *
     * <p> Adds movie tmdb ids to the respective collection or watchlist set.
     */
    public boolean download(@NonNull Set<Integer> newCollectionMovies,
            @NonNull Set<Integer> newWatchlistMovies, boolean hasMergedMovies) {
        List<Movie> movies;
        boolean hasMoreMovies = true;
        String cursor = null;
        long currentTime = System.currentTimeMillis();
        DateTime lastSyncTime = new DateTime(HexagonSettings.getLastMoviesSyncTime(context));
        HashSet<Integer> localMovies = MovieTools.getMovieTmdbIdsAsSet(context);
        if (localMovies == null) {
            Timber.e("download: querying for local movies failed.");
            return false;
        }

        if (hasMergedMovies) {
            Timber.d("download: movies changed since %s", lastSyncTime);
        } else {
            Timber.d("download: all movies");
        }

        while (hasMoreMovies) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.e("download: no network connection");
                return false;
            }

            try {
                // get service each time to check if auth was removed
                Movies moviesService = hexagonTools.getMoviesService();
                if (moviesService == null) {
                    return false;
                }

                Movies.Get request = moviesService.get();  // use default server limit
                if (hasMergedMovies) {
                    request.setUpdatedSince(lastSyncTime);
                }
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                MovieList response = request.execute();
                if (response == null) {
                    // nothing more to do
                    Timber.d("download: response was null, done here");
                    break;
                }

                movies = response.getMovies();

                if (response.getCursor() != null) {
                    cursor = response.getCursor();
                } else {
                    hasMoreMovies = false;
                }
            } catch (IOException e) {
                HexagonTools.trackFailedRequest(context, "get movies", e);
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
                                    movie.getIsInCollection() ? 1 : 0);
                        }
                        if (movie.getIsInWatchlist() != null) {
                            values.put(SeriesGuideContract.Movies.IN_WATCHLIST,
                                    movie.getIsInWatchlist() ? 1 : 0);
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
                Timber.e(e, "download: applying movie updates failed");
                return false;
            }
        }

        // set new last sync time
        if (hasMergedMovies) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(HexagonSettings.KEY_LAST_SYNC_MOVIES, currentTime)
                    .apply();
        }

        return true;
    }

    /**
     * Uploads all local movies to Hexagon.
     */
    public boolean uploadAll() {
        Timber.d("uploadAll: uploading all movies");

        List<Movie> movies = buildMovieList();
        if (movies == null) {
            Timber.e("uploadAll: movie query was null");
            return false;
        }
        if (movies.size() == 0) {
            // nothing to do
            Timber.d("uploadAll: no movies to upload");
            return true;
        }

        MovieList movieList = new MovieList();
        movieList.setMovies(movies);

        try {
            // get service each time to check if auth was removed
            Movies moviesService = hexagonTools.getMoviesService();
            if (moviesService == null) {
                return false;
            }
            moviesService.save(movieList).execute();
        } catch (IOException e) {
            HexagonTools.trackFailedRequest(context, "save movies", e);
            return false;
        }

        return true;
    }

    private List<Movie> buildMovieList() {
        List<Movie> movies = new ArrayList<>();

        // query for movies in lists (excluding movies that are only watched)
        Cursor moviesInLists = context.getContentResolver().query(
                SeriesGuideContract.Movies.CONTENT_URI,
                SeriesGuideContract.Movies.PROJECTION_IN_LIST,
                SeriesGuideContract.Movies.SELECTION_IN_LIST, null, null);
        if (moviesInLists == null) {
            return null;
        }

        while (moviesInLists.moveToNext()) {
            Movie movie = new Movie();
            movie.setTmdbId(moviesInLists.getInt(0));
            movie.setIsInCollection(moviesInLists.getInt(1) == 1);
            movie.setIsInWatchlist(moviesInLists.getInt(2) == 1);
            movies.add(movie);
        }

        moviesInLists.close();

        return movies;
    }
}
