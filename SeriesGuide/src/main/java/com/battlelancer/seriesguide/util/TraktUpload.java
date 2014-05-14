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
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.interfaces.OnTaskFinishedListener;
import com.battlelancer.seriesguide.interfaces.OnTaskProgressListener;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.services.ShowService;
import java.util.ArrayList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Uploads watched episodes in SeriesGuide to a users trakt library. Can optionally remove watched
 * flags on trakt if an episode is not watched in SeriesGuide.
 */
public class TraktUpload extends AsyncTask<Void, Integer, Integer> {

    private final Context mContext;
    private final OnTaskFinishedListener mFinishedListener;
    private final OnTaskProgressListener mProgressListener;
    private final boolean mIsUploadingUnwatched;

    public TraktUpload(Context context, OnTaskFinishedListener finishedListener,
            OnTaskProgressListener progressListener, boolean isUploadingUnwatched) {
        mContext = context;
        mFinishedListener = finishedListener;
        mProgressListener = progressListener;
        mIsUploadingUnwatched = isUploadingUnwatched;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        Timber.d("Syncing with trakt...");

        Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
        if (trakt == null) {
            return TraktTools.FAILED_CREDENTIALS;
        }

        return uploadToTrakt(trakt);
    }

    private Integer uploadToTrakt(Trakt trakt) {
        // get a list of show ids in the local database
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[] {
                Shows._ID
        }, null, null, null);
        if (showTvdbIds == null) {
            return TraktTools.FAILED;
        }
        final int count = showTvdbIds.getCount();
        if (count == 0) {
            // nothing to upload
            return TraktTools.SUCCESS_NOWORK;
        }

        Integer resultCode = TraktTools.SUCCESS;
        ShowService showService = trakt.showService();

        while (showTvdbIds.moveToNext()) {
            int showTvdbId = showTvdbIds.getInt(0);

            // build a list of all watched episodes
            /**
             * We do not have to worry about uploading episodes that are already watched on
             * trakt, it will keep the original timestamp of the episodes being watched.
             */
            List<ShowService.Episodes.Episode> watchedEpisodesToUpload = new ArrayList<>();
            Cursor watchedEpisodes = mContext.getContentResolver().query(
                    Episodes.buildEpisodesOfShowUri(showTvdbId), EpisodesQuery.PROJECTION,
                    Episodes.SELECTION_WATCHED, null, null);
            if (watchedEpisodes != null) {
                buildEpisodeList(watchedEpisodesToUpload, watchedEpisodes);
                watchedEpisodes.close();
            }

            // if requested, build a list of all unwatched episodes
            List<ShowService.Episodes.Episode> unwatchedEpisodesToUpload = new ArrayList<>();
            if (mIsUploadingUnwatched) {
                Cursor unwatchedEpisodes = mContext.getContentResolver().query(
                        Episodes.buildEpisodesOfShowUri(showTvdbId), EpisodesQuery.PROJECTION,
                        Episodes.SELECTION_UNWATCHED, null, null);
                if (unwatchedEpisodes != null) {
                    buildEpisodeList(unwatchedEpisodesToUpload, unwatchedEpisodes);
                    unwatchedEpisodes.close();
                }
            }

            // build a list of collected episodes
            List<ShowService.Episodes.Episode> collectedEpisodesToUpload = new ArrayList<>();
            Cursor collectedEpisodes = mContext.getContentResolver()
                    .query(Episodes.buildEpisodesOfShowUri(showTvdbId),
                            EpisodesQuery.PROJECTION, Episodes.SELECTION_COLLECTED, null, null);
            if (collectedEpisodes != null) {
                buildEpisodeList(collectedEpisodesToUpload, collectedEpisodes);
                collectedEpisodes.close();
            }

            // last chance to abort
            if (isCancelled()) {
                resultCode = null;
                break;
            }

            try {
                // post to trakt
                // watched episodes
                if (watchedEpisodesToUpload.size() > 0) {
                    showService.episodeSeen(new ShowService.Episodes(
                            showTvdbId, watchedEpisodesToUpload
                    ));
                }
                // optionally unwatched episodes
                if (mIsUploadingUnwatched && unwatchedEpisodesToUpload.size() > 0) {
                    showService.episodeUnseen(new ShowService.Episodes(
                            showTvdbId, unwatchedEpisodesToUpload
                    ));
                }
                // collected episodes
                if (collectedEpisodesToUpload.size() > 0) {
                    showService.episodeLibrary(new ShowService.Episodes(
                            showTvdbId, collectedEpisodesToUpload
                    ));
                }
            } catch (RetrofitError e) {
                Timber.e(e, "Uploading episodes to trakt failed");
                resultCode = TraktTools.FAILED_API;
                break;
            }

            publishProgress(count, showTvdbIds.getPosition());
        }

        showTvdbIds.close();
        return resultCode;
    }

    private static void buildEpisodeList(List<ShowService.Episodes.Episode> episodesToUpload,
            Cursor episodes) {
        while (episodes.moveToNext()) {
            int season = episodes.getInt(EpisodesQuery.SEASON);
            int episode = episodes.getInt(EpisodesQuery.EPISODE);
            episodesToUpload.add(new ShowService.Episodes.Episode(season, episode));
        }
    }

    @Override
    protected void onCancelled() {
        Timber.d("Syncing with trakt...CANCELED");
        Toast.makeText(mContext, R.string.trakt_error_general, Toast.LENGTH_LONG).show();
        reportIsFinished();
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
        reportIsFinished();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mProgressListener != null) {
            mProgressListener.onProgressUpdate(values);
        }
    }

    private void reportIsFinished() {
        if (mFinishedListener != null) {
            mFinishedListener.onTaskFinished();
        }
    }

    public interface EpisodesQuery {

        public String[] PROJECTION = new String[] {
                Episodes.SEASON, Episodes.NUMBER
        };

        int SEASON = 0;
        int EPISODE = 1;
    }
}
