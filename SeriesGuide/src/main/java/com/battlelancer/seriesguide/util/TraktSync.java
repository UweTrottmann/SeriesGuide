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
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.services.ShowService;
import com.uwetrottmann.seriesguide.R;

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
            return TraktTools.FAILED_CREDENTIALS;
        }

        if (mIsSyncToTrakt) {
            return syncToTrakt(trakt);
        } else {
            // get show ids in local database
            HashSet<Integer> localShows = ShowTools.getShowTvdbIdsAsSet(mContext);
            if (localShows == null) {
                return TraktTools.FAILED;
            }
            return TraktTools.syncToSeriesGuide(mContext, trakt, localShows, mIsSyncingUnseen);
        }
    }

    private Integer syncToTrakt(Trakt trakt) {
        // get show ids in local database for which syncing is enabled
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[]{
                Shows._ID
        }, Shows.SYNCENABLED + "=1", null, null);

        if (showTvdbIds == null) {
            return TraktTools.FAILED;
        }
        if (showTvdbIds.getCount() == 0) {
            return TraktTools.SUCCESS_NOWORK;
        }

        Integer resultCode = TraktTools.SUCCESS;
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
                resultCode = TraktTools.FAILED_API;
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
            case TraktTools.FAILED:
                message = "Something went wrong. Please try again.";
                duration = Toast.LENGTH_LONG;
                break;
            case TraktTools.FAILED_API:
                message = mContext.getString(R.string.trakt_error_general);
                duration = Toast.LENGTH_LONG;
                break;
            case TraktTools.FAILED_CREDENTIALS:
                message = "Your credentials are incomplete. Please enter them again.";
                duration = Toast.LENGTH_LONG;
                break;
            case TraktTools.SUCCESS:
                message = "Finished syncing.";
                break;
            case TraktTools.SUCCESS_NOWORK:
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
