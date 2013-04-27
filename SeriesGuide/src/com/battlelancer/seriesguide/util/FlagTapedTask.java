/*
 * Copyright 2013 Uwe Trottmann
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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.battlelancer.seriesguide.util.FlagTapeEntry.Flag;
import com.battlelancer.seriesguide.util.FlagTask.FlagAction;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.services.ShowService;
import com.jakewharton.trakt.services.ShowService.EpisodeSeenBuilder;
import com.jakewharton.trakt.services.ShowService.EpisodeUnlibraryBuilder;
import com.jakewharton.trakt.services.ShowService.EpisodeUnseenBuilder;

import java.util.List;

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
                if (!Utils.isAllowedConnection(mContext)) {
                    postFailure(true);
                    return;
                }

                try {
                    switch (mAction) {
                        case EPISODE_WATCHED: {
                            Flag episode = mFlags.get(0);
                            if (mIsFlag) {
                                mShowService.episodeSeen(mShowId)
                                        .episode(episode.season, episode.episode).fire();
                            } else {
                                mShowService.episodeUnseen(mShowId).episode(episode.season,
                                        episode.episode).fire();
                            }
                            break;
                        }
                        case EPISODE_COLLECTED: {
                            Flag episode = mFlags.get(0);
                            if (mIsFlag) {
                                mShowService.episodeLibrary(mShowId)
                                        .episode(episode.season, episode.episode).fire();
                            } else {
                                mShowService.episodeUnlibrary(mShowId)
                                        .episode(episode.season, episode.episode).fire();
                            }
                            break;
                        }
                        case SEASON_WATCHED: {
                            if (mIsFlag) {
                                mShowService.seasonSeen(mShowId).season(mFlags.get(0).season)
                                        .fire();
                            } else {
                                EpisodeUnseenBuilder builder = mShowService.episodeUnseen(mShowId);
                                for (Flag episode : mFlags) {
                                    builder.episode(episode.season, episode.episode);
                                }
                                builder.fire();
                            }
                            break;
                        }
                        case SEASON_COLLECTED: {
                            if (mIsFlag) {
                                mShowService.seasonLibrary(mShowId).season(mFlags.get(0).season)
                                        .fire();
                            } else {
                                EpisodeUnlibraryBuilder builder = mShowService
                                        .episodeUnlibrary(mShowId);
                                for (Flag episode : mFlags) {
                                    builder.episode(episode.season, episode.episode);
                                }
                                builder.fire();
                            }
                            break;
                        }
                        case SHOW_WATCHED: {
                            if (mIsFlag) {
                                mShowService.showSeen(mShowId).fire();
                            } else {
                                EpisodeUnseenBuilder builder = mShowService.episodeUnseen(mShowId);
                                for (Flag episode : mFlags) {
                                    builder.episode(episode.season, episode.episode);
                                }
                                builder.fire();
                            }
                            break;
                        }
                        case SHOW_COLLECTED: {
                            if (mIsFlag) {
                                mShowService.showLibrary(mShowId).fire();
                            } else {
                                EpisodeUnlibraryBuilder builder = mShowService
                                        .episodeUnlibrary(mShowId);
                                for (Flag episode : mFlags) {
                                    builder.episode(episode.season, episode.episode);
                                }
                                builder.fire();
                            }
                            break;
                        }
                        case EPISODE_WATCHED_PREVIOUS: {
                            EpisodeSeenBuilder builder = mShowService.episodeSeen(mShowId);
                            for (Flag episode : mFlags) {
                                builder.episode(episode.season, episode.episode);
                            }
                            builder.fire();
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
                } catch (TraktException e) {
                    postFailure(false);
                } catch (ApiException e) {
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

}
