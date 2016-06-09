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
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.trakt5.TraktLink;
import com.uwetrottmann.trakt5.entities.BaseEpisode;
import com.uwetrottmann.trakt5.entities.BaseMovie;
import com.uwetrottmann.trakt5.entities.BaseSeason;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.entities.LastActivityMore;
import com.uwetrottmann.trakt5.entities.RatedEpisode;
import com.uwetrottmann.trakt5.entities.RatedMovie;
import com.uwetrottmann.trakt5.entities.RatedShow;
import com.uwetrottmann.trakt5.entities.SearchResult;
import com.uwetrottmann.trakt5.entities.Show;
import com.uwetrottmann.trakt5.entities.ShowIds;
import com.uwetrottmann.trakt5.entities.SyncEpisode;
import com.uwetrottmann.trakt5.entities.SyncItems;
import com.uwetrottmann.trakt5.entities.SyncResponse;
import com.uwetrottmann.trakt5.entities.SyncSeason;
import com.uwetrottmann.trakt5.entities.SyncShow;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.IdType;
import com.uwetrottmann.trakt5.enums.RatingsFilter;
import com.uwetrottmann.trakt5.services.Sync;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.joda.time.DateTime;
import retrofit2.Response;
import timber.log.Timber;

import static com.battlelancer.seriesguide.sync.SgSyncAdapter.UpdateResult;

public class TraktTools {

    // Sync status codes
    public static final int SUCCESS = 1;
    public static final int FAILED_API = -1;
    public static final int FAILED = -2;
    public static final int FAILED_CREDENTIALS = -3;

    public enum Flag {
        COLLECTED("collected",
                SeriesGuideContract.Episodes.COLLECTED,
                // only remove flags for already collected episodes
                SeriesGuideContract.Episodes.COLLECTED + "=1",
                SeriesGuideContract.Episodes.SELECTION_COLLECTED,
                1, 0),
        WATCHED("watched",
                SeriesGuideContract.Episodes.WATCHED,
                // do not remove flags of skipped episodes, only of watched ones
                SeriesGuideContract.Episodes.WATCHED + "=" + EpisodeFlags.WATCHED,
                SeriesGuideContract.Episodes.SELECTION_WATCHED,
                EpisodeFlags.WATCHED, EpisodeFlags.UNWATCHED);

        final String name;
        final String databaseColumn;
        final String clearFlagSelection;
        final String flagSelection;
        final int flaggedValue;
        final int notFlaggedValue;

        Flag(String name, String databaseColumn, String clearFlagSelection, String flagSelection,
                int flaggedValue, int notFlaggedValue) {
            this.name = name;
            this.databaseColumn = databaseColumn;
            this.clearFlagSelection = clearFlagSelection;
            this.flagSelection = flagSelection;
            this.flaggedValue = flaggedValue;
            this.notFlaggedValue = notFlaggedValue;
        }
    }

    /**
     * Downloads trakt movie watched flags and mirrors them in the local database. Does NOT upload
     * any flags (e.g. trakt is considered the truth).
     */
    public static UpdateResult downloadWatchedMovies(Context context, DateTime watchedAt) {
        if (watchedAt == null) {
            Timber.e("downloadWatchedMovies: null watched_at");
            return UpdateResult.INCOMPLETE;
        }

        long lastWatchedAt = TraktSettings.getLastMoviesWatchedAt(context);
        if (!watchedAt.isAfter(lastWatchedAt)) {
            // not initial sync, no watched flags have changed
            Timber.d("downloadWatchedMovies: no changes since %tF %tT", lastWatchedAt,
                    lastWatchedAt);
            return UpdateResult.SUCCESS;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return UpdateResult.INCOMPLETE;
        }

        // download watched movies
        List<BaseMovie> watchedMovies;
        try {
            Response<List<BaseMovie>> response = ServiceUtils.getTrakt(context)
                    .sync()
                    .watchedMovies(Extended.DEFAULT_MIN)
                    .execute();
            if (response.isSuccessful()) {
                watchedMovies = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return UpdateResult.INCOMPLETE;
                }
                SgTrakt.trackFailedRequest(context, "get watched movies", response);
                return UpdateResult.INCOMPLETE;
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get watched movies", e);
            return UpdateResult.INCOMPLETE;
        }
        if (watchedMovies == null) {
            Timber.e("downloadWatchedMovies: null response");
            return UpdateResult.INCOMPLETE;
        }
        if (watchedMovies.isEmpty()) {
            Timber.d("downloadWatchedMovies: no watched movies on trakt");
            return UpdateResult.SUCCESS;
        }

        // apply watched flags for all watched trakt movies that are in the local database
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        Set<Integer> localMovies = MovieTools.getMovieTmdbIdsAsSet(context);
        if (localMovies == null) {
            return UpdateResult.INCOMPLETE;
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
            Timber.e(e, "downloadWatchedMovies: updating watched flags failed");
            return UpdateResult.INCOMPLETE;
        }

        // save last watched instant
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_MOVIES_WATCHED_AT, watchedAt.getMillis())
                .commit();

