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

import com.battlelancer.seriesguide.util.FlagTapeEntry.Flag;
import com.battlelancer.seriesguide.util.FlagTask.FlagAction;
import com.jakewharton.trakt.services.ShowService;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;

public class FlagTapedTask {

    public interface Callback {

        void onSuccess();

        void onFailure(boolean isNotConnected);
    }

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    private Context mContext;

    private ShowService mShowService;

    private FlagAction mAction;

    private int mShowId;

    private List<Flag> mFlags;

    private boolean mIsFlag;

    public FlagTapedTask(Context context, ShowService showService, FlagAction action, int showId,
            List<Flag> flags, boolean isFlag) {
        mContext = context;
        mShowService = showService;
        mAction = action;
        mShowId = showId;
        mFlags = flags;
        mIsFlag = isFlag;
    }

    public void execute(final Callback callback) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // do not even try if we are offline
                if (!AndroidUtils.isNetworkConnected(mContext)) {
                    postFailure(true);
                    return;
                }

                try {
                    switch (mAction) {
                        case EPISODE_WATCHED: {
                            Flag episode = mFlags.get(0);
                            if (mIsFlag) {
                                mShowService.episodeSeen(
                                        new ShowService.Episodes(mShowId, episode.season,
                                                episode.episode));
                            } else {
                                mShowService.episodeUnseen(
                                        new ShowService.Episodes(mShowId, episode.season,
                                                episode.episode));
                            }
                            break;
                        }
                        case EPISODE_COLLECTED: {
                            Flag episode = mFlags.get(0);
                            if (mIsFlag) {
                                mShowService.episodeLibrary(
                                        new ShowService.Episodes(mShowId, episode.season,
                                                episode.episode));
                            } else {
                                mShowService.episodeUnlibrary(
                                        new ShowService.Episodes(mShowId, episode.season,
                                                episode.episode));
                            }
                            break;
                        }
                        case SEASON_WATCHED: {
                            if (mIsFlag) {
                                mShowService.seasonSeen(
                                        new ShowService.Season(mShowId, mFlags.get(0).season));
                            } else {
                                mShowService.episodeUnseen(new ShowService.Episodes(
                                        mShowId, buildEpisodeList(mFlags)));
                            }
                            break;
                        }
                        case SEASON_COLLECTED: {
                            if (mIsFlag) {
                                mShowService.seasonLibrary(
                                        new ShowService.Season(mShowId, mFlags.get(0).season));
                            } else {
                                mShowService.episodeUnlibrary(new ShowService.Episodes(
                                        mShowId, buildEpisodeList(mFlags)));
                            }
                            break;
                        }
                        case SHOW_WATCHED: {
                            if (mIsFlag) {
                                mShowService.showSeen(new ShowService.Show(mShowId));
                            } else {
                                mShowService.episodeUnseen(new ShowService.Episodes(
                                        mShowId, buildEpisodeList(mFlags)
                                ));
                            }
                            break;
                        }
                        case SHOW_COLLECTED: {
                            if (mIsFlag) {
                                mShowService.showLibrary(new ShowService.Show(mShowId));
                            } else {
                                mShowService.episodeUnlibrary(new ShowService.Episodes(
                                        mShowId, buildEpisodeList(mFlags)
                                ));
                            }
                            break;
                        }
                        case EPISODE_WATCHED_PREVIOUS: {
                            mShowService.episodeSeen(new ShowService.Episodes(
                                    mShowId, buildEpisodeList(mFlags)
                            ));
                            break;
                        }
                    }

                    // Get back to the main thread before invoking a callback.
                    MAIN_THREAD.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onSuccess();
                        }
                    });
                } catch (RetrofitError e) {
                    postFailure(false);
                }
            }

            public void postFailure(final boolean isNotConnected) {
                // Get back to the main thread before invoking a callback.
                MAIN_THREAD.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailure(isNotConnected);
                    }
                });
            }
        }).start();
    }

    private static List<ShowService.Episodes.Episode> buildEpisodeList(List<Flag> flags) {
        List<ShowService.Episodes.Episode> episodes = new ArrayList<ShowService.Episodes.Episode>();
        for (Flag episode : flags) {
            episodes.add(new ShowService.Episodes.Episode(episode.season, episode.episode));
        }
        return episodes;
    }

}
