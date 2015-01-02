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
import android.os.Handler;
import android.os.Looper;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.FlagTapeEntry.Flag;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt.v2.entities.ShowIds;
import com.uwetrottmann.trakt.v2.entities.SyncEpisode;
import com.uwetrottmann.trakt.v2.entities.SyncItems;
import com.uwetrottmann.trakt.v2.entities.SyncSeason;
import com.uwetrottmann.trakt.v2.entities.SyncShow;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import com.uwetrottmann.trakt.v2.services.Sync;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;

public class FlagTapedTask {

    public interface Callback {

        void onSuccess();

        void onFailure(boolean isNotConnected);
    }

    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    private final Context context;
    private final Sync traktSync;
    private final EpisodeTools.EpisodeAction flagAction;
    private final int showTvdbId;
    private final List<Flag> flags;
    private final boolean isAddNotDelete;

    public FlagTapedTask(Context context, Sync traktSync, EpisodeTools.EpisodeAction action,
            int showTvdbId, List<Flag> flags, boolean isAddNotDelete) {
        this.context = context;
        this.traktSync = traktSync;
        this.flagAction = action;
        this.showTvdbId = showTvdbId;
        this.flags = flags;
        this.isAddNotDelete = isAddNotDelete;
    }

    public void execute(final Callback callback) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // do not even try if we are offline
                if (!AndroidUtils.isNetworkConnected(context)) {
                    postFailure(true);
                    return;
                }

                // outer wrapper and show are always required
                SyncShow show = new SyncShow().id(ShowIds.tvdb(showTvdbId));
                SyncItems items = new SyncItems().shows(show);

                // add season or episodes
                switch (flagAction) {
                    case SEASON_WATCHED:
                    case SEASON_COLLECTED:
                        show.seasons(new SyncSeason().number(flags.get(0).season));
                        break;
                    case EPISODE_WATCHED:
                    case EPISODE_COLLECTED:
                        Flag flag = flags.get(0);
                        show.seasons(new SyncSeason().number(flag.season)
                                .episodes(new SyncEpisode().number(flag.episode)));
                        break;
                    case EPISODE_WATCHED_PREVIOUS:
                        show.seasons(buildEpisodeList(flags));
                        break;
                }

                // execute network call
                try {
                    switch (flagAction) {
                        case SHOW_WATCHED:
                        case SEASON_WATCHED:
                        case EPISODE_WATCHED:
                            if (isAddNotDelete) {
                                traktSync.addItemsToWatchedHistory(items);
                            } else {
                                traktSync.deleteItemsFromWatchedHistory(items);
                            }
                            break;
                        case SHOW_COLLECTED:
                        case SEASON_COLLECTED:
                        case EPISODE_COLLECTED:
                            if (isAddNotDelete) {
                                traktSync.addItemsToCollection(items);
                            } else {
                                traktSync.deleteItemsFromCollection(items);
                            }
                            break;
                        case EPISODE_WATCHED_PREVIOUS:
                            traktSync.addItemsToWatchedHistory(items);
                            break;
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
                } catch (OAuthUnauthorizedException e) {
                    TraktCredentials.get(context).setCredentialsInvalid();
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

    /**
     * Builds a list of {@link com.uwetrottmann.trakt.v2.entities.SyncSeason}. Iterates through
     * given flags based on the assumption they are sorted ascending by season number.
     */
    private static List<SyncSeason> buildEpisodeList(List<Flag> flags) {
        List<SyncSeason> seasons = new ArrayList<>();

        SyncSeason currentSeason = null;
        for (Flag flag : flags) {
            if (currentSeason != null && flag.season < currentSeason.number) {
                // skip out of order flags
                continue;
            }

            // start new season?
            if (currentSeason == null || flag.season > currentSeason.number) {
                currentSeason = new SyncSeason().number(flag.season);
                currentSeason.episodes = new LinkedList<>();
                seasons.add(currentSeason);
            }

            // add episode
            currentSeason.episodes.add(new SyncEpisode().number(flag.episode));
        }

        return seasons;
    }
}
