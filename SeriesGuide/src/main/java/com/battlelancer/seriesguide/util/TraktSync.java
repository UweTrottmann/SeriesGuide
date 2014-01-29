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

import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.jakewharton.trakt.enumerations.Extended;
import com.jakewharton.trakt.services.ShowService;
import com.jakewharton.trakt.services.UserService;
import com.uwetrottmann.seriesguide.R;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import retrofit.RetrofitError;

public class TraktSync extends AsyncTask<Void, Void, Integer> {

    private static final int SUCCESS_NOWORK = 0;

    private static final int FAILED_API = -1;

    private static final int FAILED = -2;

    private static final int FAILED_CREDENTIALS = -3;

    private static final int SUCCESS = -4;

    private static final String TAG = "TraktSync";

    private FragmentActivity mContext;

    private boolean mIsSyncToTrakt;

    private View mContainer;

    private boolean mIsSyncingUnseen;

    public TraktSync(FragmentActivity activity, View container, boolean isSyncToTrakt,
            boolean isSyncingUnseen) {
        mContext = activity;
        mContainer = container;
        mIsSyncToTrakt = isSyncToTrakt;
        mIsSyncingUnseen = isSyncingUnseen;
    }

    @Override
    protected void onPreExecute() {
        if (mIsSyncToTrakt) {
            mContainer.findViewById(R.id.progressBarToTraktSync).setVisibility(View.VISIBLE);
        } else {
            mContainer.findViewById(R.id.progressBarToDeviceSync).setVisibility(View.VISIBLE);
        }
        mContainer.findViewById(R.id.syncToDeviceButton).setEnabled(false);
        mContainer.findViewById(R.id.syncToTraktButton).setEnabled(false);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        Log.d(TAG, "Syncing with trakt...");

        Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
        if (trakt == null) {
            return FAILED_CREDENTIALS;
        }

        if (mIsSyncToTrakt) {
            return syncToTrakt(trakt);
        } else {
            // get show ids in local database
            HashSet<Integer> localShows = ShowTools.getShowTvdbIdsAsSet(mContext);
            if (localShows == null) {
                return FAILED;
            }
            return syncToSeriesGuide(mContext, trakt, localShows, mIsSyncingUnseen);
        }
    }

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
            Utils.trackExceptionAndLog(context, TAG, e);
            return FAILED_API;
        }
        if (remoteShows == null) {
            return FAILED_API;
        }
        int syncCountWatched = 0;
        if (!remoteShows.isEmpty()) {
            syncCountWatched = applyEpisodeFlagChanges(context, remoteShows, localShows,
                    Episodes.WATCHED, clearExistingFlags);
        }

        // collected episodes
        try {
            // get watched episodes from trakt
            remoteShows = userService.libraryShowsCollection(username, Extended.MIN);
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(context, TAG, e);
            return FAILED_API;
        }
        if (remoteShows == null) {
            return FAILED_API;
        }
        int syncCountCollection = 0;
        if (!remoteShows.isEmpty()) {
            syncCountCollection = applyEpisodeFlagChanges(context, remoteShows, localShows,
                    Episodes.COLLECTED, clearExistingFlags);
        }

        return Math.max(syncCountCollection, syncCountWatched);
    }

    private static int applyEpisodeFlagChanges(Context context, List<TvShow> remoteShows,
            HashSet<Integer> localShows, String episodeFlagColumn, boolean clearExistingFlags) {
        int syncCount = 0;

        // loop through shows on trakt, update the ones existing locally
        for (TvShow tvShow : remoteShows) {
            if (tvShow == null || tvShow.tvdb_id == null
                    || !localShows.contains(tvShow.tvdb_id)) {
                // does not match, skip
                continue;
            }

            applyEpisodeFlagChanges(context, tvShow, episodeFlagColumn, clearExistingFlags);

            syncCount++;
        }

        return syncCount;
    }

    /**
     * Applies database ops in small increments for the given episodes, setting the appropriate flag
     * in the given column.
     *
     * @param episodeFlagColumn  Which flag column the given data should change. Supports {@link
     *                           Episodes#WATCHED} and {@link Episodes#COLLECTED}.
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
            case Episodes.WATCHED:
                episodeFlag = EpisodeFlags.WATCHED;
                episodeDefaultFlag = EpisodeFlags.UNWATCHED;
                // do not remove flag of skipped episodes, only for watched ones
                clearSelection = Episodes.SEASON + "=? AND "
                        + Episodes.WATCHED + "=" + EpisodeFlags.WATCHED;
                break;
            case Episodes.COLLECTED:
                episodeFlag = 1;
                episodeDefaultFlag = 0;
                // only remove flags for already collected episodes
                clearSelection = Episodes.SEASON + "=? AND "
                        + Episodes.COLLECTED + "=1";
                break;
            default:
                return;
        }

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        for (TvShowSeason season : tvShow.seasons) {
            if (season == null || season.season == null ||
                    season.episodes == null || season.episodes.numbers == null) {
                continue;
            }

            if (clearExistingFlags) {
                // remove all flags for episodes of the current season
                batch.add(ContentProviderOperation
                        .newUpdate(Episodes.buildEpisodesOfShowUri(tvShow.tvdb_id))
                        .withSelection(clearSelection, new String[]{String.valueOf(season.season)})
                        .withValue(episodeFlagColumn, episodeDefaultFlag)
                        .build());
            }

            // build db ops to flag episodes according to given data
            for (Integer episode : season.episodes.numbers) {
                batch.add(ContentProviderOperation
                        .newUpdate(Episodes.buildEpisodesOfShowUri(tvShow.tvdb_id))
                        .withSelection(Episodes.SEASON + "=" + season.season + " AND "
                                + Episodes.NUMBER + "=" + episode, null)
                        .withValue(episodeFlagColumn, episodeFlag)
                        .build());
            }

            // apply batch of this season
            DBUtils.applyInSmallBatches(context, batch);
            batch.clear();
        }
    }

    private Integer syncToTrakt(Trakt trakt) {
        // get show ids in local database for which syncing is enabled
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[]{
                Shows._ID
        }, Shows.SYNCENABLED + "=1", null, null);

        if (showTvdbIds == null) {
            return FAILED;
        }
        if (showTvdbIds.getCount() == 0) {
            return SUCCESS_NOWORK;
        }

        Integer resultCode = SUCCESS;
        ShowService showService = trakt.showService();

        while (showTvdbIds.moveToNext()) {
            int showTvdbId = showTvdbIds.getInt(0);
            List<ShowService.Episodes.Episode> watchedEpisodes = new ArrayList<>();

            // build a list of all watched episodes
            Cursor seenEpisodes = mContext.getContentResolver().query(
                    Episodes.buildEpisodesOfShowUri(showTvdbId), TraktSyncQuery.PROJECTION,
                    TraktSyncQuery.SELECTION_WATCHED, null, null);
            if (seenEpisodes != null) {
                buildEpisodeList(watchedEpisodes, seenEpisodes);
                seenEpisodes.close();
            }

            // build unseen episodes trakt post
            List<ShowService.Episodes.Episode> unwatchedEpisodes = new ArrayList<>();
            if (mIsSyncingUnseen) {
                Cursor unseenEpisodes = mContext.getContentResolver().query(
                        Episodes.buildEpisodesOfShowUri(showTvdbId), TraktSyncQuery.PROJECTION,
                        TraktSyncQuery.SELECTION_UNWATCHED, null, null);
                if (unseenEpisodes != null) {
                    buildEpisodeList(unwatchedEpisodes, unseenEpisodes);
                    unseenEpisodes.close();
                }
            }

            // last chance to abort
            if (isCancelled()) {
                resultCode = null;
                break;
            }

            try {
                // post to trakt
                if (watchedEpisodes.size() > 0) {
                    showService.episodeSeen(new ShowService.Episodes(
                            showTvdbId, watchedEpisodes
                    ));
                }
                if (mIsSyncingUnseen && unwatchedEpisodes.size() > 0) {
                    showService.episodeUnseen(new ShowService.Episodes(
                            showTvdbId, unwatchedEpisodes
                    ));
                }
            } catch (RetrofitError e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
                resultCode = FAILED_API;
                break;
            }
        }

        showTvdbIds.close();
        return resultCode;
    }

    private static void buildEpisodeList(List<ShowService.Episodes.Episode> watchedEpisodes,
            Cursor seenEpisodes) {
        while (seenEpisodes.moveToNext()) {
            int season = seenEpisodes.getInt(0);
            int episode = seenEpisodes.getInt(1);
            watchedEpisodes.add(new ShowService.Episodes.Episode(season, episode));
        }
    }

    @Override
    protected void onCancelled() {
        Log.d(TAG, "Syncing with trakt...CANCELED");
        Toast.makeText(mContext, "Sync cancelled", Toast.LENGTH_LONG).show();
        restoreViewStates();
    }

    @Override
    protected void onPostExecute(Integer result) {
        Log.d(TAG, "Syncing with trakt...DONE (" + result + ")");

        String message = "";
        int duration = Toast.LENGTH_SHORT;

        switch (result) {
            case FAILED:
                message = "Something went wrong. Please try again.";
                duration = Toast.LENGTH_LONG;
                break;
            case FAILED_API:
                message = mContext.getString(R.string.trakt_error_general);
                duration = Toast.LENGTH_LONG;
                break;
            case FAILED_CREDENTIALS:
                message = "Your credentials are incomplete. Please enter them again.";
                duration = Toast.LENGTH_LONG;
                break;
            case SUCCESS:
                message = "Finished syncing.";
                break;
            case SUCCESS_NOWORK:
                message = "There was nothing to sync.";
                break;
            default:
                message = "Finished syncing " + result + " show(s).";
                break;
        }

        Toast.makeText(mContext, message, duration).show();
        restoreViewStates();
    }

    private void restoreViewStates() {
        if (mIsSyncToTrakt) {
            mContainer.findViewById(R.id.progressBarToTraktSync).setVisibility(View.GONE);
        } else {
            mContainer.findViewById(R.id.progressBarToDeviceSync).setVisibility(View.GONE);
        }
        mContainer.findViewById(R.id.syncToDeviceButton).setEnabled(true);
        mContainer.findViewById(R.id.syncToTraktButton).setEnabled(true);
    }

    public interface TraktSyncQuery {

        public String[] PROJECTION = new String[]{
                Episodes.SEASON, Episodes.NUMBER
        };

        public String SELECTION_WATCHED = Episodes.WATCHED + "=" + EpisodeFlags.WATCHED;

        public String SELECTION_UNWATCHED = Episodes.WATCHED + "!=" + EpisodeFlags.WATCHED;
    }
}
