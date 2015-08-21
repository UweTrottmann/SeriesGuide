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
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.uwetrottmann.trakt.v2.TraktLink;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseEpisode;
import com.uwetrottmann.trakt.v2.entities.BaseMovie;
import com.uwetrottmann.trakt.v2.entities.BaseSeason;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.LastActivity;
import com.uwetrottmann.trakt.v2.entities.LastActivityMore;
import com.uwetrottmann.trakt.v2.entities.RatedEpisode;
import com.uwetrottmann.trakt.v2.entities.RatedMovie;
import com.uwetrottmann.trakt.v2.entities.RatedShow;
import com.uwetrottmann.trakt.v2.entities.SearchResult;
import com.uwetrottmann.trakt.v2.entities.Show;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.IdType;
import com.uwetrottmann.trakt.v2.enums.RatingsFilter;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Search;
import com.uwetrottmann.trakt.v2.services.Sync;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.joda.time.DateTime;
import retrofit.RetrofitError;
import timber.log.Timber;

import static com.battlelancer.seriesguide.sync.SgSyncAdapter.UpdateResult;

public class TraktTools {

    // Sync status codes
    public static final int SUCCESS = 1;
    public static final int SUCCESS_NOWORK = 0;
    public static final int FAILED_API = -1;
    public static final int FAILED = -2;
    public static final int FAILED_CREDENTIALS = -3;

    // Url parts
    private static final String TRAKT_SEARCH_BASE_URL = "https://trakt.tv/search/";
    private static final String TRAKT_SEARCH_SHOW_URL = TRAKT_SEARCH_BASE_URL + "tvdb?q=";
    private static final String TRAKT_SEARCH_MOVIE_URL = TRAKT_SEARCH_BASE_URL + "tmdb?q=";
    private static final String TRAKT_SEARCH_SEASON_ARG = "&s=";
    private static final String TRAKT_SEARCH_EPISODE_ARG = "&e=";

    public enum Flag {
        COLLECTED(SeriesGuideContract.Episodes.COLLECTED,
                // only remove flags for already collected episodes
                SeriesGuideContract.Episodes.COLLECTED + "=1",
                1, 0),
        WATCHED(SeriesGuideContract.Episodes.WATCHED,
                // do not remove flags of skipped episodes, only of watched ones
                SeriesGuideContract.Episodes.WATCHED + "=" + EpisodeFlags.WATCHED,
                EpisodeFlags.WATCHED, EpisodeFlags.UNWATCHED);

        final String databaseColumn;
        final String clearFlagSelection;
        final int flaggedValue;
        final int nonFlaggedValue;

        private Flag(String databaseColumn, String clearFlagSelection, int flaggedValue,
                int nonFlaggedValue) {
            this.databaseColumn = databaseColumn;
            this.clearFlagSelection = clearFlagSelection;
            this.flaggedValue = flaggedValue;
            this.nonFlaggedValue = nonFlaggedValue;
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
            Timber.d("downloadWatchedMovies: no changes since " + lastWatchedAt);
            return UpdateResult.SUCCESS;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return UpdateResult.INCOMPLETE;
        }

        // download watched movies
        List<BaseMovie> watchedMovies;
        try {
            watchedMovies = trakt.sync().watchedMovies(Extended.DEFAULT_MIN);
        } catch (RetrofitError e) {
            Timber.e(e, "downloadWatchedMovies: download failed");
            return UpdateResult.INCOMPLETE;
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(context).setCredentialsInvalid();
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

        Timber.d("downloadWatchedMovies: success, last watched_at " + watchedAt.getMillis());
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
            Timber.d("downloadMovieRatings: no changes since " + lastRatedAt);
            return UpdateResult.SUCCESS;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return UpdateResult.INCOMPLETE;
        }

        // download rated shows
        List<RatedMovie> ratedMovies;
        try {
            ratedMovies = trakt.sync().ratingsMovies(RatingsFilter.ALL, Extended.DEFAULT_MIN);
        } catch (RetrofitError e) {
            Timber.e(e, "downloadMovieRatings: download failed");
            return UpdateResult.INCOMPLETE;
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(context).setCredentialsInvalid();
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

        Timber.d("downloadMovieRatings: success, last rated_at " + ratedAt.getMillis());
        return UpdateResult.SUCCESS;
    }

