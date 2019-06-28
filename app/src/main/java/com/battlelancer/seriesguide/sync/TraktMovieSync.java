package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
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
     * or watchlist instead of removing them locally. Does NOT upload watched movies missing from
     * trakt (trakt is considered the truth).
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

        // loop through local movies to build updates
        HashSet<Integer> localMovies = MovieTools.getMovieTmdbIdsAsSet(context);
        if (localMovies == null) {
            Timber.e("syncLists: querying local movies failed");
            return false;
        }
        Set<Integer> moviesNotOnTraktCollection = new HashSet<>(); // only when merging
        Set<Integer> moviesNotOnTraktWatchlist = new HashSet<>(); // only when merging
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (Integer tmdbId : localMovies) {
            // is local movie in trakt collection, watchlist or watched?
            boolean inCollection = collection.remove(tmdbId);
            boolean inWatchlist = watchlist.remove(tmdbId);
            boolean isWatched = watched.remove(tmdbId);

            if (merging) {
                // maybe upload movie if missing from trakt collection or watchlist
                // but not if watched (considering trakt truth on watched state)
                if (!inCollection) {
                    moviesNotOnTraktCollection.add(tmdbId);
                }
                if (!inWatchlist) {
                    moviesNotOnTraktWatchlist.add(tmdbId);
                }
                // add to local collection or watchlist, but do NOT remove
                ContentProviderOperation.Builder builder = ContentProviderOperation
                        .newUpdate(SeriesGuideContract.Movies.buildMovieUri(tmdbId));
                if (inCollection || inWatchlist) {
                    if (inCollection) {
                        builder.withValue(SeriesGuideContract.Movies.IN_COLLECTION, true);
                    }
                    if (inWatchlist) {
                        builder.withValue(SeriesGuideContract.Movies.IN_WATCHLIST, true);
                    }
                }
                // update watched state
                // will take care of removing unneeded (not watched or in any list) movies
                // in later sync step
                builder.withValue(SeriesGuideContract.Movies.WATCHED, isWatched);
                batch.add(builder.build());
            } else {
                // mirror trakt collection, watchlist and watched flag
                // will take care of removing unneeded (not watched or in any list) movies
                // in later sync step
                ContentProviderOperation op = ContentProviderOperation
                        .newUpdate(SeriesGuideContract.Movies.buildMovieUri(tmdbId))
                        .withValue(SeriesGuideContract.Movies.IN_COLLECTION, inCollection)
                        .withValue(SeriesGuideContract.Movies.IN_WATCHLIST, inWatchlist)
                        .withValue(SeriesGuideContract.Movies.WATCHED, isWatched)
                        .build();
                batch.add(op);
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
            if (!uploadLists(moviesNotOnTraktCollection, moviesNotOnTraktWatchlist)) {
                return false;
            } else {
                // set merge successful
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putBoolean(TraktSettings.KEY_HAS_MERGED_MOVIES, true)
                        .apply();
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
     * Checks if the given movies are in the local collection or watchlist, then uploads them to the
     * appropriate list(s) on trakt.
     */
    private boolean uploadLists(Set<Integer> moviesNotOnTraktCollection,
            Set<Integer> moviesNotOnTraktWatchlist) {
        if (moviesNotOnTraktCollection.size() == 0 && moviesNotOnTraktWatchlist.size() == 0) {
            // nothing to upload
            Timber.d("uploadLists: nothing to uploadLists");
            return true;
        }

        // return if connectivity is lost
        if (!AndroidUtils.isNetworkConnected(context)) {
            return false;
        }

        // query for movies in lists (excluding movies that are only watched)
        Cursor moviesInLists = context.getContentResolver()
                .query(SeriesGuideContract.Movies.CONTENT_URI,
                        SeriesGuideContract.Movies.PROJECTION_IN_LIST,
                        SeriesGuideContract.Movies.SELECTION_IN_LIST, null, null);
        if (moviesInLists == null) {
            Timber.e("uploadLists: query failed");
            return false;
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
        String action = null;
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

        Timber.d("uploadLists: success, uploaded %s to collection, %s to watchlist",
                moviesToCollect.size(), moviesToWatchlist.size());
        return true;
    }
}
