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
import android.support.v4.app.FragmentManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.jakewharton.trakt.enumerations.Extended;
import com.jakewharton.trakt.enumerations.Rating;
import com.jakewharton.trakt.services.UserService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktTools {

    // Sync status codes
    static final int SUCCESS_NOWORK = 0;
    static final int FAILED_API = -1;
    static final int FAILED = -2;
    static final int FAILED_CREDENTIALS = -3;
    static final int SUCCESS = -4;

    // Url parts
    private static final String TRAKT_SEARCH_BASE_URL = "https://trakt.tv/search/";
    private static final String TRAKT_SEARCH_SHOW_URL = TRAKT_SEARCH_BASE_URL + "tvdb?q=";
    private static final String TRAKT_SEARCH_MOVIE_URL = TRAKT_SEARCH_BASE_URL + "tmdb?q=";
    private static final String TRAKT_SEARCH_SEASON_ARG = "&s=";
    private static final String TRAKT_SEARCH_EPISODE_ARG = "&e=";

    /**
     * Downloads and sets watched and collected flags from trakt on local episodes.
     *
     * @param clearExistingFlags If set, all watched and collected (and only those, e.g. skipped
     *                           flag is preserved) flags will be removed prior to getting the
     *                           actual flags from trakt (season by season).
     * @return The number of shows synced (may be 0). Or -1 if there was an error.
     */
    public static int syncToSeriesGuide(Context context, Trakt trakt,
            HashSet<Integer> localShows, boolean clearExistingFlags) {
        if (localShows.size() == 0) {
            return SUCCESS_NOWORK;
        }

        final UserService userService = trakt.userService();
        final String username = TraktCredentials.get(context).getUsername();
        List<TvShow> remoteShows;

        // watched episodes
        try {
            // get watched episodes from trakt
            remoteShows = userService.libraryShowsWatched(username, Extended.MIN);
        } catch (RetrofitError e) {
            Timber.e(e, "Downloading watched shows failed");
            return FAILED_API;
        }
        if (remoteShows == null) {
            return FAILED_API;
        }
        int syncCountWatched = 0;
        if (!remoteShows.isEmpty()) {
            syncCountWatched = applyEpisodeFlagChanges(context, remoteShows, localShows,
                    SeriesGuideContract.Episodes.WATCHED, clearExistingFlags);
        }

        // collected episodes
        try {
            // get watched episodes from trakt
            remoteShows = userService.libraryShowsCollection(username, Extended.MIN);
        } catch (RetrofitError e) {
            Timber.e(e, "Downloading collected shows failed");
            return FAILED_API;
        }
        if (remoteShows == null) {
            return FAILED_API;
        }
        int syncCountCollection = 0;
        if (!remoteShows.isEmpty()) {
            syncCountCollection = applyEpisodeFlagChanges(context, remoteShows, localShows,
                    SeriesGuideContract.Episodes.COLLECTED, clearExistingFlags);
        }

        return Math.max(syncCountCollection, syncCountWatched);
    }

    private static int applyEpisodeFlagChanges(Context context, List<TvShow> remoteShows,
            HashSet<Integer> localShows, String episodeFlagColumn, boolean clearExistingFlags) {
        int syncCount = 0;
        HashSet<Integer> skippedShows = new HashSet<>(localShows);

        // loop through shows on trakt, update the ones existing locally
        for (TvShow tvShow : remoteShows) {
            if (tvShow == null || tvShow.tvdb_id == null
                    || !localShows.contains(tvShow.tvdb_id)) {
                // does not match, skip
                continue;
            }

            applyEpisodeFlagChanges(context, tvShow, episodeFlagColumn, clearExistingFlags);

            skippedShows.remove(tvShow.tvdb_id);
            syncCount++;
        }

        // clear flags on all shows not synced
        if (clearExistingFlags && skippedShows.size() > 0) {
            clearFlagsOfShow(context, episodeFlagColumn, skippedShows);
        }

        return syncCount;
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
        DBUtils.applyInSmallBatches(context, batch);
    }

    /**
     * Applies database ops in small increments for the given episodes, setting the appropriate
     * flag
     * in the given column.
     *
     * @param episodeFlagColumn  Which flag column the given data should change. Supports {@link
     *                           com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes#WATCHED}
     *                           and {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes#COLLECTED}.
     * @param clearExistingFlags If set, existing flags for all of this shows episodes will be set
     *                           to the default flag prior applying other changes.
     */
    public static void applyEpisodeFlagChanges(Context context,
            TvShow tvShow, String episodeFlagColumn, boolean clearExistingFlags) {
        if (tvShow.seasons == null) {
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
                clearSelection = SeriesGuideContract.Episodes.SEASON + "=? AND "
                        + SeriesGuideContract.Episodes.WATCHED + "=" + EpisodeFlags.WATCHED;
                break;
            case SeriesGuideContract.Episodes.COLLECTED:
                episodeFlag = 1;
                episodeDefaultFlag = 0;
                // only remove flags for already collected episodes
                clearSelection = SeriesGuideContract.Episodes.SEASON + "=? AND "
                        + SeriesGuideContract.Episodes.COLLECTED + "=1";
                break;
            default:
                return;
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        if (clearExistingFlags) {
            // remove all flags for episodes of this show
            // loop below will run at least once (would not be here if not at least one season),
            // so op-apply is ensured
            batch.add(ContentProviderOperation
                    .newUpdate(SeriesGuideContract.Episodes.buildEpisodesOfShowUri(tvShow.tvdb_id))
                    .withSelection(clearSelection, null)
                    .withValue(episodeFlagColumn, episodeDefaultFlag)
                    .build());
        }

        for (TvShowSeason season : tvShow.seasons) {
            if (season == null || season.season == null ||
                    season.episodes == null || season.episodes.numbers == null) {
                continue;
            }

            // build db ops to flag episodes according to given data
            for (Integer episode : season.episodes.numbers) {
                batch.add(ContentProviderOperation
                        .newUpdate(
                                SeriesGuideContract.Episodes.buildEpisodesOfShowUri(tvShow.tvdb_id))
                        .withSelection(
                                SeriesGuideContract.Episodes.SEASON + "=" + season.season + " AND "
                                        + SeriesGuideContract.Episodes.NUMBER + "=" + episode, null)
                        .withValue(episodeFlagColumn, episodeFlag)
                        .build());
            }

            // apply batch of this season
            DBUtils.applyInSmallBatches(context, batch);
            batch.clear();
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

    public static String buildRatingPercentageString(Integer percentage) {
        return percentage == null ? "--%" : String.valueOf(percentage) + "%";
    }

    public static String buildRatingVotesString(Context context, Integer votes) {
        if (votes == null) {
            votes = 0;
        }
        return context.getResources().getQuantityString(R.plurals.votes, votes, votes);
    }

    public static String buildUserRatingString(Context context, Rating rating) {
        if (rating == null) {
            return context.getString(R.string.norating);
        }

        int resId;
        switch (rating) {
            case WeakSauce:
                resId = R.string.hate;
                break;
            case Terrible:
                resId = R.string.rating2;
                break;
            case Bad:
                resId = R.string.rating3;
                break;
            case Poor:
                resId = R.string.rating4;
                break;
            case Meh:
                resId = R.string.rating5;
                break;
            case Fair:
                resId = R.string.rating6;
                break;
            case Good:
                resId = R.string.rating7;
                break;
            case Great:
                resId = R.string.rating8;
                break;
            case Superb:
                resId = R.string.rating9;
                break;
            case TotallyNinja:
                resId = R.string.love;
                break;
            default:
                resId = R.string.norating;
                break;
        }

        return context.getString(resId);
    }

    public static void rateEpisode(Context context, FragmentManager fragmentManager, int showTvdbId,
            int seasonNumber, int episodeNumber) {
        if (!TraktCredentials.ensureCredentials(context)) {
            return;
        }
        TraktRateDialogFragment newFragment = TraktRateDialogFragment.newInstanceEpisode(
                showTvdbId, seasonNumber, episodeNumber);
        newFragment.show(fragmentManager, "traktratedialog");
    }

    public static void rateMovie(Context context, FragmentManager fragmentManager,
            int movieTmdbId) {
        if (!TraktCredentials.ensureCredentials(context)) {
            return;
        }
        TraktRateDialogFragment newFragment = TraktRateDialogFragment.newInstanceMovie(movieTmdbId);
        newFragment.show(fragmentManager, "traktratedialog");
    }
}