    /**
     * Downloads trakt show ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_SHOWS_RATED_AT} to 0.
     */
    public static UpdateResult downloadShowRatings(Context context, LastActivity activity) {
        if (activity.rated_at == null) {
            Timber.e("downloadShowRatings: null rated_at");
            return UpdateResult.INCOMPLETE;
        }

        long lastRatedAt = TraktSettings.getLastShowsRatedAt(context);
        if (!activity.rated_at.isAfter(lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadShowRatings: no changes since " + lastRatedAt);
            return UpdateResult.SUCCESS;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return UpdateResult.INCOMPLETE;
        }

        // download rated shows
        List<RatedShow> ratedShows;
        try {
            ratedShows = trakt.sync().ratingsShows(RatingsFilter.ALL, Extended.DEFAULT_MIN);
        } catch (RetrofitError e) {
            Timber.e(e, "downloadShowRatings: download failed");
            return UpdateResult.INCOMPLETE;
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(context).setCredentialsInvalid();
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
                .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, activity.rated_at.getMillis())
                .commit();

        Timber.d("downloadShowRatings: success, last rated_at " + activity.rated_at.getMillis());
        return UpdateResult.SUCCESS;
    }

    /**
     * Downloads trakt episode ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_EPISODES_RATED_AT} to 0.
     */
    public static UpdateResult downloadEpisodeRatings(Context context, LastActivityMore activity) {
        if (activity.rated_at == null) {
            Timber.e("downloadEpisodeRatings: null rated_at");
            return UpdateResult.INCOMPLETE;
        }

        long lastRatedAt = TraktSettings.getLastEpisodesRatedAt(context);
        if (!activity.rated_at.isAfter(lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadEpisodeRatings: no changes since " + lastRatedAt);
            return UpdateResult.SUCCESS;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return UpdateResult.INCOMPLETE;
        }

        // download rated episodes
        List<RatedEpisode> ratedEpisodes;
        try {
            ratedEpisodes = trakt.sync().ratingsEpisodes(RatingsFilter.ALL, Extended.DEFAULT_MIN);
        } catch (RetrofitError e) {
            Timber.e(e, "downloadEpisodeRatings: download failed");
            return UpdateResult.INCOMPLETE;
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(context).setCredentialsInvalid();
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
                .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, activity.rated_at.getMillis())
                .commit();

        Timber.d("downloadEpisodeRatings: success, last rated_at " + activity.rated_at.getMillis());
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
    public static int syncEpisodeFlags(Context context, HashSet<Integer> localShows,
            LastActivityMore activity, boolean isInitialSync) {
        if (activity.collected_at == null) {
            Timber.e("downloadEpisodeFlags: null collected_at");
            return FAILED;
        }
        if (activity.watched_at == null) {
            Timber.e("downloadEpisodeFlags: null watched_at");
            return FAILED;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return FAILED_CREDENTIALS;
        }
        Sync traktSync = trakt.sync();

        // watched episodes
        if (isInitialSync || activity.watched_at.isAfter(
                TraktSettings.getLastEpisodesWatchedAt(context))) {
            try {
                // get watched episodes from trakt
                List<BaseShow> remoteShows = traktSync.watchedShows(Extended.DEFAULT_MIN);
                if (remoteShows == null) {
                    Timber.e("downloadEpisodeFlags: null watched response");
                    return FAILED_API;
                }

                // apply database updates, if initial sync upload diff
                int resultCode = applyEpisodeFlagChanges(context, traktSync, remoteShows,
                        localShows, Flag.WATCHED, isInitialSync);
                if (resultCode < 0) {
                    // upload failed, abort
                    return resultCode;
                }
            } catch (RetrofitError e) {
                Timber.e(e, "downloadEpisodeFlags: watched download failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_EPISODES_WATCHED_AT,
                            activity.watched_at.getMillis())
                    .apply();

            Timber.d("downloadEpisodeFlags: success for watched");
        }

        // collected episodes
        if (isInitialSync || activity.collected_at.isAfter(
                TraktSettings.getLastEpisodesCollectedAt(context))) {
            try {
                // get collected episodes from trakt
                List<BaseShow> remoteShows = traktSync.collectionShows(Extended.DEFAULT_MIN);
                if (remoteShows == null) {
                    Timber.e("downloadEpisodeFlags: null collected response");
                    return FAILED_API;
                }

                // apply database updates,  if initial sync upload diff
                int resultCode = applyEpisodeFlagChanges(context, traktSync, remoteShows,
                        localShows, Flag.COLLECTED, isInitialSync);
                if (resultCode < 0) {
                    // upload failed, abort
                    return resultCode;
                }
            } catch (RetrofitError e) {
                Timber.e(e, "downloadEpisodeFlags: collected download failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }

            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_EPISODES_WATCHED_AT,
                            activity.collected_at.getMillis())
                    .apply();

            Timber.d("downloadEpisodeFlags: success for collected");
        }

        return SUCCESS;
    }

    private static int applyEpisodeFlagChanges(Context context, Sync traktSync,
            List<BaseShow> traktShows, HashSet<Integer> localShows, Flag flag, boolean isMerging)
            throws OAuthUnauthorizedException {
        HashSet<Integer> localShowsNotOnTrakt = new HashSet<>(localShows);

        // loop through shows on trakt, update the ones existing locally
        for (BaseShow traktShow : traktShows) {
            if (traktShow.show == null || traktShow.show.ids == null
                    || traktShow.show.ids.tvdb == null) {
                // trakt show misses required data
                continue;
            }
            if (!localShows.contains(traktShow.show.ids.tvdb)) {
                // trakt show not in local database
                continue;
            }

            localShowsNotOnTrakt.remove(traktShow.show.ids.tvdb);

            if (traktShow.seasons == null || traktShow.seasons.isEmpty()) {
                // trakt show has invalid episode data
                // do not touch show
                continue;
            }

            int resultCode = applyEpisodeFlagChanges(context, traktShow, flag, isMerging,
                    traktSync);
            if (resultCode < 0) {
                // upload failed, abort
                return resultCode;
            }
        }

        if (isMerging) {
            // upload flags of all shows NOT on trakt
            switch (flag) {
                case WATCHED:
                    return uploadWatchedEpisodes(context, traktSync, localShowsNotOnTrakt);
                case COLLECTED:
                    return uploadCollectedEpisodes(context, traktSync, localShowsNotOnTrakt);
            }
        } else {
            // clear flags on all shows NOT on trakt
            clearFlagsOfShow(context, flag, localShowsNotOnTrakt);
        }

        return SUCCESS;
    }

    /**
     * Flags the given episodes in the database (if they exist) and removes flags from all others
     * for this show.
     *
     * @param isMerging If set, you need to supply a {@code traktSync} instance.
     * @param traktSync If {@code isMerging} is set, needs to be NOT {@code null}.
     */
    public static int applyEpisodeFlagChanges(Context context, BaseShow traktShow, Flag flag,
            boolean isMerging, @Nullable Sync traktSync)
            throws OAuthUnauthorizedException {
        // guarantees:
        // show tvdb exists
        // show has at least one season
        // show is in local database

        final int showTvdbId = traktShow.show.ids.tvdb;
        final ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        final Uri ofShowUri = SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId);

        // if not merging, clear all flags for episodes of this show
        if (!isMerging) {
            // op-apply is ensured as loop below will run at least once (at least one season)
            batch.add(ContentProviderOperation
                    .newUpdate(ofShowUri)
                    .withSelection(flag.clearFlagSelection, null)
                    .withValue(flag.databaseColumn, flag.nonFlaggedValue)
                    .build());
        }

        // loop through seasons and build update ops for flagged episodes
        Set<Integer> traktFlaggedSeasonSet = new HashSet<>();
        List<SyncSeason> syncSeasons = new LinkedList<>();
        for (BaseSeason traktSeason : traktShow.seasons) {
            if (traktSeason.number == null || traktSeason.episodes == null) {
                // trakt season has no number
                continue;
            }

            // exclude from complete season upload
            traktFlaggedSeasonSet.add(traktSeason.number);

            // loop through episodes and add flag update ops
            Set<Integer> traktFlaggedEpisodeSet = new HashSet<>();
            for (BaseEpisode traktEpisode : traktSeason.episodes) {
                if (traktEpisode.number == null) {
                    // trakt episode has no number
                    continue;
                }

                // list as flagged on trakt
                traktFlaggedEpisodeSet.add(traktEpisode.number);

                batch.add(ContentProviderOperation
                        .newUpdate(ofShowUri)
                        .withSelection(SeriesGuideContract.Episodes.SEASON
                                + "=" + traktSeason.number
                                + " AND "
                                + SeriesGuideContract.Episodes.NUMBER
                                + "=" + traktEpisode.number, null)
                        .withValue(flag.databaseColumn, flag.flaggedValue)
                        .build());
            }

            if (isMerging) {
                // get local flagged episodes that are not flagged on trakt
                SyncSeason syncSeason = buildSeasonToUpload(context, showTvdbId, traktSeason.number,
                        traktFlaggedEpisodeSet, flag);
                if (syncSeason != null) {
                    syncSeasons.add(syncSeason);
                }
            }

            // apply batch of this season
            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e("Applying flag changes failed: " + showTvdbId + " season: "
                        + traktSeason.number + " column: " + flag, e);
                // do not abort, try other seasons
                // some episodes might be in incorrect state, but next update should fix that
                // this includes the clear flags op failing
            }
            batch.clear();
        }

        // if merging: upload all local flagged episodes NOT flagged on trakt
        if (isMerging && traktSync != null) {
            // append local flagged episodes FOR ALL seasons NOT on trakt (== not handled above)
            addRemainingSeasonsToUpload(context, showTvdbId, traktFlaggedSeasonSet, flag,
                    syncSeasons);

            // upload, if any
            if (!syncSeasons.isEmpty()) {
                Timber.d("applyEpisodeFlagChanges: upload " + syncSeasons.size() + " seasons for "
                        + showTvdbId);
                SyncItems syncItems = new SyncItems().shows(
                        new SyncShow().id(ShowIds.tvdb(showTvdbId)).seasons(syncSeasons));
                try {
                    switch (flag) {
                        case WATCHED:
                            traktSync.addItemsToWatchedHistory(syncItems);
                            break;
                        case COLLECTED:
                            traktSync.addItemsToCollection(syncItems);
                            break;
                    }
                } catch (RetrofitError e) {
                    Timber.e(e, "applyEpisodeFlagChanges: upload failed");
                    return FAILED_API;
                }
            }
        }

        return SUCCESS;
    }

    private static void addRemainingSeasonsToUpload(Context context, int showTvdbId,
            Set<Integer> traktFlaggedSeasonSet, Flag flag, List<SyncSeason> syncSeasons) {
        // query local seasons
        Cursor localSeasons = context.getContentResolver().query(
                SeriesGuideContract.Seasons.buildSeasonsOfShowUri(showTvdbId),
                new String[] { SeriesGuideContract.Seasons.COMBINED }, null, null, null);
        if (localSeasons == null) {
            // query failed
            return;
        }

        // build a list of local season numbers
        Set<Integer> localSeasonNumbers = new HashSet<>();
        while (localSeasons.moveToNext()) {
            localSeasonNumbers.add(localSeasons.getInt(0));
        }
        localSeasons.close();

        // loop through local seasons and add flagged episodes
        for (Integer localSeasonNumber : localSeasonNumbers) {
            if (traktFlaggedSeasonSet.contains(localSeasonNumber)) {
                // season was already processed as it is on trakt
                continue;
            }

            SyncSeason syncSeason = buildSeasonToUpload(context, showTvdbId, localSeasonNumber,
                    null, flag);
            if (syncSeason != null) {
                syncSeasons.add(syncSeason);
            }
        }
    }

    /**
     * Returns a list of flagged episodes of a season that are not included in the given set.
     * Packaged ready for upload to trakt in a {@link com.uwetrottmann.trakt.v2.entities.SyncSeason}.
     *
     * @param flaggedEpisodeNumbers If {@code null}, all flagged episodes of the season are
     * returned.
     */
    private static SyncSeason buildSeasonToUpload(Context context, int showTvdbId,
            int seasonNumber, Set<Integer> flaggedEpisodeNumbers, Flag flag) {
        // query for flagged episodes of the given season
        String flaggedSelection;
        switch (flag) {
            case WATCHED:
                flaggedSelection = SeriesGuideContract.Episodes.SELECTION_WATCHED;
                break;
            case COLLECTED:
                flaggedSelection = SeriesGuideContract.Episodes.SELECTION_COLLECTED;
                break;
            default:
                return null;
        }

        Cursor localSeason = context.getContentResolver().query(
                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                new String[] { SeriesGuideContract.Episodes.NUMBER },
                SeriesGuideContract.Episodes.SEASON + "=" + seasonNumber
                        + " AND " + flaggedSelection,
                null,
                SeriesGuideContract.Episodes.SORT_NUMBER_ASC);
        if (localSeason == null) {
            // query failed
            return null;
        }

        List<SyncEpisode> syncEpisodes = new LinkedList<>();
        while (localSeason.moveToNext()) {
            int episodeNumber = localSeason.getInt(0);
            if (flaggedEpisodeNumbers == null || !flaggedEpisodeNumbers.contains(episodeNumber)) {
                // episode NOT flagged on trakt
                syncEpisodes.add(new SyncEpisode().number(episodeNumber));
            }
        }
        localSeason.close();

        if (syncEpisodes.size() == 0) {
            // no local flagged episodes OR all local flagged episodes already flagged on trakt
            return null;
        }

        return new SyncSeason().number(seasonNumber).episodes(syncEpisodes);
    }

    private static void clearFlagsOfShow(Context context, Flag flag,
            HashSet<Integer> skippedShows) {
        if (skippedShows.size() == 0) {
            // nothing to do!
            return;
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (Integer tvShowTvdbId : skippedShows) {
            batch.add(ContentProviderOperation
                    .newUpdate(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(tvShowTvdbId))
                    .withSelection(flag.clearFlagSelection, null)
                    .withValue(flag.databaseColumn, flag.nonFlaggedValue).build());
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e("Clearing " + flag + " flags for shows failed");
            // continue, next sync will try again
        }
    }

    /**
     * Uploads all collected episodes for the given shows to trakt.
     *
     * @return Any of the {@link TraktTools} result codes.
     */
    private static int uploadCollectedEpisodes(Context context, Sync traktSync,
            HashSet<Integer> localShows) throws OAuthUnauthorizedException {
        // loop through given shows
        SyncShow syncShow = new SyncShow();
        SyncItems syncItems = new SyncItems().shows(syncShow);
        for (int showTvdbId : localShows) {
            syncShow.id(ShowIds.tvdb(showTvdbId));

            // query for watched episodes
            Cursor localEpisodes = context.getContentResolver()
                    .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                            EpisodesQuery.PROJECTION,
                            SeriesGuideContract.Episodes.SELECTION_COLLECTED,
                            null,
                            SeriesGuideContract.Episodes.SORT_SEASON_ASC);
            if (localEpisodes == null) {
                Timber.e("uploadCollectedEpisodes: query failed");
                return FAILED;
            }

            // build a list of watched episodes
            List<SyncSeason> episodesToUpload = new LinkedList<>();
            buildEpisodeList(localEpisodes, episodesToUpload);
            localEpisodes.close();

            if (episodesToUpload.size() == 0) {
                // nothing to upload for this show
                continue;
            }

            // upload
            try {
                syncShow.seasons = episodesToUpload;
                traktSync.addItemsToCollection(syncItems);
            } catch (RetrofitError e) {
                Timber.e(e, "uploadCollectedEpisodes: upload failed");
                return FAILED_API;
            }
        }

        Timber.d("uploadCollectedEpisodes: uploaded " + localShows.size() + " shows");
        return SUCCESS;
    }

    /**
     * Uploads all watched episodes for the given shows to trakt.
     *
     * @return Any of the {@link TraktTools} result codes.
     */
    private static int uploadWatchedEpisodes(Context context, Sync traktSync,
            HashSet<Integer> localShows) throws OAuthUnauthorizedException {
        // loop through given shows
        SyncShow syncShow = new SyncShow();
        SyncItems syncItems = new SyncItems().shows(syncShow);
        for (int showTvdbId : localShows) {
            syncShow.id(ShowIds.tvdb(showTvdbId));

            // query for watched episodes
            Cursor localEpisodes = context.getContentResolver().query(
                    SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                    EpisodesQuery.PROJECTION,
                    SeriesGuideContract.Episodes.SELECTION_WATCHED,
                    null,
                    SeriesGuideContract.Episodes.SORT_SEASON_ASC);
            if (localEpisodes == null) {
                Timber.e("uploadWatchedEpisodes: query failed");
                return FAILED;
            }

            // build a list of watched episodes
            List<SyncSeason> episodesToUpload = new LinkedList<>();
            buildEpisodeList(localEpisodes, episodesToUpload);
            localEpisodes.close();

            if (episodesToUpload.size() == 0) {
                // nothing to upload for this show
                continue;
            }

            // upload
            try {
                syncShow.seasons = episodesToUpload;
                traktSync.addItemsToWatchedHistory(syncItems);
            } catch (RetrofitError e) {
                Timber.e(e, "uploadWatchedEpisodes: upload failed");
                return FAILED_API;
            }
        }

        Timber.d("uploadWatchedEpisodes: uploaded " + localShows.size() + " shows");
        return SUCCESS;
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

    public static String buildEpisodeOrShowUrl(int tvdbId) {
        return TraktLink.tvdb(tvdbId);
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
        Search traktSearch = ServiceUtils.getTraktV2(context).search();

        // 3 results: may be a show, season or episode (TVDb ids are not unique)
        List<SearchResult> searchResults = traktSearch.idLookup(IdType.TVDB,
                String.valueOf(showTvdbId), 1, 3);
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

    public interface EpisodesQuery {

        public String[] PROJECTION = new String[] {
                SeriesGuideContract.Episodes.SEASON, SeriesGuideContract.Episodes.NUMBER
        };

        int SEASON = 0;
        int EPISODE = 1;
    }
}
