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
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseEpisode;
import com.uwetrottmann.trakt.v2.entities.BaseSeason;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.LastActivity;
import com.uwetrottmann.trakt.v2.entities.LastActivityMore;
import com.uwetrottmann.trakt.v2.entities.RatedEpisode;
import com.uwetrottmann.trakt.v2.entities.RatedMovie;
import com.uwetrottmann.trakt.v2.entities.RatedShow;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.enums.RatingsFilter;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
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
     * Downloads and sets watched and collected flags for episodes if they have changed on trakt (or
     * {@code isInitialSync} is true).
     *
     * @param isInitialSync If not set, all watched and collected (and only those, e.g. not skipped
     * flag) flags will be removed prior to getting the actual flags from trakt (season by season).
     * @return Any of the {@link TraktTools} result codes.
     */
    public static int downloadEpisodeFlags(Context context, HashSet<Integer> localShows,
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
        Sync sync = trakt.sync();

        // watched episodes
        if (isInitialSync || activity.watched_at.isAfter(
                TraktSettings.getLastEpisodesWatchedAt(context))) {
            List<BaseShow> remoteShows;
            try {
                // get watched episodes from trakt
                remoteShows = sync.watchedShows(Extended.DEFAULT_MIN);
            } catch (RetrofitError e) {
                Timber.e(e, "downloadEpisodeFlags: watched download failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }
            if (remoteShows == null) {
                Timber.e("downloadEpisodeFlags: null watched response");
                return FAILED_API;
            }
            // apply database updates
            if (!remoteShows.isEmpty()) {
                applyEpisodeFlagChanges(context, remoteShows, localShows,
                        SeriesGuideContract.Episodes.WATCHED, !isInitialSync);
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
            List<BaseShow> remoteShows;
            try {
                // get collected episodes from trakt
                remoteShows = sync.collectionShows(Extended.DEFAULT_MIN);
            } catch (RetrofitError e) {
                Timber.e(e, "downloadEpisodeFlags: collected download failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }
            if (remoteShows == null) {
                Timber.e("downloadEpisodeFlags: null collected response");
                return FAILED_API;
            }
            // apply database updates
            if (!remoteShows.isEmpty()) {
                applyEpisodeFlagChanges(context, remoteShows, localShows,
                        SeriesGuideContract.Episodes.COLLECTED, !isInitialSync);
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

    private static void applyEpisodeFlagChanges(Context context, List<BaseShow> remoteShows,
            HashSet<Integer> localShows, String episodeFlagColumn, boolean clearExistingFlags) {
        HashSet<Integer> skippedShows = new HashSet<>(localShows);

        // loop through shows on trakt, update the ones existing locally
        for (BaseShow show : remoteShows) {
            if (show.show == null || show.show.ids == null || show.show.ids.tvdb == null
                    || !localShows.contains(show.show.ids.tvdb)) {
                continue; // skip
            }

            applyEpisodeFlagChanges(context, show, episodeFlagColumn, clearExistingFlags);

            skippedShows.remove(show.show.ids.tvdb);
        }

        // clear flags on all shows not synced
        if (clearExistingFlags && skippedShows.size() > 0) {
            clearFlagsOfShow(context, episodeFlagColumn, skippedShows);
        }
    }

    private static void clearFlagsOfShow(Context context, String episodeFlagColumn,
            HashSet<Integer> skippedShows) {
        int episodeDefaultFlag;
        switch (episodeFlagColumn) {
            case SeriesGuideContract.Episodes.WATCHED:
                episodeDefaultFlag = EpisodeFlags.UNWATCHED;
                break;
            case SeriesGuideContract.Episodes.COLLECTED:
            default:
                episodeDefaultFlag = 0;
                break;
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (Integer tvShowTvdbId : skippedShows) {
            batch.add(ContentProviderOperation
                    .newUpdate(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(tvShowTvdbId))
                    .withValue(episodeFlagColumn, episodeDefaultFlag).build());
        }
        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e("Clearing " + episodeFlagColumn + " flags for shows failed");
            // continue, next sync will try again
        }
    }

    /**
     * Applies database ops in small increments for the given episodes, setting the appropriate flag
     * in the given column.
     *
     * @param episodeFlagColumn Which flag column the given data should change. Supports {@link
     * com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes#WATCHED} and {@link
     * com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes#COLLECTED}.
     * @param clearExistingFlags If set, existing flags for all of this shows episodes will be set
     */
    public static void applyEpisodeFlagChanges(Context context,
            BaseShow show, String episodeFlagColumn, boolean clearExistingFlags) {
        if (show.seasons == null) {
            return;
        }

        int episodeFlag;
        int episodeDefaultFlag;
        String clearSelection;
        switch (episodeFlagColumn) {
            case SeriesGuideContract.Episodes.WATCHED:
                episodeFlag = EpisodeFlags.WATCHED;
                episodeDefaultFlag = EpisodeFlags.UNWATCHED;
                // do not remove flag of skipped episodes, only for watched ones
                clearSelection = SeriesGuideContract.Episodes.WATCHED + "=" + EpisodeFlags.WATCHED;
                break;
            case SeriesGuideContract.Episodes.COLLECTED:
                episodeFlag = 1;
                episodeDefaultFlag = 0;
                // only remove flags for already collected episodes
                clearSelection = SeriesGuideContract.Episodes.COLLECTED + "=1";
                break;
            default:
                return;
        }

        final int showTvdbId = show.show.ids.tvdb;
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        if (clearExistingFlags) {
            // remove all flags for episodes of this show
            // loop below will run at least once (would not be here if not at least one season),
            // so op-apply is ensured
            batch.add(ContentProviderOperation
                    .newUpdate(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId))
                    .withSelection(clearSelection, null)
                    .withValue(episodeFlagColumn, episodeDefaultFlag)
                    .build());
        }

        for (BaseSeason season : show.seasons) {
            if (season.number == null || season.episodes == null) {
                continue; // skip
            }

            // build db ops to flag episodes according to given data
            for (BaseEpisode episode : season.episodes) {
                if (episode.number == null) {
                    continue; // skip
                }
                batch.add(ContentProviderOperation
                        .newUpdate(
                                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId))
                        .withSelection(
                                SeriesGuideContract.Episodes.SEASON + "=" + season.number
                                        + " AND " + SeriesGuideContract.Episodes.NUMBER + "="
                                        + episode.number,
                                null
                        )
                        .withValue(episodeFlagColumn, episodeFlag)
                        .build());
            }

            // apply batch of this season
            try {
                DBUtils.applyInSmallBatches(context, batch);
            } catch (OperationApplicationException e) {
                Timber.e("Applying flag changes failed: " + showTvdbId + " season: "
                        + season.number + " column: " + episodeFlagColumn, e);
                // do not abort, try other seasons
                // some episodes might be in incorrect state, but next update should fix that
                // this includes the clear flags op failing
            }
            batch.clear();
        }
    }

    /**
     * Uploads all watched and collected episodes to trakt.
     *
     * @return Any of the {@link TraktTools} result codes.
     */
    public static int uploadEpisodeFlags(Context context, HashSet<Integer> localShows) {
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return FAILED_CREDENTIALS;
        }
        Sync sync = trakt.sync();

        // loop through all local shows
        SyncItems items = new SyncItems();
        SyncShow show = new SyncShow();
        items.shows(show);
        for (int showTvdbId : localShows) {
            show.id(ShowIds.tvdb(showTvdbId));

            /**
             * We do not have to worry about uploading episodes that are already watched or
             * collected on trakt, it will keep the original timestamp.
             */

            // build a list of all watched episodes
            List<SyncSeason> watchedEpisodesToUpload = new LinkedList<>();
            Cursor watchedEpisodes = context.getContentResolver().query(
                    SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                    EpisodesQuery.PROJECTION,
                    SeriesGuideContract.Episodes.SELECTION_WATCHED,
                    null,
                    SeriesGuideContract.Episodes.SORT_SEASON_ASC);
            if (watchedEpisodes == null) {
                Timber.e("uploadEpisodeFlags: watched query failed");
                return FAILED;
            }
            buildEpisodeList(watchedEpisodes, watchedEpisodesToUpload);
            watchedEpisodes.close();

            // upload watched to trakt
            try {
                if (watchedEpisodesToUpload.size() > 0) {
                    show.seasons = watchedEpisodesToUpload;
                    sync.addItemsToWatchedHistory(items);
                }
            } catch (RetrofitError e) {
                Timber.e(e, "uploadEpisodeFlags: watched upload failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }

            // build a list of collected episodes
            List<SyncSeason> collectedEpisodesToUpload = new LinkedList<>();
            Cursor collectedEpisodes = context.getContentResolver()
                    .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                            EpisodesQuery.PROJECTION,
                            SeriesGuideContract.Episodes.SELECTION_COLLECTED,
                            null,
                            SeriesGuideContract.Episodes.SORT_SEASON_ASC);
            if (collectedEpisodes == null) {
                Timber.e("uploadEpisodeFlags: collected query failed");
                return FAILED;
            }
            buildEpisodeList(collectedEpisodes, collectedEpisodesToUpload);
            collectedEpisodes.close();

            // upload collected to trakt
            try {
                // collected episodes
                if (collectedEpisodesToUpload.size() > 0) {
                    show.seasons = collectedEpisodesToUpload;
                    sync.addItemsToCollection(items);
                }
            } catch (RetrofitError e) {
                Timber.e(e, "uploadEpisodeFlags: collected upload failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }
        }

        Timber.d("uploadEpisodeFlags: success, uploaded " + localShows.size() + " shows");
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

    public static String buildEpisodeOrShowUrl(int showTvdbId, int seasonNumber,
            int episodeNumber) {
        String uri;
        if (seasonNumber < 0 || episodeNumber < 0) {
            // look just for the show page
            uri = TRAKT_SEARCH_SHOW_URL + showTvdbId;
        } else {
            // look for the episode page
            uri = TRAKT_SEARCH_SHOW_URL + showTvdbId
                    + TRAKT_SEARCH_SEASON_ARG + seasonNumber
                    + TRAKT_SEARCH_EPISODE_ARG + episodeNumber;
        }
        return uri;
    }

    public static String buildMovieUrl(int movieTmdbId) {
        return TRAKT_SEARCH_MOVIE_URL + movieTmdbId;
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

    public interface EpisodesQuery {

        public String[] PROJECTION = new String[] {
                SeriesGuideContract.Episodes.SEASON, SeriesGuideContract.Episodes.NUMBER
        };

        int SEASON = 0;
        int EPISODE = 1;
    }
}
