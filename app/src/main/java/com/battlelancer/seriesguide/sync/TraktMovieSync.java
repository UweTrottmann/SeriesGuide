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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        Map<Integer, Integer> watchedWithPlays = downloadWatched();
        if (watchedWithPlays == null) {
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

        Set<Integer> toCollectOnTrakt = new HashSet<>(); // only when merging
        Set<Integer> toWatchlistOnTrakt = new HashSet<>(); // only when merging
        Set<Integer> toSetWatchedOnTrakt = new HashSet<>(); // only when merging
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        for (SgMovieFlags localMovie : localMovies) {
            // Is local movie in trakt collection, watchlist or watched?
            int tmdbId = localMovie.getTmdbId();
            boolean inCollectionOnTrakt = collection.remove(tmdbId);
            boolean inWatchlistOnTrakt = watchlist.remove(tmdbId);
            Integer plays = watchedWithPlays.remove(tmdbId);
            boolean isWatchedOnTrakt = plays != null;

            if (merging) {
                // Mark movie for upload if missing from Trakt collection or watchlist
                // or if not watched on Trakt.
                // Note: If watches were removed on Trakt in the meanwhile, this would re-add them.
                // But this should be the exception and not losing watches should be the
                // desired behavior for most users.

                if (localMovie.getInCollection() && !inCollectionOnTrakt) {
                    toCollectOnTrakt.add(tmdbId);
                }
                if (localMovie.getInWatchlist() && !inWatchlistOnTrakt) {
                    toWatchlistOnTrakt.add(tmdbId);
                }
                if (localMovie.getWatched() && !isWatchedOnTrakt) {
                    toSetWatchedOnTrakt.add(tmdbId);
                }

                // Add to local collection or watchlist, but do NOT remove.
                // Mark as watched, but do NOT remove watched flag.
                // Will take care of removing unneeded (not watched or in any list) movies
                // in later sync step.
                if (inCollectionOnTrakt || inWatchlistOnTrakt || isWatchedOnTrakt) {

                    ContentProviderOperation.Builder builder = ContentProviderOperation
                            .newUpdate(Movies.buildMovieUri(tmdbId));
                    boolean changed = false;

                    if (!localMovie.getInCollection() && inCollectionOnTrakt) {
                        builder.withValue(Movies.IN_COLLECTION, true);
                        changed = true;
                    }
                    if (!localMovie.getInWatchlist() && inWatchlistOnTrakt) {
                        builder.withValue(Movies.IN_WATCHLIST, true);
                        changed = true;
                    }
                    if (!localMovie.getWatched() && isWatchedOnTrakt) {
                        builder.withValue(Movies.WATCHED, true);
                        builder.withValue(Movies.PLAYS, plays >= 1 ? plays : 1);
                        changed = true;
                    }

                    if (changed) {
                        batch.add(builder.build());
                    }
                }
            } else {
                // Performance: only add op if any flag differs or if watched and plays have changed.
                if (localMovie.getInCollection() != inCollectionOnTrakt
                        || localMovie.getInWatchlist() != inWatchlistOnTrakt
                        || localMovie.getWatched() != isWatchedOnTrakt
                        || (isWatchedOnTrakt && plays >= 1 && localMovie.getPlays() != plays)) {
                    // Mirror Trakt collection, watchlist, watched flag and plays.
                    // Note: unneeded (not watched or in any list) movies
                    // are removed in a later sync step.
                    ContentProviderOperation.Builder op = ContentProviderOperation
                            .newUpdate(Movies.buildMovieUri(tmdbId))
                            .withValue(Movies.IN_COLLECTION, inCollectionOnTrakt)
                            .withValue(Movies.IN_WATCHLIST, inWatchlistOnTrakt)
                            .withValue(Movies.WATCHED, isWatchedOnTrakt);
                    int playsValue;
                    if (isWatchedOnTrakt) {
                        playsValue = plays >= 1 ? plays : 1;
                    } else {
                        playsValue = 0;
                    }
                    op.withValue(Movies.PLAYS, playsValue);
                    batch.add(op.build());
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
            // Upload movies not in Trakt collection, watchlist or watched history.
            if (uploadFlagsNotOnTrakt(
                    toCollectOnTrakt,
                    toWatchlistOnTrakt,
                    toSetWatchedOnTrakt
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
        boolean addingSuccessful = movieTools.addMovies(collection, watchlist, watchedWithPlays);
        if (addingSuccessful) {
            // store last activity timestamps
            TraktSettings.storeLastMoviesChangedAt(
                    context,
                    activity.collected_at,
                    activity.watchlisted_at,
                    activity.watched_at
            );
            // if movies were added, ensure ratings for them are downloaded next
            if (collection.size() > 0 || watchlist.size() > 0 || watchedWithPlays.size() > 0) {
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
            List<BaseMovie> collection = verifyListResponse(response,
                    "null collection response", ACTION_GET_COLLECTION);
            return toTmdbIdSet(collection);
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
            List<BaseMovie> watchlist = verifyListResponse(response,
                    "null watchlist response", ACTION_GET_WATCHLIST);
            return toTmdbIdSet(watchlist);
        } catch (Exception e) {
            Errors.logAndReport(ACTION_GET_WATCHLIST, e);
            return null;
        }
    }

    @Nullable
    private Map<Integer, Integer> downloadWatched() {
        try {
            Response<List<BaseMovie>> response = traktSync
                    .watchedMovies(null)
                    .execute();
            List<BaseMovie> watched = verifyListResponse(response,
                    "null watched response", ACTION_GET_WATCHED);
            return mapTmdbIdToPlays(watched);
        } catch (Exception e) {
            Errors.logAndReport(ACTION_GET_WATCHED, e);
            return null;
        }
    }

    @Nullable
    private List<BaseMovie> verifyListResponse(
            Response<List<BaseMovie>> response,
            String nullResponse,
            String action
    ) {
        if (response.isSuccessful()) {
            List<BaseMovie> movies = response.body();
            if (movies == null) {
                Timber.e(nullResponse);
            }
            return movies;
        } else {
            if (SgTrakt.isUnauthorized(context, response)) {
                return null;
            }
            Errors.logAndReport(action, response);
            return null;
        }
    }

    @Nullable
    private Set<Integer> toTmdbIdSet(@Nullable List<BaseMovie> movies) {
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

    @Nullable
    private Map<Integer, Integer> mapTmdbIdToPlays(@Nullable List<BaseMovie> movies) {
        if (movies == null) {
            return null;
        }

        Map<Integer, Integer> map = new HashMap<>();
        for (BaseMovie movie : movies) {
            if (movie.movie == null || movie.movie.ids == null || movie.movie.ids.tmdb == null) {
                continue; // skip invalid values
            }
            map.put(movie.movie.ids.tmdb, movie.plays);
        }
        return map;
    }

    /**
     * Uploads the given movies to the appropriate list(s)/history on Trakt.
     */
    private boolean uploadFlagsNotOnTrakt(
            Set<Integer> toCollectOnTrakt,
            Set<Integer> toWatchlistOnTrakt,
            Set<Integer> toSetWatchedOnTrakt
    ) {
        if (toCollectOnTrakt.size() == 0
                && toWatchlistOnTrakt.size() == 0
                && toSetWatchedOnTrakt.size() == 0) {
            Timber.d("uploadLists: nothing to upload");
            return true;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            return false; // Fail, no connection is available.
        }

        // Upload.
        String action = "";
        SyncItems items = new SyncItems();
        Response<SyncResponse> response = null;

        try {
            if (toCollectOnTrakt.size() > 0) {
                List<SyncMovie> moviesToCollect =
                        convertToSyncMovieList(toCollectOnTrakt);
                action = "add movies to collection";
                items.movies(moviesToCollect);
                response = traktSync.addItemsToCollection(items).execute();
            }

            if (response == null || response.isSuccessful()) {
                if (toWatchlistOnTrakt.size() > 0) {
                    List<SyncMovie> moviesToWatchlist =
                            convertToSyncMovieList(toWatchlistOnTrakt);
                    action = "add movies to watchlist";
                    items.movies(moviesToWatchlist);
                    response = traktSync.addItemsToWatchlist(items).execute();
                }
            }

            if (response == null || response.isSuccessful()) {
                if (toSetWatchedOnTrakt.size() > 0) {
                    List<SyncMovie> moviesToSetWatched =
                            convertToSyncMovieList(toSetWatchedOnTrakt);
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
                toCollectOnTrakt.size(), toWatchlistOnTrakt.size(), toSetWatchedOnTrakt.size());
        return true;
    }

    private List<SyncMovie> convertToSyncMovieList(Set<Integer> movieTmdbIds) {
        List<SyncMovie> syncMovies = new LinkedList<>();
        for (Integer tmdbId : movieTmdbIds) {
            syncMovies.add(new SyncMovie().id(MovieIds.tmdb(tmdbId)));
        }
        return syncMovies;
    }
}