        Timber.d("downloadWatchedMovies: success, last watched_at %tF %tT", watchedAt.getMillis(),
                watchedAt.getMillis());
        return UpdateResult.SUCCESS;
    }

    /**
     * Downloads trakt movie ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_MOVIES_RATED_AT} to 0.
     */
    public static UpdateResult downloadMovieRatings(Context context, DateTime ratedAt) {
        if (ratedAt == null) {
            Timber.e("downloadMovieRatings: null rated_at");
            return UpdateResult.INCOMPLETE;
        }

        long lastRatedAt = TraktSettings.getLastMoviesRatedAt(context);
        if (!ratedAt.isAfter(lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadMovieRatings: no changes since %tF %tT", lastRatedAt, lastRatedAt);
            return UpdateResult.SUCCESS;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return UpdateResult.INCOMPLETE;
        }

        // download rated shows
        List<RatedMovie> ratedMovies;
        try {
            Response<List<RatedMovie>> response = ServiceUtils.getTrakt(context)
                    .sync()
                    .ratingsMovies(RatingsFilter.ALL, Extended.DEFAULT_MIN)
                    .execute();
            if (response.isSuccessful()) {
                ratedMovies = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return UpdateResult.INCOMPLETE;
                }
                SgTrakt.trackFailedRequest(context, "get movie ratings", response);
                return UpdateResult.INCOMPLETE;
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get movie ratings", e);
            return UpdateResult.INCOMPLETE;
        }
        if (ratedMovies == null) {
            Timber.e("downloadMovieRatings: null response");
            return UpdateResult.INCOMPLETE;
        }
        if (ratedMovies.isEmpty()) {
            Timber.d("downloadMovieRatings: no ratings on trakt");
            return UpdateResult.SUCCESS;
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        long ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS;

        // go through ratings, latest first (trakt sends in that order)
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (RatedMovie movie : ratedMovies) {
            if (movie.rating == null || movie.movie == null || movie.movie.ids == null
                    || movie.movie.ids.tmdb == null) {
                // skip, can't handle
                continue;
            }
            if (movie.rated_at != null && movie.rated_at.isBefore(ratedAtThreshold)) {
                // no need to apply older ratings again
                break;
            }

            // if a movie does not exist, this update will do nothing
            ContentProviderOperation op = ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Movies.buildMovieUri(movie.movie.ids.tmdb))
                    .withValue(SeriesGuideContract.Movies.RATING_USER, movie.rating.value)
                    .build();
            batch.add(op);
        }

        // apply database updates
        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "downloadMovieRatings: database update failed");
            return UpdateResult.INCOMPLETE;
        }

        // save last rated instant
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_MOVIES_RATED_AT, ratedAt.getMillis())
                .commit();

        Timber.d("downloadMovieRatings: success, last rated_at %tF %tT", ratedAt.getMillis(),
                ratedAt.getMillis());
        return UpdateResult.SUCCESS;
    }

    /**
     * Downloads trakt show ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_SHOWS_RATED_AT} to 0.
     */
    public static UpdateResult downloadShowRatings(Context context, @Nullable DateTime ratedAt) {
        if (ratedAt == null) {
            Timber.e("downloadShowRatings: null rated_at");
            return UpdateResult.INCOMPLETE;
        }

        long lastRatedAt = TraktSettings.getLastShowsRatedAt(context);
        if (!ratedAt.isAfter(lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadShowRatings: no changes since %tF %tT", lastRatedAt, lastRatedAt);
            return UpdateResult.SUCCESS;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return UpdateResult.INCOMPLETE;
        }

        // download rated shows
        List<RatedShow> ratedShows;
        try {
            Response<List<RatedShow>> response = ServiceUtils.getTrakt(context)
                    .sync()
                    .ratingsShows(RatingsFilter.ALL, Extended.DEFAULT_MIN)
                    .execute();
            if (response.isSuccessful()) {
                ratedShows = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return UpdateResult.INCOMPLETE;
                }
                SgTrakt.trackFailedRequest(context, "get show ratings", response);
                return UpdateResult.INCOMPLETE;
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get show ratings", e);
            return UpdateResult.INCOMPLETE;
        }
        if (ratedShows == null) {
            Timber.e("downloadShowRatings: null response");
            return UpdateResult.INCOMPLETE;
        }
        if (ratedShows.isEmpty()) {
            Timber.d("downloadShowRatings: no ratings on trakt");
            return UpdateResult.SUCCESS;
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        long ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS;

        // go through ratings, latest first (trakt sends in that order)
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (RatedShow show : ratedShows) {
            if (show.rating == null || show.show == null || show.show.ids == null
                    || show.show.ids.tvdb == null) {
                // skip, can't handle
                continue;
            }
            if (show.rated_at != null && show.rated_at.isBefore(ratedAtThreshold)) {
                // no need to apply older ratings again
                break;
            }

            // if a show does not exist, this update will do nothing
            ContentProviderOperation op = ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Shows.buildShowUri(show.show.ids.tvdb))
                    .withValue(SeriesGuideContract.Shows.RATING_USER, show.rating.value)
                    .build();
            batch.add(op);
        }

        // apply database updates
        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "downloadShowRatings: database update failed");
            return UpdateResult.INCOMPLETE;
        }

        // save last rated instant
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, ratedAt.getMillis())
                .commit();

        Timber.d("downloadShowRatings: success, last rated_at %tF %tT", ratedAt.getMillis(),
                ratedAt.getMillis());
        return UpdateResult.SUCCESS;
    }

    /**
     * Downloads trakt episode ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_EPISODES_RATED_AT} to 0.
     */
    public static UpdateResult downloadEpisodeRatings(Context context, @Nullable DateTime ratedAt) {
        if (ratedAt == null) {
            Timber.e("downloadEpisodeRatings: null rated_at");
            return UpdateResult.INCOMPLETE;
        }

        long lastRatedAt = TraktSettings.getLastEpisodesRatedAt(context);
        if (!ratedAt.isAfter(lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadEpisodeRatings: no changes since %tF %tT", lastRatedAt, lastRatedAt);
            return UpdateResult.SUCCESS;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return UpdateResult.INCOMPLETE;
        }

        // download rated episodes
        List<RatedEpisode> ratedEpisodes;
        try {
            Response<List<RatedEpisode>> response
                    = ServiceUtils.getTrakt(context)
                    .sync()
                    .ratingsEpisodes(RatingsFilter.ALL, Extended.DEFAULT_MIN)
                    .execute();
            if (response.isSuccessful()) {
                ratedEpisodes = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return UpdateResult.INCOMPLETE;
                }
                SgTrakt.trackFailedRequest(context, "get episode ratings", response);
                return UpdateResult.INCOMPLETE;
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get episode ratings", e);
            return UpdateResult.INCOMPLETE;
        }
        if (ratedEpisodes == null) {
            Timber.e("downloadEpisodeRatings: null response");
            return UpdateResult.INCOMPLETE;
        }
        if (ratedEpisodes.isEmpty()) {
            Timber.d("downloadEpisodeRatings: no ratings on trakt");
            return UpdateResult.SUCCESS;
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        long ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS;

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (RatedEpisode episode : ratedEpisodes) {
            if (episode.rating == null || episode.episode == null || episode.episode.ids == null
                    || episode.episode.ids.tvdb == null) {
                // skip, can't handle
                continue;
            }
            if (episode.rated_at != null && episode.rated_at.isBefore(ratedAtThreshold)) {
                // no need to apply older ratings again
                break;
            }

            // if an episode does not exist, this update will do nothing
            ContentProviderOperation op = ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Episodes.buildEpisodeUri(episode.episode.ids.tvdb))
                    .withValue(SeriesGuideContract.Episodes.RATING_USER, episode.rating.value)
                    .build();
            batch.add(op);
        }

        // apply database updates
        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "downloadEpisodeRatings: database update failed");
            return UpdateResult.INCOMPLETE;
        }

        // save last rated instant
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, ratedAt.getMillis())
                .commit();

        Timber.d("downloadEpisodeRatings: success, last rated_at %tF %tT", ratedAt.getMillis(),
                ratedAt.getMillis());
        return UpdateResult.SUCCESS;
    }

    /**
     * Downloads, uploads and sets watched and collected flags for episodes if they have changed on
     * trakt (or {@code isInitialSync} is true).
     *
     * @param isInitialSync If set, will upload any episodes flagged locally, but not flagged on
     * trakt. If not set, all watched and collected (and only those, e.g. not skipped flag) flags
     * will be removed prior to getting the actual flags from trakt (season by season).
     * @return Any of the {@link TraktTools} result codes.
     */
    public static int syncEpisodeFlags(Context context, @NonNull HashSet<Integer> localShows,
            @NonNull LastActivityMore activity, boolean isInitialSync) {
        if (!TraktCredentials.get(context).hasCredentials()) {
            return FAILED_CREDENTIALS;
        }

        // watched episodes
        Sync traktSync = ServiceUtils.getTrakt(context).sync();
        int result = syncWatchedEpisodes(context, traktSync, localShows, activity.watched_at,
                isInitialSync);
        if (result < SUCCESS) {
            return result; // failed to process watched episodes, give up.
        }

        // collected episodes
        result = syncCollectedEpisodes(context, traktSync, localShows, activity.collected_at,
                isInitialSync);
        return result;
    }

    private static int syncWatchedEpisodes(Context context, Sync traktSync,
            @NonNull HashSet<Integer> localShows, @Nullable DateTime watchedAt,
            boolean isInitialSync) {
        if (watchedAt == null) {
            Timber.e("syncWatchedEpisodes: null watched_at");
            return FAILED;
        }

        long lastWatchedAt = TraktSettings.getLastEpisodesWatchedAt(context);
        if (isInitialSync || watchedAt.isAfter(lastWatchedAt)) {
            List<BaseShow> watchedShowsTrakt = null;
            try {
                // get watched episodes from trakt
                Response<List<BaseShow>> response
                        = traktSync.watchedShows(com.uwetrottmann.trakt5.enums.Extended.DEFAULT_MIN)
                        .execute();
                if (response.isSuccessful()) {
                    watchedShowsTrakt = response.body();
                } else {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return FAILED_CREDENTIALS;
                    }
                    SgTrakt.trackFailedRequest(context, "get watched shows", response);
                }
            } catch (IOException e) {
                SgTrakt.trackFailedRequest(context, "get watched shows", e);
            }

            if (watchedShowsTrakt == null) {
                return FAILED_API;
            }

            // apply database updates, if initial sync upload diff
            long startTime = System.currentTimeMillis();
            int result = processTraktShows(context, traktSync, watchedShowsTrakt, localShows,
                    isInitialSync, Flag.WATCHED);
            Timber.d("syncWatchedEpisodes: processing took %s ms",
                    System.currentTimeMillis() - startTime);
            if (result < SUCCESS) {
                return result; // failed to process watched episodes, give up.
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_EPISODES_WATCHED_AT, watchedAt.getMillis())
                    .apply();

            Timber.d("syncWatchedEpisodes: success");
        } else {
            Timber.d("syncWatchedEpisodes: no changes since %tF %tT", lastWatchedAt,
                    lastWatchedAt);
        }
        return SUCCESS;
    }

    private static int syncCollectedEpisodes(Context context, Sync traktSync,
            @NonNull HashSet<Integer> localShows, @Nullable DateTime collectedAt,
            boolean isInitialSync) {
        if (collectedAt == null) {
            Timber.e("syncCollectedEpisodes: null collected_at");
            return FAILED;
        }

        long lastCollectedAt = TraktSettings.getLastEpisodesCollectedAt(context);
        if (isInitialSync || collectedAt.isAfter(lastCollectedAt)) {
            List<BaseShow> collectedShowsTrakt = null;
            try {
                // get collected episodes from trakt
                Response<List<BaseShow>> response = traktSync.collectionShows(
                        com.uwetrottmann.trakt5.enums.Extended.DEFAULT_MIN).execute();
                if (response.isSuccessful()) {
                    collectedShowsTrakt = response.body();
                } else {
                    if (SgTrakt.isUnauthorized(context, response)) {
                        return FAILED_CREDENTIALS;
                    }
                    SgTrakt.trackFailedRequest(context, "get collected shows", response);
                }
            } catch (IOException e) {
                SgTrakt.trackFailedRequest(context, "get collected shows", e);
            }

            if (collectedShowsTrakt == null) {
                return FAILED_API;
            }

            // apply database updates,  if initial sync upload diff
            long startTime = System.currentTimeMillis();
            int result = processTraktShows(context, traktSync, collectedShowsTrakt, localShows,
                    isInitialSync, Flag.COLLECTED);
            Timber.d("syncCollectedEpisodes: processing took %s ms",
                    System.currentTimeMillis() - startTime);
            if (result < SUCCESS) {
                return result; // failed to process collected episodes, give up.
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_EPISODES_COLLECTED_AT, collectedAt.getMillis())
                    .apply();

            Timber.d("syncCollectedEpisodes: success");
        } else {
            Timber.d("syncCollectedEpisodes: no changes since %tF %tT", lastCollectedAt,
                    lastCollectedAt);
        }
        return SUCCESS;
    }

    /**
     * Similar to {@link #syncEpisodeFlags(Context, HashSet, LastActivityMore, boolean)}, but only
     * processes a single show and only downloads watched/collected episodes from trakt.
     */
    public static boolean storeEpisodeFlags(Context context,
            @Nullable HashMap<Integer, BaseShow> traktShows, int showTvdbId, @NonNull Flag flag) {
        if (traktShows == null || traktShows.isEmpty()) {
            return true; // no watched/collected shows on trakt, done.
        }
        if (!traktShows.containsKey(showTvdbId)) {
            return true; // show is not watched/collected on trakt, done.
        }
        BaseShow traktShow = traktShows.get(showTvdbId);
        return TraktTools.processTraktSeasons(context, null, false, showTvdbId, traktShow, flag)
                == SUCCESS;
    }

    private static int processTraktShows(Context context,
            com.uwetrottmann.trakt5.services.Sync traktSync,
            @NonNull List<BaseShow> remoteShows,
            @NonNull HashSet<Integer> localShows, boolean isInitialSync, Flag flag) {
        HashMap<Integer, BaseShow> traktShow = buildTraktShowsMap(remoteShows);

        int uploadedShowsCount = 0;
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (Integer localShow : localShows) {
            if (traktShow.containsKey(localShow)) {
                // show watched/collected on trakt
                int result = processTraktSeasons(context, traktSync, isInitialSync,
                        localShow, traktShow.get(localShow), flag);
                if (result < SUCCESS) {
                    return result; // processing seasons failed, give up.
                }
            } else {
                // show not watched/collected on trakt
                // check if this is because the show can not be tracked with trakt (yet)
                // some shows only exist on TheTVDB, keep state local and maybe upload in the future
                Integer showTraktId = ShowTools.getShowTraktId(context, localShow);
                if (showTraktId != null) {
                    if (isInitialSync) {
                        // upload all watched/collected episodes of the show
                        // do in between processing to stretch uploads over longer time periods
                        uploadEpisodes(context, traktSync, localShow, showTraktId, flag);
                        uploadedShowsCount++;
                    } else {
                        // set all watched/collected episodes of show not watched/collected
                        batch.add(ContentProviderOperation.newUpdate(
                                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(localShow))
                                .withSelection(flag.clearFlagSelection, null)
                                .withValue(flag.databaseColumn, flag.notFlaggedValue)
                                .build());
                    }
                }
            }
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "processTraktShows: failed to remove flag for %s.", flag.name);
        }

        if (uploadedShowsCount > 0) {
            Timber.d("processTraktShows: uploaded %s flags for %s complete shows.", flag.name,
                    localShows.size());
        }
        return SUCCESS;
    }

    /**
     * Sync the watched/collected episodes of the given trakt show with the local episodes. The
     * given show has to be watched/collected on trakt.
     *
     * @param isInitialSync If {@code true}, will upload watched/collected episodes that are not
     * watched/collected on trakt. If {@code false}, will set them not watched/collected (if not
     * skipped) to mirror the trakt episode.
     * @param traktSync May be {@code null} if {@code isInitialSync} is set to false as no uploading
     * may happen.
     */
    public static int processTraktSeasons(Context context, @Nullable Sync traktSync,
            boolean isInitialSync, int localShow, @NonNull BaseShow traktShow, @NonNull Flag flag) {
        HashMap<Integer, BaseSeason> traktSeasons = buildTraktSeasonsMap(traktShow.seasons);

        Cursor localSeasonsQuery = context.getContentResolver()
                .query(SeriesGuideContract.Seasons.buildSeasonsOfShowUri(localShow),
                        new String[] { SeriesGuideContract.Seasons._ID,
                                SeriesGuideContract.Seasons.COMBINED }, null, null,
                        null);
        if (localSeasonsQuery == null) {
            return FAILED;
        }
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        List<SyncSeason> syncSeasons = new ArrayList<>();
        while (localSeasonsQuery.moveToNext()) {
            String seasonId = localSeasonsQuery.getString(0);
            int seasonNumber = localSeasonsQuery.getInt(1);
            if (traktSeasons.containsKey(seasonNumber)) {
                // season watched/collected on trakt
                if (!processTraktEpisodes(context, isInitialSync, seasonId,
                        traktSeasons.get(seasonNumber), syncSeasons, flag)) {
                    return FAILED;
                }
            } else {
                // season not watched/collected on trakt
                if (isInitialSync) {
                    // schedule all watched/collected episodes of this season for upload
                    SyncSeason syncSeason = buildSyncSeason(context, seasonId, seasonNumber, flag);
                    if (syncSeason != null) {
                        syncSeasons.add(syncSeason);
                    }
                } else {
                    // set all watched/collected episodes of season not watched/collected
                    batch.add(ContentProviderOperation.newUpdate(
                            SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(seasonId))
                            .withSelection(flag.clearFlagSelection, null)
                            .withValue(flag.databaseColumn, flag.notFlaggedValue)
                            .build());
                }
            }
        }
        localSeasonsQuery.close();

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Setting seasons unwatched failed.");
        }

        if (syncSeasons.size() > 0) {
            // upload watched/collected episodes for this show
            Integer showTraktId = ShowTools.getShowTraktId(context, localShow);
            if (showTraktId == null) {
                return FAILED; // show should have a trakt id, give up
            }
            return uploadEpisodes(context, traktSync, showTraktId, syncSeasons, flag);
        } else {
            return SUCCESS;
        }
    }

    private static boolean processTraktEpisodes(Context context, boolean isInitialSync,
            String seasonId, BaseSeason traktSeason, List<SyncSeason> syncSeasons, Flag flag) {
        HashSet<Integer> traktEpisodes = buildTraktEpisodesMap(traktSeason.episodes);

        Cursor localEpisodesQuery = context.getContentResolver()
                .query(SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(
                        seasonId), new String[] {
                        SeriesGuideContract.Episodes._ID,
                        SeriesGuideContract.Episodes.NUMBER,
                        flag.databaseColumn }, null, null, null);
        if (localEpisodesQuery == null) {
            return false;
        }
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        int episodesAddFlagCount = 0;
        int episodesRemoveFlagCount = 0;
        while (localEpisodesQuery.moveToNext()) {
            int episodeId = localEpisodesQuery.getInt(0);
            int episodeNumber = localEpisodesQuery.getInt(1);
            int flagValue = localEpisodesQuery.getInt(2);
            boolean isFlagged = flag == Flag.WATCHED ?
                    EpisodeTools.isWatched(flagValue) : EpisodeTools.isCollected(flagValue);
            if (traktEpisodes.contains(episodeNumber)) {
                // episode watched/collected on trakt
                if (!isFlagged) {
                    // set as watched/collected
                    batch.add(ContentProviderOperation.newUpdate(
                            SeriesGuideContract.Episodes.buildEpisodeUri(episodeId))
                            .withValue(flag.databaseColumn, flag.flaggedValue)
                            .build());
                    episodesAddFlagCount++;
                }
            } else {
                // episode not watched/collected on trakt
                if (isInitialSync) {
                    // upload to trakt
                    syncEpisodes.add(new SyncEpisode().number(episodeNumber));
                } else {
                    // set as not watched/collected if it is currently watched/collected
                    boolean isSkipped = flag == Flag.WATCHED && EpisodeTools.isSkipped(flagValue);
                    if (!isSkipped) {
                        batch.add(ContentProviderOperation.newUpdate(
                                SeriesGuideContract.Episodes.buildEpisodeUri(episodeId))
                                .withValue(flag.databaseColumn, flag.notFlaggedValue)
                                .build());
                        episodesRemoveFlagCount++;
                    }
                }
            }
        }
        int localEpisodeCount = localEpisodesQuery.getCount();
        boolean addFlagToWholeSeason = episodesAddFlagCount == localEpisodeCount;
        boolean removeFlagFromWholeSeason = episodesRemoveFlagCount == localEpisodeCount;
        localEpisodesQuery.close();

        // performance improvement especially on initial syncs:
        // if setting the whole season as (not) watched/collected, replace with single db op
        if (addFlagToWholeSeason || removeFlagFromWholeSeason) {
            batch.clear();
            batch.add(ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(seasonId))
                    .withValue(flag.databaseColumn,
                            addFlagToWholeSeason ? flag.flaggedValue : flag.notFlaggedValue)
                    .build());
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Episodes watched/collected values database update failed.");
        }

        if (syncEpisodes.size() > 0) {
            syncSeasons.add(new SyncSeason()
                    .number(traktSeason.number)
                    .episodes(syncEpisodes));
        }

        return true;
    }

    @NonNull
    public static HashMap<Integer, BaseShow> buildTraktShowsMap(List<BaseShow> traktShows) {
        HashMap<Integer, BaseShow> traktShowsMap = new HashMap<>(traktShows.size());
        for (BaseShow traktShow : traktShows) {
            if (isTraktShowMissingRequiredValues(traktShow)) {
                continue; // trakt show misses required data, skip.
            }
            traktShowsMap.put(traktShow.show.ids.tvdb, traktShow);
        }
        return traktShowsMap;
    }

    public static boolean isTraktShowMissingRequiredValues(BaseShow traktShow) {
        return traktShow.show == null
                || traktShow.show.ids == null
                || traktShow.show.ids.tvdb == null
                || traktShow.seasons == null
                || traktShow.seasons.isEmpty();
    }

    @NonNull
    private static HashMap<Integer, BaseSeason> buildTraktSeasonsMap(List<BaseSeason> seasons) {
        HashMap<Integer, BaseSeason> traktSeasonsMap = new HashMap<>(seasons.size());
        for (BaseSeason season : seasons) {
            if (season.number == null
                    || season.episodes == null
                    || season.episodes.isEmpty()) {
                continue; // trakt season misses required data, skip.
            }
            traktSeasonsMap.put(season.number, season);
        }
        return traktSeasonsMap;
    }

    @NonNull
    private static HashSet<Integer> buildTraktEpisodesMap(List<BaseEpisode> episodes) {
        HashSet<Integer> traktEpisodesMap = new HashSet<>(episodes.size());
        for (BaseEpisode episode : episodes) {
            if (episode.number == null) {
                continue; // trakt episode misses required data, skip.
            }
            traktEpisodesMap.add(episode.number);
        }
        return traktEpisodesMap;
    }

    /**
     * Uploads all watched/collected episodes for the given show to trakt.
     *
     * @return Any of the {@link TraktTools} result codes.
     */
    private static int uploadEpisodes(Context context, Sync traktSync, int showTvdbId,
            int showTraktId, Flag flag) {
        // query for watched/collected episodes
        Cursor localEpisodes = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                EpisodesQuery.PROJECTION,
                flag.flagSelection,
                null,
                SeriesGuideContract.Episodes.SORT_SEASON_ASC);
        if (localEpisodes == null) {
            Timber.e("uploadEpisodes: query failed");
            return FAILED;
        }

        // build a list of watched/collected episodes
        List<SyncSeason> syncSeasons = new LinkedList<>();
        buildEpisodeList(localEpisodes, syncSeasons);
        localEpisodes.close();

        if (syncSeasons.size() == 0) {
            return SUCCESS; // nothing to upload for this show
        }

        return uploadEpisodes(context, traktSync, showTraktId, syncSeasons, flag);
    }

    /**
     * Uploads all the given watched/collected episodes of the given show to trakt.
     *
     * @return Any of the {@link TraktTools} result codes.
     */
    private static int uploadEpisodes(Context context, Sync traktSync, int showTraktId,
            List<SyncSeason> syncSeasons, Flag flag) {
        SyncShow syncShow = new SyncShow();
        syncShow.id(ShowIds.trakt(showTraktId));
        syncShow.seasons = syncSeasons;

        // upload
        SyncItems syncItems = new SyncItems().shows(syncShow);
        try {
            Response<SyncResponse> response;
            if (flag == Flag.WATCHED) {
                // uploading watched episodes
                response = traktSync.addItemsToWatchedHistory(syncItems).execute();
            } else {
                // uploading collected episodes
                response = traktSync.addItemsToCollection(syncItems).execute();
            }
            if (response.isSuccessful()) {
                return SUCCESS;
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return FAILED_CREDENTIALS;
                }
                SgTrakt.trackFailedRequest(context, "add episodes to " + flag.name, response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "add episodes to " + flag.name, e);
        }

        return FAILED_API;
    }

    /**
     * @param episodesCursor Cursor of episodes sorted by season (ascending).
     * @param seasons Empty list.
     */
    private static void buildEpisodeList(Cursor episodesCursor, List<SyncSeason> seasons) {
        SyncSeason currentSeason = null;
        while (episodesCursor.moveToNext()) {
            int season = episodesCursor.getInt(EpisodesQuery.SEASON);
            int episode = episodesCursor.getInt(EpisodesQuery.EPISODE);

            // create new season if none exists or number has changed
            if (currentSeason == null || currentSeason.number != season) {
                currentSeason = new SyncSeason().number(season);
                currentSeason.episodes = new LinkedList<>();
                seasons.add(currentSeason);
            }

            // add episode
            currentSeason.episodes.add(new SyncEpisode().number(episode));
        }
    }

    /**
     * Returns a list of watched/collected episodes of a season. Packaged ready for upload to trakt
     * in a {@link com.uwetrottmann.trakt.v2.entities.SyncSeason}.
     */
    private static SyncSeason buildSyncSeason(Context context, String seasonTvdbId,
            int seasonNumber, Flag flag) {
        // query for watched/collected episodes of the given season
        Cursor flaggedEpisodesQuery = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfSeasonUri(seasonTvdbId),
                new String[] { SeriesGuideContract.Episodes.NUMBER },
                flag.flagSelection,
                null,
                SeriesGuideContract.Episodes.SORT_NUMBER_ASC);
        if (flaggedEpisodesQuery == null) {
            // query failed
            return null;
        }

        List<SyncEpisode> syncEpisodes = new ArrayList<>();
        while (flaggedEpisodesQuery.moveToNext()) {
            int episodeNumber = flaggedEpisodesQuery.getInt(0);
            syncEpisodes.add(new SyncEpisode().number(episodeNumber));
        }
        flaggedEpisodesQuery.close();

        if (syncEpisodes.size() == 0) {
            return null; // no episodes watched/collected
        }

        return new SyncSeason().number(seasonNumber).episodes(syncEpisodes);
    }

    public static String buildShowUrl(int showTvdbId) {
        return TraktLink.tvdb(showTvdbId) + "?id_type=show";
    }

    public static String buildEpisodeUrl(int episodeTvdbId) {
        return TraktLink.tvdb(episodeTvdbId) + "?id_type=episode";
    }

    public static String buildMovieUrl(int movieTmdbId) {
        return TraktLink.tmdb(movieTmdbId) + "?id_type=movie";
    }

    /**
     * Returns the given double as number string with one decimal digit, like "1.5".
     */
    public static String buildRatingString(Double rating) {
        return rating == null || rating == 0 ? "--"
                : String.format(Locale.getDefault(), "%.1f", rating);
    }

    /**
     * Builds a localized string like "x votes".
     */
    public static String buildRatingVotesString(Context context, Integer votes) {
        if (votes == null || votes < 0) {
            votes = 0;
        }
        return context.getResources().getQuantityString(R.plurals.votes, votes, votes);
    }

    /**
     * Converts a rating index from 1 to 10 into a localized string representation. Any other value
     * will return the local variant of "n/a".
     */
    public static String buildUserRatingString(Context context, int rating) {
        int resId;
        switch (rating) {
            case 1:
                resId = R.string.hate;
                break;
            case 2:
                resId = R.string.rating2;
                break;
            case 3:
                resId = R.string.rating3;
                break;
            case 4:
                resId = R.string.rating4;
                break;
            case 5:
                resId = R.string.rating5;
                break;
            case 6:
                resId = R.string.rating6;
                break;
            case 7:
                resId = R.string.rating7;
                break;
            case 8:
                resId = R.string.rating8;
                break;
            case 9:
                resId = R.string.rating9;
                break;
            case 10:
                resId = R.string.love;
                break;
            default:
                resId = R.string.action_rate;
                break;
        }

        return context.getString(resId);
    }

    /**
     * Look up a show's trakt id, may return {@code null} if not found.
     *
     * <p> <b>Always</b> supply trakt services <b>without</b> auth, as retrofit will crash on auth
     * errors.
     */
    public static String lookupShowTraktId(Context context, int showTvdbId) {
        List<SearchResult> searchResults = null;
        try {
            // get up to 3 results: may be a show, season or episode (TVDb ids are not unique)
            Response<List<SearchResult>> response = ServiceUtils.getTrakt(context)
                    .search()
                    .idLookup(IdType.TVDB, String.valueOf(showTvdbId), 1, 3)
                    .execute();
            if (response.isSuccessful()) {
                searchResults = response.body();
            } else {
                SgTrakt.trackFailedRequest(context, "show trakt id lookup", response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "show trakt id lookup", e);
        }
        if (searchResults == null) {
            return null;
        }

        for (SearchResult result : searchResults) {
            if (result.episode != null) {
                // not a show result
                continue;
            }
            Show show = result.show;
            if (show != null && show.ids != null && show.ids.trakt != null) {
                return String.valueOf(show.ids.trakt);
            }
        }

        return null;
    }

    @Nullable
    public static Show getShowSummary(Context context, String showTraktId) {
        try {
            // fetch details
            retrofit2.Response<com.uwetrottmann.trakt5.entities.Show> response
                    = ServiceUtils.getTrakt(context)
                    .shows()
                    .summary(showTraktId, Extended.FULL)
                    .execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                SgTrakt.trackFailedRequest(context, "get show summary", response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, "get show summary", e);
        }
        return null;
    }

    public interface EpisodesQuery {

        String[] PROJECTION = new String[] {
                SeriesGuideContract.Episodes.SEASON, SeriesGuideContract.Episodes.NUMBER
        };

        int SEASON = 0;
        int EPISODE = 1;
    }
}
