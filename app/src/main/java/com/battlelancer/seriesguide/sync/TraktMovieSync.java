package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.model.SgMovieFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.BaseMovie;
import com.uwetrottmann.trakt5.entities.LastActivityMore;
import com.uwetrottmann.trakt5.entities.MovieIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.services.Sync;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import retrofit2.Response;
import timber.log.Timber;

public class TraktMovieSync {

    private static final String ACTION_GET_COLLECTION = "get movie collection";
    private static final String ACTION_GET_WATCHLIST = "get movie watchlist";
    private static final String ACTION_GET_WATCHED = "get watched movies";

    private Context context;
    private MovieTools movieTools;
    private Sync traktSync;

    public TraktMovieSync(Context context, MovieTools movieTools, Sync traktSync) {
        this.context = context;
        this.movieTools = movieTools;
        this.traktSync = traktSync;
    }

    /**
     * Updates the local movie database against trakt movie watchlist, collection and watched
     * movies. Adds or updates movies in the database. Movies not in any list or not watched must be
     * removed afterwards.
     *
     * <p> When syncing the first time, will upload any local movies missing from trakt collection
     * or watchlist or are not watched on Trakt instead of removing them locally.
     *
     * <p> Performs <b>synchronous network access</b>, make sure to run this on a background
     * thread.
     */
    boolean syncLists(LastActivityMore activity) {
        if (activity.collected_at == null) {
            Timber.e("syncLists: null collected_at");
            return false;
        }
        if (activity.watchlisted_at == null) {
            Timber.e("syncLists: null watchlisted_at");
            return false;
        }
        if (activity.watched_at == null) {
            Timber.e("syncLists: null watched_at");
            return false;
        }

        final boolean merging = !TraktSettings.hasMergedMovies(context);
        if (!merging && !TraktSettings.isMovieListsChanged(
                context, activity.collected_at, activity.watchlisted_at, activity.watched_at
        )) {
            Timber.d("syncLists: no changes");
            return true;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false;
        }

        // download trakt state
        Set<Integer> collection = downloadCollection();
        if (collection == null) {
            return false;
        }
        Set<Integer> watchlist = downloadWatchlist();
        if (watchlist == null) {
            return false;
        }
        Set<Integer> watched = downloadWatched();
        if (watched == null) {
            return false;
        }

        // Loop through local movies to build updates.
        List<SgMovieFlags> localMovies;
        try {
            localMovies = SgRoomDatabase.getInstance(context).movieHelper().getMovieFlags();
        } catch (Exception e) {
            Errors.logAndReport("syncLists: query local movies", e);
            return false;
        }

        Set<Integer> moviesNotOnTraktCollection = new HashSet<>(); // only when merging
        Set<Integer> moviesNotOnTraktWatchlist = new HashSet<>(); // only when merging
        Set<Integer> moviesNotWatchedOnTrakt = new HashSet<>(); // only when merging
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        for (SgMovieFlags localMovie : localMovies) {
            // Is local movie in trakt collection, watchlist or watched?
            int tmdbId = localMovie.getTmdbId();
            boolean inCollectionOnTrakt = collection.remove(tmdbId);
            boolean inWatchlistOnTrakt = watchlist.remove(tmdbId);
            boolean isWatchedOnTrakt = watched.remove(tmdbId);

            if (merging) {
                // Maybe upload movie if missing from Trakt collection or watchlist
                // or if not watched on Trakt.
                if (!inCollectionOnTrakt) {
                    moviesNotOnTraktCollection.add(tmdbId);
                }
                if (!inWatchlistOnTrakt) {
                    moviesNotOnTraktWatchlist.add(tmdbId);
                }
                if (!isWatchedOnTrakt) {
                    moviesNotWatchedOnTrakt.add(tmdbId);
                }
                // Add to local collection or watchlist, but do NOT remove.
                // Mark as watched, but do NOT remove watched flag.
                // Will take care of removing unneeded (not watched or in any list) movies
                // in later sync step.
                if (inCollectionOnTrakt || inWatchlistOnTrakt || isWatchedOnTrakt) {
                    ContentProviderOperation.Builder builder = ContentProviderOperation
                            .newUpdate(Movies.buildMovieUri(tmdbId));
                    if (inCollectionOnTrakt) {
                        builder.withValue(Movies.IN_COLLECTION, true);
                    }
                    if (inWatchlistOnTrakt) {
                        builder.withValue(Movies.IN_WATCHLIST, true);
                    }
                    if (isWatchedOnTrakt) {
                        builder.withValue(Movies.WATCHED, true);
                    }
                    batch.add(builder.build());
                }
            } else {
                // Performance: only add op if any flag differs.
                if (localMovie.getInCollection() != inCollectionOnTrakt
                        || localMovie.getInWatchlist() != inWatchlistOnTrakt
                        || localMovie.getWatched() != isWatchedOnTrakt) {
                    // Mirror Trakt collection, watchlist and watched flag.
                    // Note: unneeded (not watched or in any list) movies
                    // are removed in a later sync step.
                    ContentProviderOperation op = ContentProviderOperation
                            .newUpdate(Movies.buildMovieUri(tmdbId))
                            .withValue(Movies.IN_COLLECTION, inCollectionOnTrakt)
                            .withValue(Movies.IN_WATCHLIST, inWatchlistOnTrakt)
                            .withValue(Movies.WATCHED, isWatchedOnTrakt)
                            .build();
                    batch.add(op);
                }
            }
        }

        // apply updates to existing movies
        try {
            DBUtils.applyInSmallBatches(context, batch);
            Timber.d("syncLists: updated %s", batch.size());
        } catch (OperationApplicationException e) {
            Timber.e(e, "syncLists: database updates failed");
            return false;
        }
        batch.clear(); // release for gc

        // merge on first run
        if (merging) {
            // upload movies not in trakt collection or watchlist
            if (uploadLists(
                    moviesNotOnTraktCollection,
                    moviesNotOnTraktWatchlist,
                    moviesNotWatchedOnTrakt
            )) {
                // set merge successful
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, true)
                        .apply();
            } else {
                return false;
            }
        }

