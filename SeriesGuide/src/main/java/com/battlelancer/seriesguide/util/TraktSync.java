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
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.jakewharton.trakt.services.ShowService;
import com.uwetrottmann.androidutils.Lists;
import com.uwetrottmann.seriesguide.R;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;

public class TraktSync extends AsyncTask<Void, Void, Integer> {

    private static final int SUCCESS_WORK = 100;

    private static final int SUCCESS_NOWORK = 101;

    private static final int FAILED_CREDENTIALS = 102;

    private static final int FAILED_API = 103;

    private static final int FAILED = 104;

    private static final String TAG = "TraktSync";

    private FragmentActivity mContext;

    private String mResult;

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
        Trakt manager = ServiceUtils.getTraktWithAuth(mContext);
        if (manager == null) {
            return FAILED_CREDENTIALS;
        }

        if (mIsSyncToTrakt) {
            return syncToTrakt(manager);
        } else {
            return syncToSeriesGuide(manager, TraktCredentials.get(mContext).getUsername());
        }
    }

    private Integer syncToSeriesGuide(Trakt manager, String username) {
        mResult = "";

        List<TvShow> shows;
        try {
            // get watched episodes from trakt
            shows = manager.userService().libraryShowsWatchedMinimum(username);
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(mContext, TAG, e);
            return FAILED_API;
        }
        if (shows == null) {
            return FAILED;
        }
        if (shows.isEmpty()) {
            return SUCCESS_NOWORK;
        }

        // get show ids in local database
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[]{
                Shows._ID
        }, null, null, null);
        if (showTvdbIds == null) {
            return FAILED;
        }

        // assume we have a local list of which shows to sync (later...)
        final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();
        while (showTvdbIds.moveToNext()) {
            int showTvdbId = showTvdbIds.getInt(0);

            // find a show with matching tvdb id
            for (TvShow tvShow : shows) {
                if (tvShow == null || tvShow.tvdb_id == null || tvShow.tvdb_id != showTvdbId) {
                    // does not match, skip
                    continue;
                }

                buildSeasonBatch(mContext, batch, tvShow, Episodes.WATCHED, EpisodeFlags.WATCHED);

                // last chance to abort
                if (isCancelled()) {
                    showTvdbIds.close();
                    return null;
                }

                // apply batch
                if (mIsSyncingUnseen) {
                    // remove any flags from all episodes
                    ContentValues values = new ContentValues();
                    values.put(Episodes.WATCHED, EpisodeFlags.UNWATCHED);
                    mContext.getContentResolver().update(
                            Episodes.buildEpisodesOfShowUri(showTvdbId), values, null, null);
                }
                DBUtils.applyInSmallBatches(mContext, batch);
                batch.clear();

                // add to result string
                if (mResult.length() != 0) {
                    mResult += ", ";
                }
                mResult += tvShow.title;

                // remove synced show to reduce next max loop count
                shows.remove(tvShow);

                // found matching show, get next one from cursor
                break;
            }
        }

        showTvdbIds.close();

        if (mResult.length() != 0) {
            return SUCCESS_WORK;
        } else {
            return SUCCESS_NOWORK;
        }
    }

    private Integer syncToTrakt(Trakt manager) {
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
                showTvdbIds.close();
                return null;
            }

            try {
                // post to trakt
                if (watchedEpisodes.size() > 0) {
                    manager.showService().episodeSeen(new ShowService.Episodes(
                            showTvdbId, watchedEpisodes
                    ));
                }
                if (mIsSyncingUnseen && unwatchedEpisodes.size() > 0) {
                    manager.showService().episodeUnseen(new ShowService.Episodes(
                            showTvdbId, unwatchedEpisodes
                    ));
                }
            } catch (RetrofitError e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
                return FAILED_API;
            }
        }

        showTvdbIds.close();
        return SUCCESS_WORK;
    }

    @Override
    protected void onCancelled() {
        Toast.makeText(mContext, "Sync cancelled", Toast.LENGTH_LONG).show();
        restoreViewStates();
    }

    @Override
    protected void onPostExecute(Integer result) {
        String message = "";
        int duration = Toast.LENGTH_SHORT;

        switch (result) {
            case SUCCESS_WORK:
                message = "Finished syncing";
                if (mResult != null) {
                    message += " (" + mResult + ")";
                }
                break;
            case SUCCESS_NOWORK:
                message = "There was nothing to sync.";
                break;
            case FAILED_CREDENTIALS:
                message = "Your credentials are incomplete. Please enter them again.";
                duration = Toast.LENGTH_LONG;
                break;
            case FAILED:
                message = "Something went wrong. Please try again.";
                duration = Toast.LENGTH_LONG;
                break;
            case FAILED_API:
                message = "Could not communicate with trakt servers. Try again later.";
                duration = Toast.LENGTH_LONG;
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

    private static void buildEpisodeList(List<ShowService.Episodes.Episode> watchedEpisodes,
            Cursor seenEpisodes) {
        while (seenEpisodes.moveToNext()) {
            int season = seenEpisodes.getInt(0);
            int episode = seenEpisodes.getInt(1);
            watchedEpisodes.add(new ShowService.Episodes.Episode(season, episode));
        }
    }

    /**
     * Adds database ops for episodes of seasons existing locally, with the op setting the given
     * episode column to the given flag.
     */
    public static void buildSeasonBatch(Context context, ArrayList<ContentProviderOperation> batch,
            TvShow tvShow, String episodeFlagColumn, int episodeFlag) {
        if (tvShow.seasons == null) {
            return;
        }

        // go through watched seasons, try to match them with local season
        for (TvShowSeason season : tvShow.seasons) {
            if (season == null || season.season == null ||
                    season.episodes == null || season.episodes.numbers == null) {
                continue;
            }

            // get local season
            Cursor seasonMatch = context.getContentResolver().query(
                    Seasons.buildSeasonsOfShowUri(tvShow.tvdb_id), new String[]{
                    Seasons._ID
            }, Seasons.COMBINED + "=?", new String[]{
                    season.season.toString()
            }, null);
            if (seasonMatch == null) {
                continue;
            }

            // build db ops to flag local episodes according to given data
            if (seasonMatch.moveToFirst()) {
                String seasonId = seasonMatch.getString(0);

                for (Integer episode : season.episodes.numbers) {
                    batch.add(ContentProviderOperation
                            .newUpdate(Episodes.buildEpisodesOfSeasonUri(seasonId))
                            .withSelection(Episodes.NUMBER + "=?", new String[]{
                                    episode.toString()
                            }).withValue(episodeFlagColumn, episodeFlag)
                            .build());
                }
            }

            seasonMatch.close();
        }
    }

    public interface TraktSyncQuery {

        public String[] PROJECTION = new String[]{
                Episodes.SEASON, Episodes.NUMBER
        };

        public String SELECTION_WATCHED = Episodes.WATCHED + "=" + EpisodeFlags.WATCHED;

        public String SELECTION_UNWATCHED = Episodes.WATCHED + "!=" + EpisodeFlags.WATCHED;
    }
}
