/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.Username;
import com.uwetrottmann.trakt5.enums.Extended;
import com.uwetrottmann.trakt5.enums.HistoryType;
import java.util.List;
import retrofit2.Call;

/**
 * Loads last 72 hours of trakt watched movies, or at least one older watched movie.
 */
public class TraktRecentMovieHistoryLoader extends TraktRecentEpisodeHistoryLoader {

    public TraktRecentMovieHistoryLoader(Context context) {
        super(context);
    }

    @Override
    protected void addItems(List<NowAdapter.NowItem> items, List<HistoryEntry> history) {
        // add movies
        long threeDaysAgo = System.currentTimeMillis() - 3 * DateUtils.DAY_IN_MILLIS;
        for (int i = 0, size = history.size(); i < size; i++) {
            HistoryEntry entry = history.get(i);

            if (entry.movie == null || entry.movie.ids == null || entry.movie.ids.tmdb == null
                    || entry.watched_at == null) {
                // missing required values
                continue;
            }

            // only include movies watched in the last 72 hours
            // however, include at least one older one if there are none
            if (entry.watched_at.isBefore(threeDaysAgo) && items.size() > 1) {
                break;
            }

            String poster = (entry.movie.images == null || entry.movie.images.poster == null) ? null
                    : entry.movie.images.poster.thumb;
            items.add(new NowAdapter.NowItem()
                    .displayData(
                            entry.watched_at.getMillis(),
                            "",
                            entry.movie.title,
                            poster
                    )
                    .tmdbId(entry.movie.ids.tmdb)
                    .recentlyWatchedTrakt(entry.action)
            );
        }
    }

    @NonNull
    @Override
    protected String getAction() {
        return "get user movie history";
    }

    @Override
    protected Call<List<HistoryEntry>> buildCall(TraktV2 trakt) {
        return buildUserMovieHistoryCall(trakt);
    }

    public static Call<List<HistoryEntry>> buildUserMovieHistoryCall(TraktV2 trakt) {
        return trakt.users()
                .history(Username.ME, HistoryType.MOVIES, 1, MAX_HISTORY_SIZE, Extended.IMAGES);
    }
}
