/*
 * Copyright 2011 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.SeriesGuideApplication;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.TvShow;
import com.jakewharton.trakt.entities.TvShowSeason;
import com.jakewharton.trakt.services.ShowService;
import com.uwetrottmann.androidutils.Lists;
import com.uwetrottmann.seriesguide.R;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.RemoteException;
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
        if (!ServiceUtils.hasTraktCredentials(mContext)) {
            return FAILED_CREDENTIALS;
        }

        Trakt manager = ServiceUtils.getTraktServiceManagerWithAuth(mContext, false);
        if (manager == null) {
            // password could not be decrypted
            return FAILED_CREDENTIALS;
        }

        if (mIsSyncToTrakt) {
            return syncToTrakt(manager);
        } else {
            return syncToSeriesGuide(manager, ServiceUtils.getTraktUsername(mContext));
        }
    }

    private Integer syncToSeriesGuide(Trakt manager, String username) {
        mResult = "";

        List<TvShow> shows;
        try {
            // get watched episodes from trakt
            shows = manager.userService().libraryShowsWatchedExtended(username);
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(TAG, e);
            return FAILED_API;
        }

        // get show ids in local database
        Cursor showTvdbIds = mContext.getContentResolver().query(Shows.CONTENT_URI, new String[]{
                Shows._ID
        }, null, null, null);

        // assume we have a local list of which shows to sync (later...)
        while (showTvdbIds.moveToNext()) {
            int tvdbId = showTvdbIds.getInt(0);
            for (TvShow tvShow : shows) {
                if (tvdbId == tvShow.tvdb_id) {
                    if (mResult.length() != 0) {
                        mResult += ", ";
                    }

                    if (mIsSyncingUnseen) {
                        ContentValues values = new ContentValues();
                        values.put(Episodes.WATCHED, false);
                        mContext.getContentResolver().update(
                                Episodes.buildEpisodesOfShowUri(tvdbId), values, null, null);
                    }

                    final ArrayList<ContentProviderOperation> batch = Lists.newArrayList();

                    // go through watched seasons, try to match them with local
                    // season
                    List<TvShowSeason> seasons = tvShow.seasons;
                    for (TvShowSeason season : seasons) {
                        Cursor seasonMatch = mContext.getContentResolver().query(
                                Seasons.buildSeasonsOfShowUri(tvdbId), new String[]{
                                Seasons._ID
                        }, Seasons.COMBINED + "=?", new String[]{
                                season.season.toString()
                        }, null);

                        // if we found a season, go on with its episodes
                        if (seasonMatch.moveToFirst()) {
                            String seasonId = seasonMatch.getString(0);

                            // build episodes update query to mark seen episodes

                            for (Integer episode : season.episodes.numbers) {
                                batch.add(ContentProviderOperation
                                        .newUpdate(Episodes.buildEpisodesOfSeasonUri(seasonId))
                                        .withSelection(Episodes.NUMBER + "=?", new String[]{
                                                episode.toString()
                                        }).withValue(Episodes.WATCHED, true).build());
                            }

                        }

                        seasonMatch.close();
                    }

                    // last chance to abort before doing work
                    if (isCancelled()) {
                        showTvdbIds.close();
                        return null;
                    }

                    try {
                        mContext.getContentResolver().applyBatch(
                                SeriesGuideApplication.CONTENT_AUTHORITY,
                                batch);
                    } catch (RemoteException e) {
                        // Failed binder transactions aren't recoverable
                        Utils.trackExceptionAndLog(TAG, e);
                        throw new RuntimeException("Problem applying batch operation", e);
                    } catch (OperationApplicationException e) {
                        // Failures like constraint violation aren't
                        // recoverable
                        Utils.trackExceptionAndLog(TAG, e);
                        throw new RuntimeException("Problem applying batch operation", e);
                    }

                    mResult += tvShow.title;

                    // remove synced show
                    shows.remove(tvShow);
                    break;
                }
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

        if (showTvdbIds.getCount() == 0) {
            return SUCCESS_NOWORK;
        }

        while (showTvdbIds.moveToNext()) {
            int showTvdbId = showTvdbIds.getInt(0);
            List<ShowService.Episodes.Episode> watchedEpisodes
                    = new ArrayList<ShowService.Episodes.Episode>();

            // build a list of all watched episodes
            Cursor seenEpisodes = mContext.getContentResolver().query(
                    Episodes.buildEpisodesOfShowUri(showTvdbId), new String[]{
                    Episodes.SEASON, Episodes.NUMBER
            }, Episodes.WATCHED + "=?", new String[]{
                    "1"
            }, null);
            if (seenEpisodes != null) {
                buildEpisodeList(watchedEpisodes, seenEpisodes);
                seenEpisodes.close();
            }

            // build unseen episodes trakt post
            List<ShowService.Episodes.Episode> unwatchedEpisodes
                    = new ArrayList<ShowService.Episodes.Episode>();
            if (mIsSyncingUnseen) {
                Cursor unseenEpisodes = mContext.getContentResolver().query(
                        Episodes.buildEpisodesOfShowUri(showTvdbId), new String[]{
                        Episodes.SEASON, Episodes.NUMBER
                }, Episodes.WATCHED + "=?", new String[]{
                        "0"
                }, null);
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
                Utils.trackExceptionAndLog(TAG, e);
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
}