        // add movies from trakt missing locally
        // all local movies were removed from trakt collection, watchlist, and watched list
        // so they only contain movies missing locally
        boolean addingSuccessful = movieTools.addMovies(collection, watchlist, watched);
        if (addingSuccessful) {
            // store last activity timestamps
            TraktSettings.storeLastMoviesChangedAt(
                    context,
                    activity.collected_at,
                    activity.watchlisted_at,
                    activity.watched_at
            );
            // if movies were added, ensure ratings for them are downloaded next
            if (collection.size() > 0 || watchlist.size() > 0 || watched.size() > 0) {
                TraktSettings.resetMoviesLastRatedAt(context);
            }
        }

        return addingSuccessful;
    }

    @Nullable
    private Set<Integer> downloadCollection() {
        try {
            Response<List<BaseMovie>> response = traktSync
                    .collectionMovies(null)
                    .execute();
            return verifyListResponse(response, "null collection response", ACTION_GET_COLLECTION);
        } catch (Exception e) {
            Errors.logAndReport(ACTION_GET_COLLECTION, e);
            return null;
        }
    }

    @Nullable
    private Set<Integer> downloadWatchlist() {
        try {
            Response<List<BaseMovie>> response = traktSync
                    .watchlistMovies(null)
                    .execute();
            return verifyListResponse(response, "null watchlist response", ACTION_GET_WATCHLIST);
        } catch (Exception e) {
            Errors.logAndReport(ACTION_GET_WATCHLIST, e);
            return null;
        }
    }

    @Nullable
    private Set<Integer> downloadWatched() {
        try {
            Response<List<BaseMovie>> response = traktSync
                    .watchedMovies(null)
                    .execute();
            return verifyListResponse(response, "null watched response", ACTION_GET_WATCHED);
        } catch (Exception e) {
            Errors.logAndReport(ACTION_GET_WATCHED, e);
            return null;
        }
    }

    @Nullable
    private Set<Integer> verifyListResponse(
            Response<List<BaseMovie>> response,
            String nullResponse,
            String action
    ) {
        if (response.isSuccessful()) {
            Set<Integer> tmdbIdSet = buildTmdbIdSet(response.body());
            if (tmdbIdSet == null) {
                Timber.e(nullResponse);
            }
            return tmdbIdSet;
        } else {
            if (SgTrakt.isUnauthorized(context, response)) {
                return null;
            }
            Errors.logAndReport(action, response);
            return null;
        }
    }

    @Nullable
    private Set<Integer> buildTmdbIdSet(@Nullable List<BaseMovie> movies) {
        if (movies == null) {
            return null;
        }

        Set<Integer> tmdbIdSet = new HashSet<>();
        for (BaseMovie movie : movies) {
            if (movie.movie == null || movie.movie.ids == null || movie.movie.ids.tmdb == null) {
                continue; // skip invalid values
            }
            tmdbIdSet.add(movie.movie.ids.tmdb);
        }
        return tmdbIdSet;
    }

    /**
     * Checks if the given movies are in the local collection or watchlist or are watched,
     * then uploads them to the appropriate list(s) on Trakt.
     */
    private boolean uploadLists(
            Set<Integer> moviesNotOnTraktCollection,
            Set<Integer> moviesNotOnTraktWatchlist,
            Set<Integer> moviesNotWatchedOnTrakt
    ) {
        if (moviesNotOnTraktCollection.size() == 0
                && moviesNotOnTraktWatchlist.size() == 0
                && moviesNotWatchedOnTrakt.size() == 0) {
            // nothing to upload
            Timber.d("uploadLists: nothing to uploadLists");
            return true;
        }

        // return if connectivity is lost
        if (!AndroidUtils.isNetworkConnected(context)) {
            return false;
        }

        List<SgMovieFlags> moviesOnListsOrWatched = SgRoomDatabase.getInstance(context)
                .movieHelper().getMoviesOnListsOrWatched();

        // Build list of collected, watchlisted or watched movies to upload.
        List<SyncMovie> moviesToCollect = new LinkedList<>();
        List<SyncMovie> moviesToWatchlist = new LinkedList<>();
        List<SyncMovie> moviesToSetWatched = new LinkedList<>();

        for (SgMovieFlags movie : moviesOnListsOrWatched) {
            int tmdbId = movie.getTmdbId();
            SyncMovie syncMovie = new SyncMovie().id(MovieIds.tmdb(tmdbId));

            if (movie.getInCollection()
                    && moviesNotOnTraktCollection.contains(tmdbId)) {
                moviesToCollect.add(syncMovie);
            }
            if (movie.getInWatchlist()
                    && moviesNotOnTraktWatchlist.contains(tmdbId)) {
                moviesToWatchlist.add(syncMovie);
            }
            if (movie.getWatched()
                    && moviesNotWatchedOnTrakt.contains(tmdbId)) {
                moviesToSetWatched.add(syncMovie);
            }
        }

        // upload
        String action = "";
        SyncItems items = new SyncItems();
        Response<SyncResponse> response = null;
        try {
            if (moviesToCollect.size() > 0) {
                action = "add movies to collection";
                items.movies(moviesToCollect);
                response = traktSync.addItemsToCollection(items).execute();
            }
            if (response == null || response.isSuccessful()) {
                if (moviesToWatchlist.size() > 0) {
                    action = "add movies to watchlist";
                    items.movies(moviesToWatchlist);
                    response = traktSync.addItemsToWatchlist(items).execute();
                }
            }
            if (response == null || response.isSuccessful()) {
                if (moviesToSetWatched.size() > 0) {
                    // Note: not setting a watched date (because not having one),
                    // so Trakt will use the movie release date.
                    action = "add movies to watched history";
                    items.movies(moviesToSetWatched);
                    response = traktSync.addItemsToWatchedHistory(items).execute();
                }
            }
        } catch (Exception e) {
            Errors.logAndReport(action, e);
            return false;
        }
        if (response != null && !response.isSuccessful()) {
            if (SgTrakt.isUnauthorized(context, response)) {
                return false;
            }
            Errors.logAndReport(action, response);
            return false;
        }

        Timber.d("uploadLists: success, uploaded %s to collection, %s to watchlist, %s set watched",
                moviesToCollect.size(), moviesToWatchlist.size(), moviesToSetWatched.size());
        return true;
    }
}
