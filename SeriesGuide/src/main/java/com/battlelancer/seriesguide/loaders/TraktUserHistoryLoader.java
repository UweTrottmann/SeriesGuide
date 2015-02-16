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
import android.text.format.DateUtils;
import com.battlelancer.seriesguide.adapters.NowAdapter;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt.v2.TraktV2;
import com.uwetrottmann.trakt.v2.entities.HistoryEntry;
import com.uwetrottmann.trakt.v2.enums.Extended;
import com.uwetrottmann.trakt.v2.exceptions.OAuthUnauthorizedException;
import java.util.LinkedList;
import java.util.List;
import retrofit.RetrofitError;
import timber.log.Timber;

/**
 * Loads last 24 hours of trakt watched episodes, or at least one older episode.
 */
public class TraktUserHistoryLoader extends GenericSimpleLoader<List<NowAdapter.NowItem>> {

    public static final int MAX_HISTORY_SIZE = 25;

    public TraktUserHistoryLoader(Context context) {
        super(context);
    }

    @Override
    public List<NowAdapter.NowItem> loadInBackground() {
        TraktV2 trakt = ServiceUtils.getTraktV2WithAuth(getContext());
        if (trakt == null) {
            return null;
        }

        List<HistoryEntry> history;
        try {
            history = trakt.users().historyEpisodes("me", 1, MAX_HISTORY_SIZE, Extended.IMAGES);
        } catch (RetrofitError e) {
            Timber.e(e, "Loading user episode history failed");
            return null;
        } catch (OAuthUnauthorizedException e) {
            TraktCredentials.get(getContext()).setCredentialsInvalid();
            return null;
        }

        if (history == null || history.isEmpty()) {
            return null;
        }

        long timeDayAgo = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
        List<NowAdapter.NowItem> items = new LinkedList<>();
        for (HistoryEntry entry : history) {
            if (entry.episode == null || entry.episode.ids == null || entry.episode.ids.tvdb == null
                    || entry.show == null || entry.watched_at == null) {
                // missing required values
                continue;
            }

            // only include episodes watched in the last 24 hours
            // however, include at least one older episode if there are none, yet
            if (entry.watched_at.isBefore(timeDayAgo) && items.size() > 0) {
                break;
            }

            String poster = (entry.show.images == null || entry.show.images.poster == null)
                    ? null : entry.show.images.poster.thumb;
            String description = (entry.episode.season == null || entry.episode.number == null)
                    ? entry.episode.title
                    : Utils.getNextEpisodeString(getContext(), entry.episode.season,
                            entry.episode.number, entry.episode.title);
            NowAdapter.NowItem item = new NowAdapter.NowItem().recentlyWatchedTrakt(
                    entry.episode.ids.tvdb,
                    entry.show.ids == null ? null : entry.show.ids.tvdb,
                    entry.watched_at.getMillis(),
                    entry.show.title,
                    description,
                    poster
            );
            items.add(item);
        }

        // add link to more history
        if (items.size() > 0) {
            items.add(new NowAdapter.NowItem().recentlyWatchedMoreLink());
        }

        return items;
    }
}
