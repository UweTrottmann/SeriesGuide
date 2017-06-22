package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.BaseMovie;
import com.uwetrottmann.trakt5.entities.LastActivityMore;
import com.uwetrottmann.trakt5.entities.MovieIds;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncMovie;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.services.Sync;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.threeten.bp.OffsetDateTime;
import retrofit2.Response;
import timber.log.Timber;

public class TraktMovieSync {

    private Context context;
    private MovieTools movieTools;
    private Sync traktSync;

    public TraktMovieSync(Context context, MovieTools movieTools, Sync traktSync) {
        this.context = context;
        this.movieTools = movieTools;
        this.traktSync = traktSync;
    }

    /**
     * Updates the local movie database against trakt movie watchlist and collection. Adds, updates
     * and removes movies in the database.
     *
     * <p> When syncing the first time, will upload any local movies missing from trakt collection
     * or watchlist instead of removing them locally.
     *
     * <p> Performs <b>synchronous network access</b>, make sure to run this on a background
     * thread.
     */
    public boolean syncLists(LastActivityMore activity) {
        if (activity.collected_at == null) {
            Timber.e("syncLists: null collected_at");
            return false;
        }
        if (activity.watchlisted_at == null) {
            Timber.e("syncLists: null watchlisted_at");
            return false;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false;
        }

        final boolean merging = !TraktSettings.hasMergedMovies(context);
        if (!merging && !TraktSettings.isMovieListsChanged(context, activity.collected_at,
                activity.watchlisted_at)) {
            Timber.d("syncLists: no changes");
            return true;
        }

        // download collection
        Set<Integer> collection;
        try {
            Response<List<BaseMovie>> response = traktSync
                    .collectionMovies(null)
                    .execute();
            if (response.isSuccessful()) {
                collection = buildTmdbIdSet(response.body());
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false;
                }
                SgTrakt.trackFailedRequest(context, "get movie collection", response);
                return false;
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get movie collection", e);
            return false;
        }
        if (collection == null) {
            Timber.e("syncLists: null collection response");
            return false;
        }
        // download watchlist
        Set<Integer> watchlist;
        try {
            Response<List<BaseMovie>> response = traktSync
                    .watchlistMovies(null)
                    .execute();
            if (response.isSuccessful()) {
                watchlist = buildTmdbIdSet(response.body());
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false;
                }
                SgTrakt.trackFailedRequest(context, "get movie watchlist", response);
                return false;
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get movie watchlist", e);
            return false;
        }
        if (watchlist == null) {
            Timber.e("syncLists: null watchlist response");
            return false;
        }

        // build updates
        // loop through all local movies
        Set<Integer> moviesNotOnTraktCollection = new HashSet<>();
        Set<Integer> moviesNotOnTraktWatchlist = new HashSet<>();
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        HashSet<Integer> localMovies = MovieTools.getMovieTmdbIdsAsSet(context);
        if (localMovies == null) {
            Timber.e("syncLists: querying local movies failed");
            return false;
        }
        for (Integer tmdbId : localMovies) {
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
            Timber.d("syncLists: updated %s", batch.size());
        } catch (OperationApplicationException e) {
            Timber.e(e, "syncLists: database updates failed");
            return false;
        }
        batch.clear();

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
        // all local movies were removed from trakt collection and watchlist,
        // so they only contain movies missing locally
        boolean addingSuccessful = movieTools.addMovies(collection, watchlist);
        if (addingSuccessful) {
            // store last activity timestamps
            TraktSettings.storeLastMoviesChangedAt(context, activity.collected_at,
                    activity.watchlisted_at);
            // if movies were added,
            // ensure all movie ratings and watched flags are downloaded next
            if (collection.size() > 0 || watchlist.size() > 0) {
                TraktSettings.resetMoviesLastActivity(context);
            }
        }

        return addingSuccessful;
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
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, action, e);
            return false;
        }
        if (response != null && !response.isSuccessful()) {
            if (SgTrakt.isUnauthorized(context, response)) {
                return false;
            }
            SgTrakt.trackFailedRequest(context, action, response);
            return false;
        }

        Timber.d("uploadLists: success, uploaded %s to collection, %s to watchlist",
                moviesToCollect.size(), moviesToWatchlist.size());
        return true;
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
     * Downloads trakt movie watched flags and mirrors them in the local database. Does NOT upload
     * any flags (e.g. trakt is considered the truth).
     */
    public boolean downloadWatched(OffsetDateTime watchedAt) {
        if (watchedAt == null) {
            Timber.e("downloadWatched: null watched_at");
            return false;
        }

        long lastWatchedAt = TraktSettings.getLastMoviesWatchedAt(context);
        if (!TimeTools.isAfterMillis(watchedAt, lastWatchedAt)) {
            // not initial sync, no watched flags have changed
            Timber.d("downloadWatched: no changes since %tF %tT", lastWatchedAt, lastWatchedAt);
            return true;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false;
        }

        // download watched movies
        List<BaseMovie> watchedMovies;
        try {
            Response<List<BaseMovie>> response = traktSync
                    .watchedMovies(null)
                    .execute();
            if (response.isSuccessful()) {
                watchedMovies = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false;
                }
                SgTrakt.trackFailedRequest(context, "get watched movies", response);
                return false;
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get watched movies", e);
            return false;
        }
        if (watchedMovies == null) {
            Timber.e("downloadWatched: null response");
            return false;
        }
        if (watchedMovies.isEmpty()) {
            Timber.d("downloadWatched: no watched movies on trakt");
            return true;
        }

        // apply watched flags for all watched trakt movies that are in the local database
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        Set<Integer> localMovies = MovieTools.getMovieTmdbIdsAsSet(context);
        if (localMovies == null) {
            return false;
        }
        Set<Integer> unwatchedMovies = new HashSet<>(localMovies);
        for (BaseMovie movie : watchedMovies) {
            if (movie.movie == null || movie.movie.ids == null || movie.movie.ids.tmdb == null) {
                // required values are missing
                continue;
            }
            if (!localMovies.contains(movie.movie.ids.tmdb)) {
                // movie NOT in local database
                // add a shell entry for storing watched state
                batch.add(ContentProviderOperation.newInsert(
                        SeriesGuideContract.Movies.CONTENT_URI)
                        .withValue(SeriesGuideContract.Movies.TMDB_ID, movie.movie.ids.tmdb)
                        .withValue(SeriesGuideContract.Movies.WATCHED, true)
                        .withValue(SeriesGuideContract.Movies.IN_COLLECTION, false)
                        .withValue(SeriesGuideContract.Movies.IN_WATCHLIST, false)
                        .build());
            } else {
                // movie IN local database
                // set movie watched
                batch.add(ContentProviderOperation.newUpdate(
                        SeriesGuideContract.Movies.buildMovieUri(movie.movie.ids.tmdb))
                        .withValue(SeriesGuideContract.Movies.WATCHED, true)
                        .build());
                unwatchedMovies.remove(movie.movie.ids.tmdb);
            }
        }

        // remove watched flags from all remaining local movies
        for (Integer tmdbId : unwatchedMovies) {
            batch.add(ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Movies.buildMovieUri(tmdbId))
                    .withValue(SeriesGuideContract.Movies.WATCHED, false)
                    .build());
        }

        // apply database updates
        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "downloadWatched: updating watched flags failed");
            return false;
        }

        // save last watched instant
        long watchedAtTime = watchedAt.toInstant().toEpochMilli();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_MOVIES_WATCHED_AT, watchedAtTime)
                .apply();

        Timber.d("downloadWatched: success, last watched_at %tF %tT", watchedAtTime, watchedAtTime);
        return true;
    }

}
