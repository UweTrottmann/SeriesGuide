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
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.BaseEpisode;
import com.uwetrottmann.trakt.v2.entities.BaseSeason;
import com.uwetrottmann.trakt.v2.entities.BaseShow;
import com.uwetrottmann.trakt.v2.entities.LastActivityMore;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import retrofit.RetrofitError;
import timber.log.Timber;

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
     * Downloads and sets watched and collected flags from trakt on local episodes.
     *
     * @param isInitialSync If not set, all watched and collected (and only those, e.g. not skipped
     * flag) flags will be removed prior to getting the actual flags from trakt (season by season).
     * @return Any of the {@link TraktTools} result codes.
     */
    public static int syncToSeriesGuide(Context context, HashSet<Integer> localShows,
            LastActivityMore activity, boolean isInitialSync) {
        if (localShows.size() == 0) {
            return SUCCESS_NOWORK;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return FAILED_CREDENTIALS;
        }
        Sync sync = trakt.sync();
        List<BaseShow> remoteShows;

        // watched episodes
        // only sync if flags have changed
        long lastWatched = activity.watched_at == null ? 0 : activity.watched_at.getMillis();
        if (isInitialSync || (lastWatched != 0
                && lastWatched > TraktSettings.getLastActivityEpisodesWatched(context))) {
            try {
                // get watched episodes from trakt
                remoteShows = sync.watchedShows(Extended.DEFAULT_MIN);
            } catch (RetrofitError e) {
                Timber.e(e, "Downloading watched shows failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }
            if (remoteShows == null) {
                return FAILED_API;
            }
            // apply any database updates
            if (!remoteShows.isEmpty()) {
                applyEpisodeFlagChanges(context, remoteShows, localShows,
                        SeriesGuideContract.Episodes.WATCHED, !isInitialSync);
            }
            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_ACTIVITY_EPISODES_WATCHED, lastWatched)
                    .apply();
        }

        // collected episodes
        // only sync if flags have changed
        long lastCollected = activity.collected_at == null ? 0 : activity.collected_at.getMillis();
        if (isInitialSync || (lastCollected != 0
                && lastCollected > TraktSettings.getLastActivityEpisodesCollected(context))) {
            try {
                // get watched episodes from trakt
                remoteShows = sync.collectionShows(Extended.DEFAULT_MIN);
            } catch (RetrofitError e) {
                Timber.e(e, "Downloading collected shows failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }
            if (remoteShows == null) {
                return FAILED_API;
            }
            if (!remoteShows.isEmpty()) {
                applyEpisodeFlagChanges(context, remoteShows, localShows,
                        SeriesGuideContract.Episodes.COLLECTED, !isInitialSync);
            }
            // store new last activity time
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(TraktSettings.KEY_LAST_ACTIVITY_EPISODES_WATCHED, lastCollected)
                    .apply();
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
    public static int uploadToTrakt(Context context, HashSet<Integer> localShows) {
        if (localShows.size() == 0) {
            return SUCCESS_NOWORK;
        }

        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(context);
        if (trakt == null) {
            return FAILED_CREDENTIALS;
        }
        Sync sync = trakt.sync();

        // loop through all local shows
        for (int showTvdbId : localShows) {
            // build a list of all watched episodes
            /**
             * We do not have to worry about uploading episodes that are already watched on
             * trakt, it will keep the original timestamp of the episodes being watched.
             */
            List<SyncSeason> watchedEpisodesToUpload = new LinkedList<>();
            Cursor watchedEpisodes = context.getContentResolver().query(
                    SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                    EpisodesQuery.PROJECTION,
                    SeriesGuideContract.Episodes.SELECTION_WATCHED,
                    null,
                    SeriesGuideContract.Episodes.SORT_SEASON_ASC);
            if (watchedEpisodes == null) {
                return FAILED;
            }
            buildEpisodeList(watchedEpisodes, watchedEpisodesToUpload);
            watchedEpisodes.close();

            // build a list of collected episodes
            List<SyncSeason> collectedEpisodesToUpload = new LinkedList<>();
            Cursor collectedEpisodes = context.getContentResolver()
                    .query(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(showTvdbId),
                            EpisodesQuery.PROJECTION,
                            SeriesGuideContract.Episodes.SELECTION_COLLECTED,
                            null,
                            SeriesGuideContract.Episodes.SORT_SEASON_ASC);
            if (collectedEpisodes == null) {
                return FAILED;
            }
            buildEpisodeList(collectedEpisodes, collectedEpisodesToUpload);
            collectedEpisodes.close();

            // post to trakt
            SyncShow show = new SyncShow().id(ShowIds.tvdb(showTvdbId));
            SyncItems items = new SyncItems().shows(show);
            try {
                // watched episodes
                if (watchedEpisodesToUpload.size() > 0) {
                    show.seasons = watchedEpisodesToUpload;
                    sync.addItemsToWatchedHistory(items);
                }
                // collected episodes
                if (collectedEpisodesToUpload.size() > 0) {
                    show.seasons = collectedEpisodesToUpload;
                    sync.addItemsToCollection(items);
                }
            } catch (RetrofitError e) {
                Timber.e(e, "Uploading episodes to trakt failed");
                return FAILED_API;
            } catch (OAuthUnauthorizedException e) {
                TraktCredentials.get(context).setCredentialsInvalid();
                return FAILED_CREDENTIALS;
            }
        }

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
