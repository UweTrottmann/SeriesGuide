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

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.services.ShowService;
import java.util.ArrayList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

public class TraktSync extends AsyncTask<Void, Void, Integer> {

    private Context mContext;

    private final Button mUploadButton;
    private final View mProgressIndicator;

    private boolean mIsSyncingUnseen;

    public TraktSync(Context context, Button uploadButton, View progressIndicator,
            boolean isSyncingUnseen) {
        mContext = context;
        mUploadButton = uploadButton;
        mProgressIndicator = progressIndicator;
        mIsSyncingUnseen = isSyncingUnseen;
    }

    @Override
    protected void onPreExecute() {
        mProgressIndicator.setVisibility(View.VISIBLE);
        mUploadButton.setEnabled(false);
    }

    @Override
    protected Integer doInBackground(Void... params) {
        Timber.d("Syncing with trakt...");

        Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
        if (trakt == null) {
            return TraktTools.FAILED_CREDENTIALS;
        }

        return syncToTrakt(trakt);
    }

    private Integer syncToTrakt(Trakt trakt) {
        // get show ids in local database for which syncing is enabled
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[] {
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
                Timber.e(e, "Uploading episodes failed");
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
        Timber.d("Syncing with trakt...CANCELED");
        Toast.makeText(mContext, R.string.trakt_error_general, Toast.LENGTH_LONG).show();
        restoreViewStates();
    }

    @Override
    protected void onPostExecute(Integer result) {
        Timber.d("Uploading to trakt...DONE (" + result + ")");

        int messageResId;
        int duration = Toast.LENGTH_SHORT;

        switch (result) {
            case TraktTools.FAILED:
            case TraktTools.FAILED_API:
                messageResId = R.string.trakt_error_general;
                duration = Toast.LENGTH_LONG;
                break;
            case TraktTools.FAILED_CREDENTIALS:
                messageResId = R.string.trakt_error_credentials;
                duration = Toast.LENGTH_LONG;
                break;
            case TraktTools.SUCCESS_NOWORK:
                messageResId = R.string.upload_no_work;
                break;
            default:
            case TraktTools.SUCCESS:
                messageResId = R.string.upload_done;
                break;
        }

        Toast.makeText(mContext, messageResId, duration).show();
        restoreViewStates();
    }

    private void restoreViewStates() {
        mProgressIndicator.setVisibility(View.GONE);
        mUploadButton.setEnabled(true);
    }

    public interface TraktSyncQuery {

        public String[] PROJECTION = new String[] {
                Episodes.SEASON, Episodes.NUMBER
        };

        public String SELECTION_WATCHED = Episodes.WATCHED + "=" + EpisodeFlags.WATCHED;

        public String SELECTION_UNWATCHED = Episodes.WATCHED + "!=" + EpisodeFlags.WATCHED;
    }
}
